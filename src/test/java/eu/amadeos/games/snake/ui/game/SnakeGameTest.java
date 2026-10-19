package eu.amadeos.games.snake.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.games.snake.bootstrap.SnakeModule;
import eu.amadeos.games.snake.domain.Cell;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.Field;
import eu.amadeos.games.snake.domain.Game;
import eu.amadeos.games.snake.domain.GameResult;
import eu.amadeos.games.snake.repository.ReplayFile;
import eu.amadeos.games.snake.repository.SnakeSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys on the keyboard and calls
 * the steps itself, so everything happens in order on the test thread.
 */
class SnakeGameTest {
    private static final double STEP = SnakeGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final Mouse mouse = new Mouse();
    private final List<GameResult> results = new ArrayList<>();
    private final List<Replay<Direction>> replays = new ArrayList<>();
    private SnakeGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(SnakeModule.class, SnakeModule.MESSAGES, Locale.ENGLISH);
        game = new SnakeGame(keyboard, mouse, msg, new SnakeSettings(dir.resolve("snake.properties")), () -> 77L,
                new SplittableRandom(4), new Mixer(), Runnable::run);
        game.onGameOver((result, replay) -> {
            results.add(result);
            replays.add(replay);
        });
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * SnakeGame.UPDATES_PER_SECOND));
    }

    @Test
    void enterStartsAGameThatWaitsUntilTheSnakeIsTurned() {
        assertInstanceOf(SnakeGame.TitleScene.class, game.top());
        tap(KeyEvent.VK_ENTER);
        SnakeGame.PlayScene play = assertInstanceOf(SnakeGame.PlayScene.class, game.top());
        Cell start = play.game().head();
        seconds(1);
        assertEquals(start, play.game().head(), "get ready: the snake waits");
        tap(KeyEvent.VK_UP);
        seconds(Field.START_INTERVAL * 3);
        assertEquals(new Cell(start.x(), start.y() - 3), play.game().head(), "three ticks up");
        assertEquals(List.of(new Replay.Entry<>(0, Direction.UP)), play.replay().entries());
    }

    // --8<-- [start:test-pace]
    @Test
    void theSnakeMovesAtItsOwnPaceWhileTheLoopSteps120TimesASecond() {
        tap(KeyEvent.VK_ENTER);
        SnakeGame.PlayScene play = assertInstanceOf(SnakeGame.PlayScene.class, game.top());
        seconds(SnakeGame.READY_SECONDS);
        long before = play.game().ticks();
        seconds(1.5);
        // 1.5 seconds at 0.15 seconds a cell, whatever the update rate.
        assertEquals(10, play.game().ticks() - before, 1);
    }
    // --8<-- [end:test-pace]

    @Test
    void pFreezesTheGameAndContinuesIt() {
        tap(KeyEvent.VK_ENTER);
        tap(KeyEvent.VK_DOWN);
        SnakeGame.PlayScene play = assertInstanceOf(SnakeGame.PlayScene.class, game.top());
        tap(KeyEvent.VK_P);
        assertEquals(2, game.sceneCount());
        long ticks = play.game().ticks();
        seconds(1);
        assertEquals(ticks, play.game().ticks());
        mouse.pressed(MouseEvent.BUTTON1);
        steps(1);
        seconds(0.5);
        assertTrue(play.game().ticks() > ticks, "moves on");
    }

    // --8<-- [start:test-replay]
    @Test
    void theEndHandsOverTheGameAndTheTitleReplaysIt() {
        tap(KeyEvent.VK_ENTER);
        SnakeGame.PlayScene play = assertInstanceOf(SnakeGame.PlayScene.class, game.top());
        // A few turns, then straight into the wall.
        tap(KeyEvent.VK_DOWN);
        seconds(0.5);
        tap(KeyEvent.VK_RIGHT);
        seconds(0.4);
        tap(KeyEvent.VK_UP);
        seconds(10);
        Game played = play.game();
        assertEquals(Game.Phase.OVER, played.phase());
        assertEquals(List.of(new GameResult(played.score(), played.length(), false)), results);
        Replay<Direction> replay = replays.getFirst();
        assertEquals(77, replay.seed());
        assertEquals(3, replay.entries().size());

        game.setBest(new ReplayFile.Best(replay, played.score()));
        game.showTitle();
        SnakeGame.TitleScene title = assertInstanceOf(SnakeGame.TitleScene.class, game.top());
        assertTrue(title.replaying());
        while (title.demo().phase() == Game.Phase.PLAYING) steps(1);
        assertEquals(played.body(), title.demo().body(), "the same game, to the last cell");
        assertEquals(played.ticks(), title.demo().ticks());
    }
    // --8<-- [end:test-replay]

    @Test
    void aClickStartsAGame() {
        mouse.pressed(MouseEvent.BUTTON1);
        steps(1);
        assertInstanceOf(SnakeGame.PlayScene.class, game.top());
    }

    @Test
    void theDemoEatsApplesAndTheJuiceSplashes() {
        boolean juice = false;
        for (int i = 0; i < 30 * 120 && !juice; i++) {
            steps(1);
            juice = game.effects().particles().count() > 0;
        }
        assertTrue(juice, "an apple was eaten");
        SnakeGame.TitleScene title = assertInstanceOf(SnakeGame.TitleScene.class, game.top());
        assertFalse(title.replaying(), "no best game yet: the computer plays");
    }

    @Test
    void everySceneDrawsWorldAndOverlay() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        seconds(2);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        tap(KeyEvent.VK_ENTER);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        tap(KeyEvent.VK_P);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        g.dispose();
        // The wall in the top left corner is grey stone, not black.
        assertTrue((image.getRGB(10, (int) Field.HUD + 10) & 0xFFFFFF) != 0);
    }
}
