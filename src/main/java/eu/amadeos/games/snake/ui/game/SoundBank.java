package eu.amadeos.games.snake.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;

/** The sound effects of Snake, made once at start: arcade tones, no recordings. */
// --8<-- [start:sounds]
final class SoundBank {
    final Sound apple = Sound.tone(Wave.SQUARE, 600, 900, 0.06, 0.22);
    /** E, G, high E: a little fanfare. */
    final Sound golden = note(659, 0.06).then(note(784, 0.06)).then(note(1319, 0.15));
    final Sound goldenAppears = Sound.tone(Wave.TRIANGLE, 1200, 1600, 0.08, 0.2);
    final Sound goldenGone = Sound.tone(Wave.TRIANGLE, 900, 500, 0.15, 0.15);
    final Sound crash = Sound.tone(Wave.NOISE, 0, 0, 0.2, 0.2);
    /** G, E, C, falling. */
    final Sound gameOver = note(392, 0.2).then(note(330, 0.2)).then(Sound.tone(Wave.TRIANGLE, 262, 250, 0.5, 0.3));

    private static Sound note(double hz, double seconds) {
        return Sound.tone(Wave.TRIANGLE, hz, hz, seconds, 0.3);
    }
}
// --8<-- [end:sounds]
