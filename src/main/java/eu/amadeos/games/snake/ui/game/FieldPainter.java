package eu.amadeos.games.snake.ui.game;

import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.snake.domain.Cell;
import eu.amadeos.games.snake.domain.Direction;
import eu.amadeos.games.snake.domain.Field;
import eu.amadeos.games.snake.domain.Game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the world in logical coordinates (640 × 400): the lawn with its stone wall, the apples, the snake
 * and the particles. Every picture is a {@link Sprite} drawn in code, so it is sharp at any size and
 * scaled only once for the screen.
 */
final class FieldPainter {
    private static final Color GRASS_LIGHT = new Color(0x5E9E3A);
    private static final Color GRASS_DARK = new Color(0x548F33);
    private static final Color SKIN = new Color(0x2E6FD0);
    private static final Color SKIN_DARK = new Color(0x1B4A94);
    private static final Color SKIN_LIGHT = new Color(0x7DB0F0);

    private static final double C = Field.CELL;

    private final Sprite background = Sprite.drawn(Field.WIDTH, Field.HEIGHT, FieldPainter::drawBackground);
    private final Sprite apple = Sprite.drawn(C, C, (g, w, h) -> drawApple(g, w, new Color(0xD8302A),
            new Color(0xFF9A80)));
    private final Sprite golden = Sprite.drawn(C, C, (g, w, h) -> drawApple(g, w, new Color(0xD4A020),
            new Color(0xFFF0A0)));
    private final Sprite segment = Sprite.drawn(C, C, FieldPainter::drawSegment);
    private final Map<Direction, Sprite> heads = new EnumMap<>(Direction.class);

    FieldPainter() {
        for (Direction d : Direction.values()) heads.put(d, Sprite.drawn(C, C, (g, w, h) -> drawHead(g, w, d)));
    }

    // --8<-- [start:paint]
    /**
     * @param between how far the time is from the last tick to the next, 0 to 1: each segment is drawn that
     *                far on its way from its cell before the tick to its cell after it
     */
    void paint(Graphics2D g, Game game, Particles particles, double between, double alpha) {
        background.draw(g, 0, 0);
        apple.draw(g, Field.x(game.apple().x()), Field.y(game.apple().y()));
        // The golden apple blinks in its last ticks.
        Cell gold = game.golden();
        if (gold != null && (game.goldenTicksLeft() > 10 || game.goldenTicksLeft() % 2 == 0)) {
            golden.draw(g, Field.x(gold.x()), Field.y(gold.y()));
        }
        List<Cell> now = game.body();
        List<Cell> before = game.previousBody();
        double t = game.phase() == Game.Phase.OVER ? 1 : between;
        // From the tail to the head, so the head lies on top.
        for (int i = now.size() - 1; i >= 0; i--) {
            Cell to = now.get(i);
            Cell from = i < before.size() ? before.get(i) : to;   // a new tail segment appears in place
            double x = from.x() + (to.x() - from.x()) * t;
            double y = from.y() + (to.y() - from.y()) * t;
            (i == 0 ? heads.get(game.heading()) : segment).draw(g, Field.x(x), Field.y(y));
        }
        particles.render(g, alpha);
    }
    // --8<-- [end:paint]

    /** The lawn as a chequerboard of two greens, inside a wall of grey stones; black above it. */
    private static void drawBackground(Graphics2D g, double width, double height) {
        g.setColor(Color.BLACK);
        g.fill(new Rectangle2D.Double(0, 0, width, Field.HUD));
        for (int y = 0; y < Field.ROWS; y++) {
            for (int x = 0; x < Field.COLUMNS; x++) {
                Rectangle2D.Double cell = new Rectangle2D.Double(Field.x(x), Field.y(y), C, C);
                if (Field.isWall(new Cell(x, y))) {
                    drawStone(g, cell, (x * 7 + y * 3) % 5);
                } else {
                    g.setColor((x + y) % 2 == 0 ? GRASS_LIGHT : GRASS_DARK);
                    g.fill(cell);
                }
            }
        }
        // A shadow of the wall on the lawn.
        g.setPaint(new GradientPaint(0, (float) Field.y(1), new Color(0, 0, 0, 90), 0, (float) Field.y(1.3),
                new Color(0, 0, 0, 0)));
        g.fill(new Rectangle2D.Double(Field.x(1), Field.y(1), Field.x(Field.COLUMNS - 2), C * 0.3));
    }

