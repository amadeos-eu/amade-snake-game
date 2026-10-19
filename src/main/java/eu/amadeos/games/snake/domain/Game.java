package eu.amadeos.games.snake.domain;

import eu.amadeos.gameengine.random.SeededRandom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A game of Snake. Plain Java without a clock: {@link #tick()} moves the snake by one cell, {@link #turn}
 * tells it where to go next, and the {@link #events()} tell what happened in the last tick.
 *
 * <p>The game is <i>deterministic</i>: everything left to chance (where the apples appear) comes from a
 * {@link SeededRandom} made from the seed, and time exists only as ticks. The same seed and the same turns
 * at the same ticks always play the same game, which is what makes replays possible.
 */
public final class Game {
    /** The snake moves; the snake has crashed (or filled the field). */
    public enum Phase { PLAYING, OVER }

    /** At most this many turns wait for the next ticks; more key presses within one tick are ignored. */
    static final int MAX_QUEUED_TURNS = 2;

    private final long seed;
    private final SeededRandom random;
    /** The cells of the snake, the head first. */
    private final Deque<Cell> body = new ArrayDeque<>();
    private final Deque<Direction> turns = new ArrayDeque<>();
    private final List<Event> events = new ArrayList<>();
    private List<Cell> previous;
    private Direction heading = Direction.RIGHT;
    private Cell apple;
    private Cell golden;
    private int goldenTicks;
    private int eaten;
    private int score;
    private long ticks;
    private Phase phase = Phase.PLAYING;

    // --8<-- [start:new-game]
    public Game(long seed) {
        this.seed = seed;
        this.random = new SeededRandom(seed);
        // In the middle of the field, heading right.
        int y = Field.ROWS / 2;
        for (int i = 0; i < Field.START_LENGTH; i++) body.addLast(new Cell(Field.COLUMNS / 3 - i, y));
        previous = List.copyOf(body);
        apple = freeCell();
    }
    // --8<-- [end:new-game]

    public long seed() {
        return seed;
    }

    /** The snake, the head first. */
    public List<Cell> body() {
        return List.copyOf(body);
    }

    /** The snake as it was before the last tick, for drawing the movement in between. */
    public List<Cell> previousBody() {
        return previous;
    }

    public Cell head() {
        return body.getFirst();
    }

    public Direction heading() {
        return heading;
    }

    public Cell apple() {
        return apple;
    }

    /** The golden apple, or {@code null} while there is none. */
    public Cell golden() {
        return golden;
    }

    /** Ticks left until the golden apple disappears; 0 while there is none. */
    public int goldenTicksLeft() {
        return golden == null ? 0 : goldenTicks;
    }

    public int score() {
        return score;
    }

    public int length() {
        return body.size();
    }

    /** The ticks so far. */
    public long ticks() {
        return ticks;
    }

    public Phase phase() {
        return phase;
    }

    /** What happened in the last tick. */
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }

    // --8<-- [start:interval]
    /** Seconds per tick: the more the snake has eaten, the faster it moves. */
    public double interval() {
        return Math.max(Field.MIN_INTERVAL, Field.START_INTERVAL - eaten * Field.SPEED_UP);
    }
    // --8<-- [end:interval]

    // --8<-- [start:turn]
    /**
     * The player wants to go {@code direction} next. Turns wait in a queue for the next ticks, so two quick
     * presses within one tick (up, then left) make two turns instead of losing one. A turn back into the
     * snake's own neck, or in the direction it already goes, is ignored.
     *
     * @return whether the turn was accepted
     */
    public boolean turn(Direction direction) {
        if (phase == Phase.OVER || turns.size() >= MAX_QUEUED_TURNS) return false;
        Direction last = turns.isEmpty() ? heading : turns.getLast();
        if (direction == last || direction == last.opposite()) return false;
        turns.addLast(direction);
        return true;
    }
    // --8<-- [end:turn]

    /**
     * Whether the head may move into {@code cell} in the next tick without crashing: not the wall, not the
     * body. The tail is free, because it moves on in the same tick, unless the snake grows.
     */
    public boolean isFree(Cell cell) {
        if (Field.isWall(cell)) return false;
        if (cell.equals(body.getLast())) return !cell.equals(apple);
        return !body.contains(cell);
    }

    // --8<-- [start:tick]
    /** One tick: the snake moves by one cell, eats, grows or crashes. */
    public void tick() {
        events.clear();
        if (phase == Phase.OVER) return;
        ticks++;
        if (!turns.isEmpty()) heading = turns.removeFirst();
        Cell next = head().step(heading);
        if (!isFree(next)) {
            previous = List.copyOf(body);    // the snake stops where it is
            end(new Event.Crashed(next), false);
            return;
        }
        previous = List.copyOf(body);
        boolean grows = next.equals(apple);
        body.addFirst(next);
        if (!grows) body.removeLast();
        if (next.equals(golden)) eatGolden(next);
        else if (golden != null && --goldenTicks == 0) {
            events.add(new Event.GoldenGone(golden));
            golden = null;
        }
        if (grows) eatApple(next);
    }
    // --8<-- [end:tick]

    // --8<-- [start:eat]
    private void eatApple(Cell cell) {
        eaten++;
        score += Field.APPLE_POINTS;
        events.add(new Event.Ate(cell, false));
        apple = freeCell();
        if (apple == null) {
            end(null, true);    // no room left: the snake fills the whole field
            return;
        }
        if (eaten % Field.GOLDEN_EVERY == 0 && golden == null) {
            golden = freeCell();
            goldenTicks = Field.GOLDEN_TICKS;
            if (golden != null) events.add(new Event.GoldenAppeared(golden));
        }
    }
    // --8<-- [end:eat]

    private void eatGolden(Cell cell) {
        score += Field.GOLDEN_POINTS;
        events.add(new Event.Ate(cell, true));
        golden = null;
    }

    private void end(Event.Crashed crash, boolean full) {
        phase = Phase.OVER;
        turns.clear();
        if (crash != null) events.add(crash);
        events.add(new Event.GameOver(new GameResult(score, body.size(), full)));
    }

    // --8<-- [start:free-cell]
    /**
     * A random empty cell, or {@code null} if there is none. The empty cells are counted in a fixed order,
     * row by row, and the random number picks one of them: the same state and the same generator always
     * give the same cell.
     */
    private Cell freeCell() {
        Set<Cell> taken = new HashSet<>(body);
        if (apple != null) taken.add(apple);
        if (golden != null) taken.add(golden);
        List<Cell> free = new ArrayList<>(Field.INSIDE);
        for (int y = 1; y < Field.ROWS - 1; y++) {
            for (int x = 1; x < Field.COLUMNS - 1; x++) {
                Cell cell = new Cell(x, y);
                if (!taken.contains(cell)) free.add(cell);
            }
        }
        return free.isEmpty() ? null : free.get(random.nextInt(free.size()));
    }
    // --8<-- [end:free-cell]

    /** For tests: puts the apple where the test wants it. */
    void putApple(Cell cell) {
        apple = cell;
    }

    /** For tests: puts a golden apple that stays for {@code ticks}. */
    void putGolden(Cell cell, int ticks) {
        golden = cell;
        goldenTicks = ticks;
    }
}
