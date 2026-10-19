package eu.amadeos.games.snake.domain;

/** Something that happened during a tick; the user interface decides what it looks and sounds like. */
// --8<-- [start:event]
public sealed interface Event {
    /** The snake ate an apple at {@code cell}; {@code golden} for a golden one. */
    record Ate(Cell cell, boolean golden) implements Event {
    }

    record GoldenAppeared(Cell cell) implements Event {
    }

    /** A golden apple was not eaten in time. */
    record GoldenGone(Cell cell) implements Event {
    }

    /** The head ran into the wall or the snake's own body at {@code cell}. */
    record Crashed(Cell cell) implements Event {
    }

    record GameOver(GameResult result) implements Event {
    }
}
// --8<-- [end:event]