    private static void drawStone(Graphics2D g, Rectangle2D.Double cell, int shade) {
        Color stone = new Color(0x6A6E76 + shade * 0x060606);
        g.setColor(new Color(0x2A2C30));
        g.fill(cell);
        RoundRectangle2D.Double block = new RoundRectangle2D.Double(cell.x + 1, cell.y + 1, cell.width - 2,
                cell.height - 2, 5, 5);
        g.setPaint(new GradientPaint((float) cell.x, (float) cell.y, stone.brighter(), (float) cell.x,
                (float) (cell.y + cell.height), stone.darker()));
        g.fill(block);
    }

    private static void drawApple(Graphics2D g, double size, Color colour, Color shine) {
        double r = size * 0.38;
        double cx = size / 2;
        double cy = size * 0.55;
        g.setPaint(new RadialGradientPaint((float) (cx - r / 3), (float) (cy - r / 3), (float) (r * 1.3),
                new float[]{0, 1}, new Color[]{shine, colour}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(new Color(0x5A3A1A));
        g.setStroke(new BasicStroke((float) (size * 0.07)));
        g.draw(new Line2D.Double(cx, cy - r, cx + size * 0.05, cy - r - size * 0.15));
        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(cx + size * 0.04, cy - r - size * 0.06);
        leaf.quadTo(cx + size * 0.25, cy - r - size * 0.22, cx + size * 0.34, cy - r - size * 0.04);
        leaf.quadTo(cx + size * 0.18, cy - r + size * 0.04, cx + size * 0.04, cy - r - size * 0.06);
        g.setColor(new Color(0x3C9A2A));
        g.fill(leaf);
    }

    private static void drawSegment(Graphics2D g, double w, double h) {
        RoundRectangle2D.Double body = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, w * 0.45, h * 0.45);
        g.setPaint(new GradientPaint(0, 0, SKIN_LIGHT, (float) w, (float) h, SKIN));
        g.fill(body);
        g.setColor(SKIN_DARK);
        g.setStroke(new BasicStroke((float) (w * 0.06)));
        g.draw(body);
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(new Ellipse2D.Double(w * 0.3, h * 0.3, w * 0.4, h * 0.4));
    }

    /** The head, drawn looking right and turned to its direction. */
    private static void drawHead(Graphics2D g, double size, Direction direction) {
        AffineTransform saved = g.getTransform();
        double angle = switch (direction) {
            case RIGHT -> 0;
            case DOWN -> Math.PI / 2;
            case LEFT -> Math.PI;
            case UP -> -Math.PI / 2;
        };
        g.rotate(angle, size / 2, size / 2);
        RoundRectangle2D.Double head = new RoundRectangle2D.Double(0.5, 0.5, size - 1, size - 1, size * 0.7,
                size * 0.7);
        g.setPaint(new GradientPaint(0, 0, SKIN_LIGHT, 0, (float) size, SKIN));
        g.fill(head);
        g.setColor(SKIN_DARK);
        g.setStroke(new BasicStroke((float) (size * 0.06)));
        g.draw(head);
        for (double y : new double[]{size * 0.28, size * 0.72}) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(size * 0.52, y - size * 0.13, size * 0.26, size * 0.26));
            g.setColor(Color.BLACK);
            g.fill(new Ellipse2D.Double(size * 0.62, y - size * 0.07, size * 0.14, size * 0.14));
        }
        g.setTransform(saved);
    }
}
