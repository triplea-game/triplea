import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Generates the Meuse Corridor map package: Voronoi polygons from hand-placed sites, plus the
 * centers/polygons/place text files and the base tiles.
 */
public class MapGen {
  static final int W = 1600, H = 1000, MARGIN = 30, TILE = 256;
  /** Clears the unit box, whose top edge sits 23px above the site. */
  static final int NAME_ABOVE = 30;

  record Site(String name, int x, int y, String zone) {}

  // East (low x, German staging) to west (high x, the Meuse). Positions are jittered on purpose:
  // a regular lattice is what made the old map read as a rectangle.
  static final List<Site> SITES = List.of(
      // A: German staging, 4
      new Site("Blankenheim", 105, 195, "A"),
      new Site("Prum", 128, 430, "A"),
      new Site("Bitburg", 100, 660, "A"),
      new Site("Echternach", 133, 878, "A"),
      // B: the forest wall - only three cells, so every attack funnels through them
      new Site("Losheim Gap", 340, 232, "B"),
      new Site("Clervaux", 318, 545, "B"),
      new Site("Vianden", 349, 838, "B"),
      // C: road hubs, 5
      new Site("St. Vith", 566, 148, "C"),
      new Site("Houffalize", 588, 372, "C"),
      new Site("Wiltz", 551, 604, "C"),
      new Site("Bastogne", 604, 806, "C"),
      new Site("Martelange", 558, 952, "C"),
      // D: the open plateau, 9
      new Site("Vielsalm", 812, 108, "D"),
      new Site("Erezee", 792, 288, "D"),
      new Site("La Roche", 878, 432, "D"),
      new Site("Hotton", 806, 556, "D"),
      new Site("Nassogne", 901, 688, "D"),
      new Site("Saint-Hubert", 828, 812, "D"),
      new Site("Libramont", 884, 928, "D"),
      new Site("Marche", 1010, 498, "D"),
      new Site("Neufchateau", 1002, 872, "D"),
      // E: approaches, 7
      new Site("Durbuy", 1148, 172, "E"),
      new Site("Ciney", 1195, 372, "E"),
      new Site("Rochefort", 1128, 566, "E"),
      new Site("Beauraing", 1208, 736, "E"),
      new Site("Bertrix", 1140, 908, "E"),
      new Site("Havelange", 1252, 258, "E"),
      new Site("Wellin", 1268, 632, "E"),
      // F: Meuse crossings, 5
      new Site("Huy", 1452, 128, "F"),
      new Site("Andenne", 1488, 318, "F"),
      new Site("Namur", 1436, 494, "F"),
      new Site("Dinant", 1481, 690, "F"),
      new Site("Givet", 1444, 892, "F"));

  // Terrain colours, matching the palette the Ardennes prototype established.
  static final Map<String, Color> TERRAIN_FILL = Map.of(
      "Open", new Color(0xD5CEA3), "Forest", new Color(0x8FA678), "Town", new Color(0xB6A398));

  /** Fallback when no XML is supplied; genxml.py derives terrain from the same bands. */
  static final Map<String, String> ZONE_TERRAIN = Map.of(
      "A", "Open", "B", "Forest", "C", "Town", "D", "Open", "E", "Open", "F", "Town");

