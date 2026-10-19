package eu.amadeos.games.snake.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.snake.bootstrap.SnakeModule;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.GameResult;
import eu.amadeos.games.snake.repository.ReplayFile;
import eu.amadeos.games.snake.repository.SnakeSettings;
import eu.amadeos.games.snake.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnakePresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private BiConsumer<GameResult, Replay<Direction>> gameOver;
    private SnakePresenter presenter;
    private HighScores scores;
    private SnakeSettings settings;
    private ReplayFile replays;

    private final SnakeView view = new SnakeView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<String> askName(String message) {
            log.add("ask " + message);
            return Optional.of("Ada");
        }

        @Override
        public void showScores(int highlight) {
            log.add("scores " + highlight);
        }

        @Override
        public void showAbout() {
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void newGame() {
            log.add("new");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public void setBest(ReplayFile.Best best) {
            log.add("best " + best.score());
        }

        @Override
        public void onGameOver(BiConsumer<GameResult, Replay<Direction>> listener) {
            gameOver = listener;
        }
    };

    @BeforeEach
    void create() {
        Messages msg = Messages.load(SnakeModule.class, SnakeModule.MESSAGES, Locale.ENGLISH);
        settings = new SnakeSettings(dir.resolve("snake.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        replays = new ReplayFile(dir.resolve("best.replay"));
        presenter = new SnakePresenter(view, arena, msg, settings, scores, replays, () -> Instant.EPOCH);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.PIXEL);
        presenter.setSound(false);
        assertEquals(List.of("mode PIXEL", "sound false"), log);
        assertEquals(RenderMode.PIXEL, settings.renderMode());
    }

    @Test
    void aGoodScoreEntersTheTableAndTheBestGameIsKept() {
        log.clear();
        Replay<Direction> replay = new Replay<>(Direction.class, 42);
        replay.record(3, Direction.UP);
        gameOver.accept(new GameResult(230, 27, false), replay);
        assertEquals(List.of("best 230", "ask 230 points, length 27.", "scores 0"), log);
        assertEquals("Ada", scores.list(SnakePresenter.TABLE).getFirst().name());
        assertEquals(new ReplayFile.Best(replay, 230), replays.load().orElseThrow());
    }

    @Test
    void theBestGameIsReplayedAfterARestartAndOnlyABetterOneReplacesIt() {
        Replay<Direction> replay = new Replay<>(Direction.class, 7);
        replays.save(new ReplayFile.Best(replay, 500));
        log.clear();
        presenter.start();
        assertEquals("best 500", log.get(3));
        log.clear();
        gameOver.accept(new GameResult(100, 14, false), new Replay<>(Direction.class, 8));
        assertEquals(500, replays.load().orElseThrow().score());
    }

    @Test
    void noPointsNoEntry() {
        log.clear();
        gameOver.accept(new GameResult(0, 4, false), new Replay<>(Direction.class, 1));
        assertEquals(List.of(), log);
    }
}
