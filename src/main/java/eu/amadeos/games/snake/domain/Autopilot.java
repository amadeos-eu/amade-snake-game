package eu.amadeos.games.snake.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * The computer player of the title screen. Before each tick it looks at the three cells the head can move
 * to, keeps those that do not crash and that leave the snake enough room behind them, and of those takes
 * the one nearest the apple (the golden one first). Not clever, but it rarely traps itself: the room is
 * counted with the flood fill from Mines.
 */
public final class Autopilot {
    /** The direction for the next tick; the current heading if no turn is better. */
    // --8<-- [start:choose]
    public Direction choose(Game game) {
        Cell target = game.golden() != null ? game.golden() : game.apple();
        Direction best = game.heading();
        int bestDistance = Integer.MAX_VALUE;
        int bestRoom = -1;
        for (Direction direction : Direction.values()) {
            if (direction == game.heading().opposite()) continue;
            Cell next = game.head().step(direction);
            if (!game.isFree(next)) continue;
            int room = Math.min(room(game, next), game.length());
            int distance = next.distance(target);
            // More room first (up to the snake's length, which is enough), then nearer the apple.
            if (room > bestRoom || (room == bestRoom && distance < bestDistance)) {
                best = direction;
                bestRoom = room;
                bestDistance = distance;
            }
        }
        return best;
    }
    // --8<-- [end:choose]

    /** How many cells the head could still reach from {@code start}, counting at most the snake's length. */
    private static int room(Game game, Cell start) {
        Set<Cell> seen = new HashSet<>();
        Deque<Cell> work = new ArrayDeque<>();
        seen.add(start);
        work.push(start);
        while (!work.isEmpty() && seen.size() < game.length()) {
            Cell cell = work.pop();
            for (Direction d : Direction.values()) {
                Cell next = cell.step(d);
                if (!seen.contains(next) && game.isFree(next)) {
                    seen.add(next);
                    work.push(next);
                }
            }
        }
        return seen.size();
    }
}