  public static void main(String[] args) throws Exception {
    Path out = Path.of(args[0]);
    Files.createDirectories(out.resolve("map/baseTiles"));

    // Roads, objectives and terrain live in the generated XML, so this reads them back rather than
    // keeping a second copy that could drift. Absent on the first pass, which produces the
    // adjacency the XML generator needs.
    Graph graph = args.length > 1 ? Graph.fromXml(Path.of(args[1])) : Graph.empty();

    List<List<double[]>> cells = new ArrayList<>();
    for (Site s : SITES) cells.add(voronoi(s));

    // centers.txt
    StringBuilder centers = new StringBuilder();
    for (Site s : SITES) centers.append(s.name()).append("  (").append(s.x()).append(",").append(s.y()).append(")\n");
    Files.writeString(out.resolve("map/centers.txt"), centers.toString());

    // polygons.txt
    StringBuilder polys = new StringBuilder();
    for (int i = 0; i < SITES.size(); i++) {
      polys.append(SITES.get(i).name()).append("  < ");
      for (double[] p : cells.get(i)) polys.append(" (").append(Math.round(p[0])).append(",").append(Math.round(p[1])).append(")");
      polys.append(" >\n");
    }
    Files.writeString(out.resolve("map/polygons.txt"), polys.toString());

    // place.txt: a 50x47 unit box centred on each site
    StringBuilder place = new StringBuilder();
    for (Site s : SITES) {
      int x = s.x() - 25, y = s.y() - 23;
      place.append(s.name())
          .append("  (").append(x).append(",").append(y).append(")")
          .append("  (").append(x + 50).append(",").append(y).append(")")
          .append("  (").append(x).append(",").append(y + 47).append(")")
          .append("  (").append(x + 50).append(",").append(y + 47).append(")\n");
    }
    Files.writeString(out.resolve("map/place.txt"), place.toString());

    // name_place.txt: without it the engine centres each name in the territory's bounding box,
    // which for these cells lands under the unit stack. Names go above it instead. The engine
    // measures with Arial Bold 12 (MapImage.getPropertyMapFont), so measure with the same font.
    Font mapFont = new Font("Arial", Font.BOLD, 12);
    FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        .createGraphics()
        .getFontMetrics(mapFont);
    StringBuilder names = new StringBuilder();
    for (Site s : SITES) {
      names.append(s.name())
          .append("  (").append(s.x() - fm.stringWidth(s.name()) / 2)
          .append(",").append(s.y() - NAME_ABOVE).append(")\n");
    }
    Files.writeString(out.resolve("map/name_place.txt"), names.toString());

    // vc.txt: the marker's top-left. Left of the unit box, so it clears both units and the name.
    StringBuilder vc = new StringBuilder();
    for (Site s : SITES) {
      if (!graph.objectives().contains(s.name())) continue;
      vc.append(s.name())
          .append("  (").append(s.x() - 46).append(",").append(s.y() - 11).append(")\n");
    }
    if (!graph.objectives().isEmpty()) {
      Files.writeString(out.resolve("map/vc.txt"), vc.toString());
    }

    // Voronoi adjacency: cells that share a boundary segment of real length.
    StringBuilder adj = new StringBuilder();
    for (int i = 0; i < SITES.size(); i++)
      for (int j = i + 1; j < SITES.size(); j++)
        if (sharedEdgeLength(cells.get(i), cells.get(j)) > 24)
          adj.append(SITES.get(i).name()).append(" | ").append(SITES.get(j).name()).append("\n");
    Files.writeString(out.resolve("adjacency.txt"), adj.toString());

    render(out, cells, graph);
    System.out.println(
        "sites=" + SITES.size()
            + " adjacencyCandidates=" + adj.toString().lines().count()
            + " roadsDrawn=" + graph.roads().size()
            + " objectives=" + graph.objectives().size());
  }

  /** Roads, objectives and terrain, read back out of the generated game XML. */
  record Graph(List<String[]> roads, Set<String> objectives, Map<String, String> terrain) {
    static Graph empty() {
      return new Graph(List.of(), Set.of(), Map.of());
    }

    static Graph fromXml(Path xml) throws Exception {
      var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(xml.toFile());
      List<String[]> roads = new ArrayList<>();
      Set<String> objectives = new LinkedHashSet<>();
      Map<String, String> terrain = new LinkedHashMap<>();
      var attachments = doc.getElementsByTagName("attachment");
      for (int i = 0; i < attachments.getLength(); i++) {
        var element = (org.w3c.dom.Element) attachments.item(i);
        String attachTo = element.getAttribute("attachTo");
        String javaClass = element.getAttribute("javaClass");
        var options = element.getElementsByTagName("option");
        for (int j = 0; j < options.getLength(); j++) {
          var option = (org.w3c.dom.Element) options.item(j);
          String name = option.getAttribute("name");
          String value = option.getAttribute("value");
          if (javaClass.endsWith("SupplyTerritoryAttachment") && name.equals("roadConnection")) {
            roads.add(new String[] {attachTo, value});
          } else if (javaClass.endsWith("TerritoryAttachment")) {
            if (name.equals("victoryCity") && !value.equals("0")) {
              objectives.add(attachTo);
            } else if (name.equals("territoryEffect")) {
              terrain.put(attachTo, value);
            }
          }
        }
      }
      return new Graph(List.copyOf(roads), Set.copyOf(objectives), Map.copyOf(terrain));
    }
  }

  /** Cell = map rect clipped by the perpendicular bisector against every other site. */
  static List<double[]> voronoi(Site s) {
    List<double[]> poly = new ArrayList<>(List.of(
        new double[] {MARGIN, MARGIN}, new double[] {W - MARGIN, MARGIN},
        new double[] {W - MARGIN, H - MARGIN}, new double[] {MARGIN, H - MARGIN}));
    for (Site o : SITES) {
      if (o == s) continue;
      double dx = o.x() - s.x(), dy = o.y() - s.y();
      double mx = (o.x() + s.x()) / 2.0, my = (o.y() + s.y()) / 2.0;
      // Keep the half-plane containing s: dot(p - m, d) <= 0
      poly = clip(poly, dx, dy, dx * mx + dy * my);
      if (poly.isEmpty()) break;
    }
    return poly;
  }

