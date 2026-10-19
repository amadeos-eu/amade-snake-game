import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Snake, a blue snake winding towards a red apple on a green square, as a PNG
 * of the given size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-snake-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-snake-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * The files are part of the repository; run this again only to change the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        ImageIO.write(icon(size), "png", file);
    }

    static BufferedImage icon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 48.0, size / 48.0);
        // The lawn.
        RoundRectangle2D.Double lawn = new RoundRectangle2D.Double(2, 2, 44, 44, 10, 10);
        g.setPaint(new GradientPaint(0, 2, new Color(0x6CB044), 0, 46, new Color(0x3F7A26)));
        g.fill(lawn);
        g.setColor(new Color(0x2A5A1A));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(lawn);
        // The snake: an S from the bottom left, its head near the apple.
        Path2D.Double body = new Path2D.Double();
        body.moveTo(9, 38);
        body.lineTo(30, 38);
        body.quadTo(37, 38, 37, 31);
        body.quadTo(37, 25, 30, 25);
        body.lineTo(18, 25);
        body.quadTo(11, 25, 11, 18);
        body.quadTo(11, 12, 18, 12);
        body.lineTo(24, 12);
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x1B4A94));
        g.draw(body);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x4A8AE0));
        g.draw(body);
        // The head with an eye.
        g.setColor(new Color(0x2E6FD0));
        g.fill(new Ellipse2D.Double(21, 7.5, 10, 9));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(25.5, 8.5, 3.5, 3.5));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(26.8, 9.3, 1.8, 1.8));
        // The apple.
        g.setPaint(new RadialGradientPaint(37, 10, 7, new float[]{0, 1},
                new Color[]{new Color(0xFF9A80), new Color(0xD8302A)}));
        g.fill(new Ellipse2D.Double(34, 8, 9, 9));
        g.setColor(new Color(0x3C9A2A));
        g.fill(new Ellipse2D.Double(38.5, 6, 4.5, 2.5));
        g.dispose();
        return image;
    }
}
