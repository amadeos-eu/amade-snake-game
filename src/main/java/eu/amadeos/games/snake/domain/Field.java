package eu.amadeos.games.snake.domain;

/**
 * The field in cells and in logical units (640 × 400): a band of 40 units at the top for the score, below
 * it 32 × 18 cells of 20 units. The outermost ring of cells is the wall, so the snake has 30 × 16 cells.
 * Times are in seconds per tick.
 */
public final class Field {
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;
    /** The band at the top where score and length are shown. */
    public static final double HUD = 40;
    public static final double CELL = 20;

    public static final int COLUMNS = 32;
    public static final int ROWS = 18;
    /** The cells inside the wall. */
    public static final int INSIDE = (COLUMNS - 2) * (ROWS - 2);

    public static final int START_LENGTH = 4;
    // --8<-- [start:pace]
    /** The pace at the start: a cell every 150 ms, nearly seven a second. */
    public static final double START_INTERVAL = 0.15;
    /** Every apple takes this much off the interval, down to {@link #MIN_INTERVAL}. */
    public static final double SPEED_UP = 0.002;
    /** The fastest pace, reached with the 35th apple: a cell every 80 ms, 12.5 a second. */
    public static final double MIN_INTERVAL = 0.08;
    // --8<-- [end:pace]

    public static final int APPLE_POINTS = 10;
    public static final int GOLDEN_POINTS = 50;
    /** After every fifth apple a golden one appears … */
    public static final int GOLDEN_EVERY = 5;
    /** … and stays for this many ticks. */
    public static final int GOLDEN_TICKS = 40;

    private Field() {
    }

    public static boolean isWall(Cell cell) {
        return cell.x() <= 0 || cell.y() <= 0 || cell.x() >= COLUMNS - 1 || cell.y() >= ROWS - 1;
    }

    /** The left edge of a column in logical units. */
    public static double x(double column) {
        return column * CELL;
    }

    /** The top edge of a row in logical units. */
    public static double y(double row) {
        return HUD + row * CELL;
    }
}
