package eu.amadeos.games.snake.ui.game;

import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.GameResult;
import eu.amadeos.games.snake.repository.ReplayFile;

import java.util.function.BiConsumer;

/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title screen, where the best game is replayed or the computer plays. */
    void showTitle();

    void newGame();

    /** Pauses a running game; does nothing elsewhere. */
    void pause();

    void setSound(boolean on);

    /** The best game so far, for the title screen; {@code null} for none. */
    void setBest(ReplayFile.Best best);

    /** Called on the Swing thread when a game ends, with the game as a replay. */
    void onGameOver(BiConsumer<GameResult, Replay<Direction>> listener);
}
