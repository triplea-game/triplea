import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Generates 48px NATO-style unit counters for the Small Front: Meuse map. */
public final class UnitGen {
  private static final int SIZE = 48;
  private static final int FRAME_X = 2;
  private static final int FRAME_Y = 11;
  private static final int FRAME_W = 43;
  private static final int FRAME_H = 30;
  private static final float STROKE = 2.2f;

  private record Palette(Color line, Color fill) {}

  private enum Symbol {
    INFANTRY,
    ARTILLERY,
    SELF_PROPELLED_ARTILLERY,
    ARMOUR,
    MECHANIZED,
    FIGHTER,
    AIRFIELD
  }

  private record UnitIcon(String fileName, Symbol symbol) {}

  private static final List<UnitIcon> ICONS =
      List.of(
          new UnitIcon("infantry.png", Symbol.INFANTRY),
          new UnitIcon("americanInfantry.png", Symbol.INFANTRY),
          new UnitIcon("artillery.png", Symbol.ARTILLERY),
          new UnitIcon("selfPropelledArtillery.png", Symbol.SELF_PROPELLED_ARTILLERY),
          new UnitIcon("armour.png", Symbol.ARMOUR),
          new UnitIcon("mechanized.png", Symbol.MECHANIZED),
          new UnitIcon("fighter.png", Symbol.FIGHTER),
          new UnitIcon("airfield.png", Symbol.AIRFIELD));

  private UnitGen() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: java tools/UnitGen.java <map-package-root>");
    }
    Path root = Path.of(args[0]);
    generateSide(root, "Germans", new Palette(new Color(0x202020), new Color(0xE5E1D8)));
    generateSide(root, "Americans", new Palette(new Color(0x244A73), new Color(0xE7EDF5)));
    System.out.println("generated=" + ICONS.size() * 2 + " NATO-style unit counters");
  }

  private static void generateSide(Path root, String player, Palette palette) throws IOException {
    Path directory = root.resolve("map/units").resolve(player);
    Files.createDirectories(directory);
    for (UnitIcon icon : ICONS) {
      ImageIO.write(draw(icon.symbol(), palette), "png", directory.resolve(icon.fileName()).toFile());
    }
  }

  private static BufferedImage draw(Symbol symbol, Palette palette) {
    BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Battalion echelon indicator: two vertical bars centred above the affiliation frame.
    g.setColor(palette.line());
    g.drawLine(20, 3, 20, 8);
    g.drawLine(27, 3, 27, 8);

    // A straight rectangular frame keeps the counter close to APP-6/MIL-STD-2525 unit framing.
    g.setColor(palette.fill());
    g.fillRect(FRAME_X, FRAME_Y, FRAME_W, FRAME_H);
    g.setColor(palette.line());
    g.drawRect(FRAME_X, FRAME_Y, FRAME_W, FRAME_H);

    switch (symbol) {
      case INFANTRY -> infantry(g);
      case ARTILLERY -> artillery(g);
      case SELF_PROPELLED_ARTILLERY -> selfPropelledArtillery(g);
      case ARMOUR -> armour(g);
      case MECHANIZED -> mechanized(g);
      case FIGHTER -> fighter(g);
      case AIRFIELD -> airfield(g);
    }
    g.dispose();
    return image;
  }

  private static void infantry(Graphics2D g) {
    g.drawLine(8, 15, 39, 37);
    g.drawLine(39, 15, 8, 37);
  }

  private static void artillery(Graphics2D g) {
    g.fill(new Ellipse2D.Double(19, 20, 10, 10));
  }

  private static void selfPropelledArtillery(Graphics2D g) {
    // Artillery dot over a horizontal tracked-mobility oval. The horizontal orientation fixes the
    // previous 90-degree-rotated self-propelled symbol.
    g.fill(new Ellipse2D.Double(20, 17, 8, 8));
    g.draw(new Ellipse2D.Double(11, 28, 26, 8));
  }

  private static void armour(Graphics2D g) {
    // NATO armour branch icon: a true horizontal oval, not a rounded rectangle or capsule.
    g.draw(new Ellipse2D.Double(9, 18, 30, 16));
  }

  private static void mechanized(Graphics2D g) {
    // Mechanized infantry combines the infantry cross with tracked mobility.
    g.drawLine(9, 15, 38, 32);
    g.drawLine(38, 15, 9, 32);
    g.draw(new Ellipse2D.Double(13, 29, 22, 7));
  }

  private static void airfield(Graphics2D g) {
    // A runway with centreline markings distinguishes the infrastructure counter from aircraft.
    g.drawRect(19, 15, 10, 22);
    g.drawLine(24, 17, 24, 21);
    g.drawLine(24, 24, 24, 28);
    g.drawLine(24, 31, 24, 35);
  }

  private static void fighter(Graphics2D g) {
    // Fixed-wing fighter/air icon rendered as the requested official-style infinity silhouette.
    Path2D.Double infinity = new Path2D.Double();
    infinity.moveTo(7, 26);
    infinity.curveTo(10, 17, 17, 17, 24, 26);
    infinity.curveTo(31, 35, 38, 35, 41, 26);
    infinity.curveTo(38, 17, 31, 17, 24, 26);
    infinity.curveTo(17, 35, 10, 35, 7, 26);
    g.draw(infinity);
  }
}
