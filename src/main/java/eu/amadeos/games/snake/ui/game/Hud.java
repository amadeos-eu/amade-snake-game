package eu.amadeos.games.snake.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.games.snake.domain.Field;
import eu.amadeos.games.snake.domain.Game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The overlay: score, length and pace in the band at the top, and the texts of the title, pause and end
 * screens. The canvas draws it after the world, at the full size of the screen, so the letters are sharp
 * in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xF0C040);
    /** A veil over the field behind texts. */
    static final Color VEIL = new Color(0, 0, 0, 160);
    /** The baseline of the status band. */
    private static final float BASELINE = 25f;

    private final Messages msg;
    // Fonts are made once, not in every frame.
    private final Font small = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(13f);
    private final Map<Integer, Font> fonts = new HashMap<>();
    private final Rectangle2D.Double veil = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);

    Hud(Messages msg) {
        this.msg = msg;
    }

    // --8<-- [start:hud]
    /** Score on the left, the snake's length in the middle, its pace in cells per second on the right. */
    void status(Graphics2D g, Game game) {
        g.setFont(small);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(CHALK);
        g.drawString(msg.format("hud.score", game.score()), (float) Field.CELL, BASELINE);
        String length = msg.format("hud.length", game.length());
        g.drawString(length, (float) (Field.WIDTH - fm.stringWidth(length)) / 2, BASELINE);
        String pace = msg.format("hud.pace", 1 / game.interval());
        g.drawString(pace, (float) (Field.WIDTH - Field.CELL - fm.stringWidth(pace)), BASELINE);
    }
    // --8<-- [end:hud]

    /** Darkens the field so text over it stays readable. */
    void veil(Graphics2D g) {
        g.setColor(VEIL);
        g.fill(veil);
    }

    /** A line of text centred horizontally, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, int size, boolean bold, Color colour) {
        g.setFont(fonts.computeIfAbsent(2 * size + (bold ? 1 : 0),
                key -> new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size)));
        g.setColor(colour);
        g.drawString(line, (float) (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2, (float) y);
    }
}
