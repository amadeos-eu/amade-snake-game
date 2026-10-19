package eu.amadeos.games.snake.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutopilotTest {
    // --8<-- [start:test-demo]
    /** Ten minutes of the title screen at the fastest pace, in a fraction of a second. */
    @Test
    void theComputerPlaysLongGames() {
        long ticks = 0;
        int games = 0;
        int longest = 0;
        for (long seed = 1; ticks < 10 * 60 / Field.MIN_INTERVAL; seed++) {
            Game game = new Game(seed);
            Autopilot pilot = new Autopilot();
            while (game.phase() == Game.Phase.PLAYING) {
                game.turn(pilot.choose(game));
                game.tick();
            }
            ticks += game.ticks();
            games++;
            longest = Math.max(longest, game.length());
        }
        assertTrue(longest >= 60, "longest snake " + longest);
        assertTrue(games <= 10, games + " games in ten minutes");
    }
    // --8<-- [end:test-demo]
}