  /** Sutherland-Hodgman against the half-plane a*x + b*y <= c. */
  static List<double[]> clip(List<double[]> poly, double a, double b, double c) {
    List<double[]> out = new ArrayList<>();
    for (int i = 0; i < poly.size(); i++) {
      double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
      double dp = a * p[0] + b * p[1] - c, dq = a * q[0] + b * q[1] - c;
      if (dp <= 0) out.add(p);
      if ((dp < 0 && dq > 0) || (dp > 0 && dq < 0)) {
        double t = dp / (dp - dq);
        out.add(new double[] {p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1])});
      }
    }
    return out;
  }

  static double sharedEdgeLength(List<double[]> a, List<double[]> b) {
    double total = 0;
    for (int i = 0; i < a.size(); i++) {
      double[] p1 = a.get(i), p2 = a.get((i + 1) % a.size());
      for (int j = 0; j < b.size(); j++) {
        double[] q1 = b.get(j), q2 = b.get((j + 1) % b.size());
        if (near(p1, q2) && near(p2, q1)) total += dist(p1, p2);
        else if (near(p1, q1) && near(p2, q2)) total += dist(p1, p2);
      }
    }
    return total;
  }

  static boolean near(double[] p, double[] q) { return dist(p, q) < 1.5; }
  static double dist(double[] p, double[] q) { return Math.hypot(p[0] - q[0], p[1] - q[1]); }

  /**
   * TripleA fills every land polygon with the owner's colour at POLYGONS_LEVEL, which sits directly
   * on top of BASE_MAP_LEVEL. Anything drawn into a base tile inside a polygon is therefore invisible
   * in game. Terrain art belongs in a relief tile, which draws at RELIEF_LEVEL above that fill, and
   * has to be translucent for the owner colour to read through it. Names and victory-city markers are
   * left to the engine, which draws them higher still.
   */
  static void render(Path out, List<List<double[]>> cells, Graph graph) throws IOException {
    BufferedImage base = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = base.createGraphics();
    hints(g);
    g.setColor(new Color(0xE8E0C8));
    g.fillRect(0, 0, W, H);
    for (int i = 0; i < SITES.size(); i++) {
      g.setColor(TERRAIN_FILL.getOrDefault(terrainOf(SITES.get(i), graph), TERRAIN_FILL.get("Open")));
      g.fillPolygon(awt(cells.get(i)));
    }
    g.setColor(new Color(0x20, 0x20, 0x20));
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
    g.drawString("SMALL FRONT: MEUSE CORRIDOR — PROTOTYPE 0.1", 40, 24);
    g.dispose();

    BufferedImage relief = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    Graphics2D r = relief.createGraphics();
    hints(r);
    // Terrain textures. Everything here is translucent so the owner colour still reads through, but
    // each terrain now gets a colour wash plus its own texture so Forest, Town and Open are told
    // apart at a glance over both the grey and the blue owner fills.
    for (int i = 0; i < SITES.size(); i++) {
      String terrain = terrainOf(SITES.get(i), graph);
      Polygon cell = awt(cells.get(i));
      Rectangle bb = cell.getBounds();
      Shape old = r.getClip();
      r.setClip(cell);
      switch (terrain) {
        case "Forest" -> {
          r.setColor(new Color(46, 92, 42, 95));
          r.fillPolygon(cell);
          r.setColor(new Color(22, 54, 24, 135));
          for (int x = bb.x; x < bb.x + bb.width; x += 15)
            for (int y = bb.y; y < bb.y + bb.height; y += 15) {
              int jx = (x * 13 + y * 7) % 7 - 3, jy = (x * 7 + y * 13) % 7 - 3;
              r.fillOval(x + jx, y + jy, 8, 8);
            }
        }
        case "Town" -> {
          r.setColor(new Color(206, 170, 138, 80));
          r.fillPolygon(cell);
          r.setColor(new Color(84, 64, 50, 150));
          for (int x = bb.x; x < bb.x + bb.width; x += 13)
            for (int y = bb.y; y < bb.y + bb.height; y += 13) r.fillRect(x, y, 7, 7);
        }
        default -> {
          r.setColor(new Color(214, 202, 150, 55));
          r.fillPolygon(cell);
          r.setColor(new Color(150, 140, 96, 70));
          for (int y = bb.y; y < bb.y + bb.height; y += 12) r.drawLine(bb.x, y, bb.x + bb.width, y);
        }
      }
      r.setClip(old);
    }
    // Rivers: the Meuse frames the western objective line, the Our the eastern forest wall. Offset
    // sideways from the town centres so the water clears the unit stacks that sit on the sites.
    river(r, 52, "Huy", "Andenne", "Namur", "Dinant", "Givet");
    river(r, -44, "Losheim Gap", "Clervaux", "Vianden");
    // Roads: a dark casing under a light metalled centre reads as a road, not just a line.
    for (String[] road : graph.roads()) {
      Site a = site(road[0]), b = site(road[1]);
      if (a == null || b == null) continue;
      r.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      r.setColor(new Color(70, 52, 34, 150));
      r.drawLine(a.x(), a.y(), b.x(), b.y());
      r.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      r.setColor(new Color(238, 228, 202, 230));
      r.drawLine(a.x(), a.y(), b.x(), b.y());
    }
    r.dispose();

    tiles(base, out.resolve("map/baseTiles"));
    tiles(relief, out.resolve("map/reliefTiles"));

    BufferedImage small = new BufferedImage(W / 10, H / 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D sg = small.createGraphics();
    sg.drawImage(base, 0, 0, W / 10, H / 10, null);
    sg.dispose();
    ImageIO.write(small, "png", out.resolve("map/smallMap.png").toFile());

    // A documentation preview only: the game never composites the layers this way.
    BufferedImage preview = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    Graphics2D pg = preview.createGraphics();
    hints(pg);
    pg.drawImage(base, 0, 0, null);
    pg.drawImage(relief, 0, 0, null);
    pg.setColor(new Color(0x33, 0x33, 0x33, 170));
    pg.setStroke(new BasicStroke(2f));
    for (List<double[]> cell : cells) pg.drawPolygon(awt(cell));
    for (Site s : SITES) label(pg, s, terrainOf(s, graph), graph.objectives().contains(s.name()));
    pg.dispose();
    ImageIO.write(preview, "png", out.resolve("preview.png").toFile());

    vcMarker(out.resolve("map/misc/vc.png"));
  }

  static void hints(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  static void tiles(BufferedImage img, Path dir) throws IOException {
    Files.createDirectories(dir);
    for (int ty = 0; ty * TILE < H; ty++)
      for (int tx = 0; tx * TILE < W; tx++) {
        int w = Math.min(TILE, W - tx * TILE), h = Math.min(TILE, H - ty * TILE);
        ImageIO.write(img.getSubimage(tx * TILE, ty * TILE, w, h), "png",
            dir.resolve(tx + "_" + ty + ".png").toFile());
      }
  }

  /** The engine stamps this at every vc.txt point; it does not ship a default. */
  static void vcMarker(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    BufferedImage img = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    hints(g);
    Polygon diamond = new Polygon(new int[] {11, 21, 11, 1}, new int[] {1, 11, 21, 11}, 4);
    g.setColor(new Color(0xD9A521));
    g.fillPolygon(diamond);
    g.setColor(new Color(0x6B5110));
    g.setStroke(new BasicStroke(1.5f));
    g.drawPolygon(diamond);
    g.dispose();
    ImageIO.write(img, "png", file.toFile());
  }

  /** Preview only. In game the engine draws names itself, above the owner colour. */
  static void label(Graphics2D g, Site s, String terrain, boolean objective) {
    int y = s.y() - (objective ? 22 : 12);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
    FontMetrics fm = g.getFontMetrics();
    String name = s.name().toUpperCase(Locale.ROOT);
    int x = s.x() - fm.stringWidth(name) / 2;
    // A light halo keeps the name readable wherever the cell colour lands.
    g.setColor(new Color(255, 255, 255, 150));
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++) g.drawString(name, x + dx, y + dy);
    g.setColor(new Color(0x1A, 0x1A, 0x1A));
    g.drawString(name, x, y);

    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    fm = g.getFontMetrics();
    String type = terrain.toUpperCase(Locale.ROOT);
    g.setColor(new Color(0x44, 0x44, 0x44));
    g.drawString(type, s.x() - fm.stringWidth(type) / 2, y + 12);
  }

  static String terrainOf(Site s, Graph graph) {
    return graph.terrain().getOrDefault(s.name(), ZONE_TERRAIN.getOrDefault(s.zone(), "Open"));
  }

  static Site site(String name) {
    return SITES.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
  }

  /**
   * Draws a translucent river through the named sites, offset horizontally by {@code dx} so the
   * water clears the town/unit boxes that sit on the sites. A darker channel under a brighter core
   * gives it depth without hiding the owner colour underneath.
   */
  static void river(Graphics2D r, int dx, String... names) {
    int n = names.length;
    int[] xs = new int[n], ys = new int[n];
    for (int i = 0; i < n; i++) {
      Site s = site(names[i]);
      xs[i] = s.x() + dx;
      ys[i] = s.y();
    }
    r.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    r.setColor(new Color(60, 104, 158, 160));
    for (int i = 0; i < n - 1; i++) r.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
    r.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    r.setColor(new Color(122, 168, 214, 175));
    for (int i = 0; i < n - 1; i++) r.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
  }

  static Polygon awt(List<double[]> pts) {
    Polygon p = new Polygon();
    for (double[] q : pts) p.addPoint((int) Math.round(q[0]), (int) Math.round(q[1]));
    return p;
  }
}
