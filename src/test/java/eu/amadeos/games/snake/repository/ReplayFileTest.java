package eu.amadeos.games.snake.repository;

import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.games.snake.domain.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayFileTest {
    @TempDir
    Path dir;

    @Test
    void theBestGameIsKeptAsText() throws Exception {
        ReplayFile file = new ReplayFile(dir.resolve("games/snake/best.replay"));
        assertEquals(Optional.empty(), file.load());
        Replay<Direction> replay = new Replay<>(Direction.class, 42);
        replay.record(3, Direction.UP);
        replay.record(9, Direction.LEFT);
        file.save(new ReplayFile.Best(replay, 340));
        assertEquals("# score 340\nseed 42\n3 UP\n9 LEFT\n", Files.readString(dir.resolve("games/snake/best.replay")));
        assertEquals(new ReplayFile.Best(replay, 340), file.load().orElseThrow());
    }

    @Test
    void aBrokenFileIsNoReplay() throws Exception {
        Path path = dir.resolve("best.replay");
        Files.writeString(path, "seed 1\n2 SIDEWAYS\n");
        assertTrue(new ReplayFile(path).load().isEmpty());
    }
}
