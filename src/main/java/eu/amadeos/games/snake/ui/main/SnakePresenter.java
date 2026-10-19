package eu.amadeos.games.snake.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.GameResult;
import eu.amadeos.games.snake.repository.ReplayFile;
import eu.amadeos.games.snake.repository.SnakeSettings;
import eu.amadeos.games.snake.ui.game.Arena;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Everything around the field, without Swing: the menu commands, the settings, pausing when the window
 * loses the focus, the best results and the replay of the best game. All methods run on the Swing thread.
 */
public final class SnakePresenter {
    /** The one table of best results. */
    static final String TABLE = "classic";

    private final SnakeView view;
    private final Arena arena;
    private final Messages msg;
    private final SnakeSettings settings;
    private final HighScores scores;
    private final ReplayFile replays;
    private final Supplier<Instant> now;
    private int bestScore;

    @Inject
    public SnakePresenter(SnakeView view, Arena arena, Messages msg, SnakeSettings settings, HighScores scores,
                          ReplayFile replays) {
        this(view, arena, msg, settings, scores, replays, Instant::now);
    }

    SnakePresenter(SnakeView view, Arena arena, Messages msg, SnakeSettings settings, HighScores scores,
                   ReplayFile replays, Supplier<Instant> now) {
        this.view = view;
        this.arena = arena;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.replays = replays;
        this.now = now;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode);
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.onGameOver(this::gameOver);
        replays.load().ifPresent(best -> {
            bestScore = best.score();
            arena.setBest(best);
        });
        arena.showTitle();
    }

    public void newGame() {
        arena.newGame();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running game waits. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    public void showScores() {
        view.showScores(-1);
    }

    public void showAbout() {
        view.showAbout();
    }

    // --8<-- [start:game-over]
    /** A game ended: the best one so far becomes the title screen's replay, a good score enters the table. */
    void gameOver(GameResult result, Replay<Direction> replay) {
        if (result.score() > bestScore) {
            bestScore = result.score();
            ReplayFile.Best best = new ReplayFile.Best(replay, result.score());
            replays.save(best);
            arena.setBest(best);
        }
        if (result.score() <= 0 || !scores.qualifies(TABLE, result.score())) return;
        String message = msg.format(result.full() ? "scores.full" : "scores.reached", result.score(), result.length());
        view.askName(message).ifPresent(name -> {
            int place = scores.add(TABLE, new Score(name, result.score(), now.get()));
            view.showScores(place);
        });
    }
    // --8<-- [end:game-over]
}
