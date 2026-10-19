package eu.amadeos.games.snake.domain;

/** A square of the field, counted from the top left corner; the outermost ring is the wall. */
public record Cell(int x, int y) {
    /** The neighbour in a direction. */
    public Cell step(Direction direction) {
        return new Cell(x + direction.dx(), y + direction.dy());
    }

    /** Steps from here to {@code other}, along the grid. */
    public int distance(Cell other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
