package eu.amadeos.games.snake.repository;

import eu.amadeos.gameengine.replay.Replay;
import eu.amadeos.games.snake.domain.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * The best game so far, as a replay in {@code ~/.local/share/amadeos/games/snake/best.replay}: a comment
 * with its points, then the seed and the turns. The title screen plays it back.
 */
// --8<-- [start:replay-file]
public final class ReplayFile {
    private static final Logger LOG = LoggerFactory.getLogger(ReplayFile.class);
    private static final String SCORE = "# score ";

    /** A replay and the points it reached. */
    public record Best(Replay<Direction> replay, int score) {
    }

    private final Path file;

    public ReplayFile(Path file) {
        this.file = file;
    }

    /** The best game, or empty if there is none yet or the file cannot be read. */
    public Optional<Best> load() {
        if (!Files.exists(file)) return Optional.empty();
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String first = text.lines().findFirst().orElse("");
            int score = first.startsWith(SCORE) ? Integer.parseInt(first.substring(SCORE.length()).strip()) : 0;
            return Optional.of(new Best(Replay.parse(Direction.class, text), score));
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Cannot read the replay {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    /** Keeps {@code best}, replacing the file in one step, so a crash never leaves half a replay. */
    public void save(Best best) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, SCORE + best.score() + "\n" + best.replay().toText(), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.warn("Cannot save the replay {}: {}", file, e.getMessage());
        }
    }
}
// --8<-- [end:replay-file]
