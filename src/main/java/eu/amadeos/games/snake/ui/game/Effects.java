package eu.amadeos.games.snake.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.games.snake.domain.Cell;
import eu.amadeos.games.snake.domain.Event;
import eu.amadeos.games.snake.domain.Field;

import java.awt.Color;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * What the events of a tick look and sound like: a burst of juice for an apple, splinters for a crash,
 * blips from the mixer. The rules know nothing of this; they only report what happened.
 *
 * <p>The particles take their chance from a random generator of their own, never from the rules' one:
 * splinters must not change where the next apple appears.
 */
final class Effects {
    private static final Color[] PALETTE = {new Color(0xE8402A), new Color(0xF0C040), new Color(0x7DB0F0),
            Color.WHITE};
    private static final int RED = 0;
    private static final int GOLD = 1;
    private static final int BLUE = 2;
    private static final int WHITE = 3;
    private static final double GRAVITY = 300;

    private final Particles particles = new Particles(2000, PALETTE);
    private final SoundBank sounds = new SoundBank();
    private final Mixer mixer;
    private final RandomGenerator random;

    Effects(Mixer mixer, RandomGenerator random) {
        this.mixer = mixer;
        this.random = random;
    }

    Particles particles() {
        return particles;
    }

    void update(double seconds) {
        particles.update(seconds, GRAVITY);
    }

    void clear() {
        particles.clear();
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last tick. */
    void react(List<Event> events, boolean audible) {
        for (Event event : events) {
            switch (event) {
                case Event.Ate(Cell cell, boolean golden) -> {
                    burst(cell, golden ? GOLD : RED, golden ? 50 : 20, 120);
                    if (audible) mixer.play(golden ? sounds.golden : sounds.apple);
                }
                case Event.GoldenAppeared appeared -> {
                    burst(appeared.cell(), WHITE, 12, 60);
                    if (audible) mixer.play(sounds.goldenAppears);
                }
                case Event.GoldenGone gone -> {
                    if (audible) mixer.play(sounds.goldenGone);
                }
                case Event.Crashed(Cell cell) -> {
                    burst(cell, BLUE, 60, 180);
                    if (audible) mixer.play(sounds.crash);
                }
                case Event.GameOver over -> {
                    if (audible) mixer.play(sounds.gameOver);
                }
            }
        }
    }
    // --8<-- [end:react]

    /** {@code count} particles fly out of the middle of a cell in all directions. */
    private void burst(Cell cell, int colour, int count, double speed) {
        double x = Field.x(cell.x()) + Field.CELL / 2;
        double y = Field.y(cell.y()) + Field.CELL / 2;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double v = speed * (0.3 + random.nextDouble() * 0.7);
            particles.spawn(x, y, Math.cos(angle) * v, Math.sin(angle) * v - 60, 0.4 + random.nextDouble() * 0.5,
                    1.5 + random.nextDouble() * 2, colour);
        }
    }
}
