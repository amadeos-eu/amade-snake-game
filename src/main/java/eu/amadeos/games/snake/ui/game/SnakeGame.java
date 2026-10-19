package eu.amadeos.games.snake.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.loop.Ticker;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.snake.domain.Autopilot;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.Event;
import eu.amadeos.games.snake.domain.Game;
import eu.amadeos.games.snake.domain.GameResult;
import eu.amadeos.games.snake.repository.ReplayFile;
import eu.amadeos.games.snake.repository.SnakeSettings;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * The running game: the loop, the controls, the effects and the scenes (title with a replay or the computer
 * playing, a game, the pause screen, the end of a game). Everything here runs on the game loop thread,
 * except the {@link Arena} methods, which only post commands to it.
 */
public final class SnakeGame implements Arena {
    /** The actions of the game; which keys trigger them comes from the settings. */
    public enum Action { UP, DOWN, LEFT, RIGHT, PAUSE, START }

    /** Steps of the loop per second: the keys are read this often, whatever the snake's pace. */
    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;
    /** A new game waits this long before the snake moves, unless the player turns it first. */
    static final double READY_SECONDS = 1.5;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final Controls<Action> controls;
    private final Messages msg;
    private final LongSupplier seeds;
    private final Executor ui;
    private final Mixer mixer;
    private final FieldPainter field = new FieldPainter();
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private volatile BiConsumer<GameResult, Replay<Direction>> onGameOver = (result, replay) -> { };
    /** The best game, replayed on the title screen; only touched on the loop thread. */
    private ReplayFile.Best best;
    private GameLoop loop;

    @Inject
    public SnakeGame(Keyboard keyboard, Mouse mouse, Messages msg, SnakeSettings settings, RandomGenerator looks,
                     Mixer mixer, EdtExecutor ui) {
        this(keyboard, mouse, msg, settings, SeededRandom::freshSeed, looks, mixer, (Executor) ui);
    }

