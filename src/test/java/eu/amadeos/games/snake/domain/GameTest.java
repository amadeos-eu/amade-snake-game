package eu.amadeos.games.snake.domain;

import eu.amadeos.gameengine.replay.Replay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    /** The snake starts with its head at (10, 9), heading right, four cells long. */
    private final Game game = new Game(1);

    private void ticks(int n) {
        for (int i = 0; i < n; i++) game.tick();
    }

    @Test
    void theSnakeMovesOneCellPerTick() {
        assertEquals(List.of(new Cell(10, 9), new Cell(9, 9), new Cell(8, 9), new Cell(7, 9)), game.body());
        game.putApple(new Cell(1, 1));
        game.tick();
        assertEquals(new Cell(11, 9), game.head());
        assertEquals(4, game.length());
        assertEquals(new Cell(10, 9), game.previousBody().getFirst());
        assertEquals(1, game.ticks());
    }

    // --8<-- [start:test-eat]
    @Test
    void anAppleMakesTheSnakeLongerAndFaster() {
        game.putApple(new Cell(11, 9));
        game.tick();
        assertEquals(5, game.length());
        assertEquals(10, game.score());
        assertEquals(List.of(new Event.Ate(new Cell(11, 9), false)), game.events());
        assertEquals(0.148, game.interval(), 1e-9);
        assertFalse(game.body().contains(game.apple()), "the next apple lies elsewhere");
    }
    // --8<-- [end:test-eat]

    // --8<-- [start:test-turns]
    @Test
    void twoQuickTurnsWithinOneTickAreBothCarriedOut() {
        game.putApple(new Cell(1, 1));
        assertTrue(game.turn(Direction.UP));
        assertTrue(game.turn(Direction.LEFT));
        assertFalse(game.turn(Direction.DOWN), "two turns wait at most");
        game.tick();
        assertEquals(new Cell(10, 8), game.head());
        game.tick();
        assertEquals(new Cell(9, 8), game.head());
    }

    @Test
    void theSnakeCannotTurnBackIntoItsNeck() {
        assertFalse(game.turn(Direction.LEFT), "it heads right");
        assertFalse(game.turn(Direction.RIGHT), "already its direction");
        assertTrue(game.turn(Direction.UP));
        assertFalse(game.turn(Direction.DOWN), "back after the queued turn up");
    }
    // --8<-- [end:test-turns]

    @Test
    void theWallEndsTheGame() {
        game.putApple(new Cell(1, 1));
        ticks(20);
        assertEquals(new Cell(30, 9), game.head());
        assertEquals(Game.Phase.PLAYING, game.phase());
        game.tick();
        assertEquals(Game.Phase.OVER, game.phase());
        assertEquals(List.of(new Event.Crashed(new Cell(31, 9)), new Event.GameOver(new GameResult(0, 4, false))),
                game.events());
        assertEquals(new Cell(30, 9), game.head(), "the snake stops at the wall");
        game.tick();
        assertEquals(21, game.ticks(), "nothing moves any more");
    }

    @Test
    void theSnakeMustNotBiteItself() {
        game.putApple(new Cell(11, 9));
        game.tick();
        game.putApple(new Cell(12, 9));
        game.tick();
        game.putApple(new Cell(1, 1));
        game.turn(Direction.UP);
        game.tick();
        game.turn(Direction.LEFT);
        game.tick();
        game.turn(Direction.DOWN);
        game.tick();
        assertEquals(Game.Phase.OVER, game.phase());
        assertTrue(game.events().contains(new Event.Crashed(new Cell(11, 9))));
    }

    @Test
    void theTailIsFreeBecauseItMovesOn() {
        game.putApple(new Cell(1, 1));
        game.turn(Direction.UP);
        game.tick();
        game.turn(Direction.LEFT);
        game.tick();
        game.turn(Direction.DOWN);
        game.tick();
        assertEquals(Game.Phase.PLAYING, game.phase());
        assertEquals(new Cell(9, 9), game.head(), "where the tail was a tick ago");
    }

    @Test
    void afterFiveApplesAGoldenOneAppears() {
        for (int x = 11; x <= 15; x++) {
            assertNull(game.golden());
            game.putApple(new Cell(x, 9));
            game.tick();
        }
        assertNotNull(game.golden());
        assertTrue(game.events().contains(new Event.GoldenAppeared(game.golden())));
        assertEquals(Field.GOLDEN_TICKS, game.goldenTicksLeft());
    }

    @Test
    void aGoldenAppleGivesPointsButOnlyForAWhile() {
        game.putApple(new Cell(1, 1));
        game.putGolden(new Cell(11, 9), 40);
        game.tick();
        assertEquals(List.of(new Event.Ate(new Cell(11, 9), true)), game.events());
        assertEquals(50, game.score());
        assertEquals(4, game.length(), "it does not make the snake longer");

        game.putGolden(new Cell(1, 2), 2);
        game.tick();
        assertEquals(List.of(), game.events());
        game.tick();
        assertEquals(List.of(new Event.GoldenGone(new Cell(1, 2))), game.events());
        assertNull(game.golden());
    }

    // --8<-- [start:test-determinism]
    /** Plays a whole game with the computer at the keys and records every accepted turn. */
    private static Replay<Direction> play(long seed, List<Integer> scores) {
        Game game = new Game(seed);
        Replay<Direction> replay = new Replay<>(Direction.class, seed);
        Autopilot pilot = new Autopilot();
        while (game.phase() == Game.Phase.PLAYING && game.ticks() < 20_000) {
            Direction turn = pilot.choose(game);
            if (game.turn(turn)) replay.record(game.ticks(), turn);
            game.tick();
        }
        scores.add(game.score());
        return replay;
    }

    @Test
    void theSameSeedAndTheSameTurnsPlayTheSameGame() {
        List<Integer> scores = new ArrayList<>();
        Replay<Direction> replay = play(2026, scores);

        // Play it again from the replay alone: same apples, same crash, same points.
        Game again = new Game(replay.seed());
        Replay<Direction>.Playback playback = replay.playback();
        while (again.phase() == Game.Phase.PLAYING && again.ticks() < 20_000) {
            for (Direction turn : playback.actionsAt(again.ticks())) again.turn(turn);
            again.tick();
        }
        assertEquals(scores.getFirst(), again.score());
        assertTrue(again.score() > 100, "a real game, not a crash at the start: " + again.score());
    }
    // --8<-- [end:test-determinism]

    @Test
    void anotherSeedPutsTheApplesElsewhere() {
        assertNotEquals(new Game(1).apple(), new Game(2).apple());
        assertEquals(new Game(3).apple(), new Game(3).apple());
    }
}