    /**
     * @param seeds the seed of each new game
     * @param looks chance for the particles, apart from the rules' own
     */
    SnakeGame(Keyboard keyboard, Mouse mouse, Messages msg, SnakeSettings settings, LongSupplier seeds,
              RandomGenerator looks, Mixer mixer, Executor ui) {
        this.keyboard = keyboard;
        this.mouse = mouse;
        this.msg = msg;
        this.seeds = seeds;
        this.mixer = mixer;
        this.ui = ui;
        this.hud = new Hud(msg);
        this.effects = new Effects(mixer, looks);
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.UP, settings.keys("up", KeyEvent.VK_UP, KeyEvent.VK_W))
                .bind(Action.DOWN, settings.keys("down", KeyEvent.VK_DOWN, KeyEvent.VK_S))
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.START, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        scenes.push(new TitleScene());
    }

    // --8<-- [start:start]
    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }
    // --8<-- [end:start]

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        mouse.poll();
        scenes.update(seconds);
    }

    /** Enter, Space or a click: on to the next screen. */
    private boolean proceed() {
        return controls.wasPressed(Action.START) || mouse.wasPressed(MouseEvent.BUTTON1);
    }

    /** The directions whose keys went down since the last step. */
    private List<Direction> pressedDirections() {
        List<Direction> pressed = new ArrayList<>(1);
        for (Direction direction : Direction.values()) {
            if (controls.wasPressed(Action.valueOf(direction.name()))) pressed.add(direction);
        }
        return pressed;
    }

    /** The world of all scenes, in logical coordinates. */
    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    /** Score and texts of all scenes, drawn after the world at the screen's full size. */
    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> scenes.replace(new PlayScene()));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public void setBest(ReplayFile.Best best) {
        run(() -> this.best = best);
    }

    @Override
    public void onGameOver(BiConsumer<GameResult, Replay<Direction>> listener) {
        onGameOver = listener;
    }

    /** On the loop thread when it runs; directly before the loop starts and in tests. */
    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    int sceneCount() {
        return scenes.size();
    }

    Effects effects() {
        return effects;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /**
     * The best game played back, then the computer playing, in turns, silently; Enter, Space or a click
     * starts a game.
     */
    final class TitleScene implements Scene {
        private final Autopilot pilot = new Autopilot();
        private Game demo;
        private Replay<Direction>.Playback playback;
        private Ticker ticker;
        private double over;

        TitleScene() {
            effects.clear();
            startDemo(true);
        }

        private void startDemo(boolean replay) {
            if (replay && best != null) {
                demo = new Game(best.replay().seed());
                playback = best.replay().playback();
            } else {
                demo = new Game(seeds.getAsLong());
                playback = null;
            }
            ticker = new Ticker(demo.interval());
            over = 0;
        }

        // --8<-- [start:title-update]
        @Override
        public void update(double seconds) {
            if (proceed()) {
                scenes.replace(new PlayScene());
                return;
            }
            effects.update(seconds);
            if (demo.phase() == Game.Phase.OVER) {
                over += seconds;
                // Two seconds to see the end, then the other kind of demo.
                if (over > 2) startDemo(playback == null);
                return;
            }
            for (int i = ticker.advance(seconds); i > 0 && demo.phase() == Game.Phase.PLAYING; i--) {
                if (playback != null) {
                    for (Direction turn : playback.actionsAt(demo.ticks())) demo.turn(turn);
                } else {
                    demo.turn(pilot.choose(demo));
                }
                demo.tick();
                effects.react(demo.events(), false);
                ticker.setInterval(demo.interval());
            }
        }
        // --8<-- [end:title-update]

        @Override
        public void render(Graphics2D g, double alpha) {
            field.paint(g, demo, effects.particles(), ticker.progress(alpha * STEP), alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("window.title"), 150, 72, true, Hud.GOLD);
            hud.text(g, msg.get("title.start"), 215, 18, false, Hud.CHALK);
            hud.text(g, msg.get("title.keys"), 290, 13, false, Hud.CHALK);
            hud.text(g, msg.get("title.screen"), 312, 13, false, Hud.CHALK);
            if (playback != null) {
                hud.text(g, msg.format("title.replay", best.score()), 360, 14, true, Hud.GOLD);
            }
        }

        Game demo() {
            return demo;
        }

        boolean replaying() {
            return playback != null;
        }
    }

    // --8<-- [start:play]
    /** A game: the keys turn the snake, the ticker moves it, every accepted turn goes into the replay. */
    final class PlayScene implements Scene {
        private final Game game = new Game(seeds.getAsLong());
        private final Replay<Direction> replay = new Replay<>(Direction.class, game.seed());
        private final Ticker ticker = new Ticker(game.interval());
        private double ready = READY_SECONDS;

        PlayScene() {
            effects.clear();
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            for (Direction direction : pressedDirections()) {
                if (game.turn(direction)) {
                    replay.record(game.ticks(), direction);
                    ready = 0;   // the first turn starts the snake at once
                }
            }
            effects.update(seconds);
            if (ready > 0) {
                ready -= seconds;
                return;
            }
            for (int i = ticker.advance(seconds); i > 0; i--) {
                game.tick();
                effects.react(game.events(), true);
                ticker.setInterval(game.interval());
                if (game.phase() == Game.Phase.OVER) {
                    finish();
                    return;
                }
            }
        }

        private void finish() {
            for (Event event : game.events()) {
                if (event instanceof Event.GameOver(GameResult result)) {
                    scenes.replace(new OverScene(game, result));
                    // The presenter lives on the Swing thread: hand the result and the replay over there.
                    BiConsumer<GameResult, Replay<Direction>> listener = onGameOver;
                    ui.execute(() -> listener.accept(result, replay));
                }
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            double between = ready > 0 ? 0 : ticker.progress(alpha * STEP);
            field.paint(g, game, effects.particles(), between, alpha);
        }
        // --8<-- [end:play]

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game);
            if (ready > 0) {
                hud.text(g, msg.get("play.ready"), 160, 28, true, Hud.GOLD);
                hud.text(g, msg.get("play.keys"), 190, 14, false, Hud.CHALK);
            }
        }

        Game game() {
            return game;
        }

        Replay<Direction> replay() {
            return replay;
        }
    }

    /** Drawn over the frozen game until P, Esc, Enter, Space or a click. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE) || proceed()) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("pause.title"), 200, 40, true, Hud.CHALK);
            hud.text(g, msg.get("pause.continue"), 245, 18, false, Hud.CHALK);
        }
    }

    /** The end: the splinters settle, the score stays, Enter, Space or a click leads back to the title. */
    final class OverScene implements Scene {
        private final Game game;
        private final GameResult result;
        private double shown;

        OverScene(Game game, GameResult result) {
            this.game = game;
            this.result = result;
        }

        @Override
        public void update(double seconds) {
            effects.update(seconds);
            shown += seconds;
            // A short wait, so a key meant for the snake does not skip the end.
            if (shown > 1 && proceed()) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            field.paint(g, game, effects.particles(), 1, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game);
            hud.veil(g);
            hud.text(g, msg.get(result.full() ? "over.full" : "over.lost"), 190, 40, true, Hud.GOLD);
            hud.text(g, msg.format("over.score", result.score(), result.length()), 232, 20, false, Hud.CHALK);
            hud.text(g, msg.get("over.continue"), 270, 16, false, Hud.CHALK);
        }
    }
}
