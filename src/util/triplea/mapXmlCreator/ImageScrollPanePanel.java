package util.triplea.mapXmlCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.util.PointFileReaderWriter;
import util.image.FileOpen;

/**
 * Base class for image *Panel classes that show a map for defining certain map XML properties.
 * It contains the polygons and center points of the territories.
 */
public abstract class ImageScrollPanePanel {
  private static Font font = null;

  protected static MapXmlCreator mapXMLCreator;
  protected static Map<String, List<Polygon>> polygons = Maps.newHashMap(); // hash map for polygon
                                                                            // points
  public static boolean polygonsInvalid = true;

  private JPanel imagePanel;
  private static Map<String, Point> centers = Maps.newHashMap();

  protected void layout(final JPanel stepActionPanel) {
    final Dimension size = stepActionPanel.getSize();
    JScrollPane js = new JScrollPane(createImagePanel());
    js.setBorder(null);
    stepActionPanel.setLayout(new BorderLayout());
    stepActionPanel.add(js, BorderLayout.CENTER);
    stepActionPanel.setPreferredSize(size);
  }

  abstract protected void paintPreparation(final Map<String, Point> centers);

  abstract protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start);

  abstract protected void paintOwnSpecifics(final Graphics g, final Map<String, Point> centers);

  abstract protected void mouseClickedOnImage(final Map<String, Point> centers,
      final MouseEvent e);

  protected void repaint() {
    SwingUtilities.invokeLater(() -> imagePanel.repaint());
  }

  protected JPanel createImagePanel() {

    if (polygonsInvalid) {
      polygons.clear();
      polygonsInvalid = false;
    }
    if (polygons.isEmpty())
      MapXmlCreator.mapPolygonsFile = loadPolygons();

    final Image mapImage = Toolkit.getDefaultToolkit().getImage(MapXmlCreator.mapImageFile.getAbsolutePath());
    if (centers.isEmpty()) {
      loadCenters();
    }

    paintPreparation(centers);

    imagePanel = new JPanel() {
      private static final long serialVersionUID = -7130828419508975924L;

      @Override
      public void paint(final Graphics g) {
        // super.paint(g);
        final Rectangle clipBounds = g.getClipBounds();
        if (font == null)
          font = g.getFont();
        else
          g.setFont(font);
        // g.drawImage(mapImage, 0, 0, clipBounds.width, clipBounds.height, this);
        // g.drawImage(mapImage, 0, 0, this);
        g.drawImage(mapImage, clipBounds.x, clipBounds.y, clipBounds.x + clipBounds.width,
            clipBounds.y + clipBounds.height, clipBounds.x, clipBounds.y, clipBounds.x + clipBounds.width,
            clipBounds.y + clipBounds.height, this);
        paintOwnSpecifics(g, centers);
        g.setColor(Color.red);
        final FontMetrics fontMetrics = g.getFontMetrics();
        for (final Entry<String, Point> centerEntry : centers.entrySet()) {
          final String centerName = centerEntry.getKey();
          final Point item = centerEntry.getValue();
          final int x_text_start = item.x - centerName.length() / 2 * 5;
          final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
          final Rectangle boxRect = new Rectangle(Math.max(0, x_text_start - 2), Math.max(0, item.y - 6),
              (int) stringBounds.getWidth() + 4, (int) stringBounds.getHeight());
          if (clipBounds.intersects(boxRect)) {
            g.setColor(Color.white);
            g.fillRect(boxRect.x, boxRect.y, boxRect.width, boxRect.height);
            g.setColor(Color.red);
            g.drawString(centerName, Math.max(0, x_text_start), item.y + 5);
          }
          boxRect.width += boxRect.width;
          boxRect.height += boxRect.height;
          if (clipBounds.intersects(boxRect))
            paintCenterSpecifics(g, centerName, fontMetrics, item, x_text_start);
        }
      }

    };
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // set up the image panel size dimensions ...etc
        imagePanel
            .setPreferredSize(new Dimension(mapImage.getWidth(mapXMLCreator), mapImage.getHeight(mapXMLCreator)));
      }
    });
    imagePanel
        .setPreferredSize(new Dimension(mapImage.getWidth(mapXMLCreator), mapImage.getHeight(mapXMLCreator)));

    final MouseAdapter imageMouseAdapter = new MouseAdapter() {
      private final Cursor defCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
      private final Cursor hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      private final Point pp = new Point();

      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseClickedOnImage(centers, e);
      }

      @Override
      public void mouseDragged(final MouseEvent e) {
        final JViewport vport = (JViewport) imagePanel.getParent();
        final Point cp = e.getPoint();
        final Point vp = vport.getViewPosition();
        vp.translate(pp.x - cp.x, pp.y - cp.y);
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {

            imagePanel.scrollRectToVisible(new Rectangle(vp, vport.getSize()));
          }
        });
        pp.setLocation(cp);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        imagePanel.setCursor(hndCursor);
        pp.setLocation(e.getPoint());
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        imagePanel.setCursor(defCursor);
        pp.setLocation(e.getPoint());
      }
    };
    imagePanel.addMouseListener(imageMouseAdapter);
    imagePanel.addMouseMotionListener(imageMouseAdapter);

    return imagePanel;
  }

  protected JPanel getImagePanel() {
    return imagePanel;
  }

  private static File loadPolygons() {
    File file = null;
    if (MapXmlCreator.mapPolygonsFile == null) {
      if (MapXmlCreator.mapFolderLocation != null && MapXmlCreator.mapFolderLocation.exists())
        file = new File(MapXmlCreator.mapFolderLocation, "polygons.txt");
      if (file == null || !file.exists())
        file = new File(MapXmlCreator.mapImageFile.getParent() + File.separator + "polygons.txt");
    } else {
      file = MapXmlCreator.mapPolygonsFile;
    }
    if (MapXmlCreator.mapPolygonsFile != null
        || file.exists()
            && (MapXmlCreator.testMode || JOptionPane.showConfirmDialog(new JPanel(),
                "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territory shapes?",
                "File Suggestion",
                1) == 0)) {
      try {
        Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.INFO,
            "Load Polygons from " + file.getPath());
        polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(file.getPath()));
      } catch (final IOException ex1) {
        Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.SEVERE,
            "Something wrong with your Polygons file");
        ex1.printStackTrace();
      }
    } else {
      try {
        Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.FINE, "Select the Polygons file");
        final String polyPath =
            new FileOpen("Select A Polygon File", MapXmlCreator.mapFolderLocation, ".txt").getPathString();
        if (polyPath != null) {
          Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.FINE, "Polygons : " + polyPath);
          polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(polyPath));
        } else {
          Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.WARNING,
              "Polygons file not given. Will run regardless");
        }
      } catch (final IOException ex1) {
        Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.SEVERE,
            "Something wrong with your Polygons file");
        ex1.printStackTrace();
      }
    }
    return file;
  }

  final public static String territoryNameUnknown = "unknown";
  final public static String territorySeaZoneInfix = "Sea Zone";

  protected static String findTerritoryName(final Point p, final Map<String, List<Polygon>> polygons) {
    String seaName = territoryNameUnknown;
    // try to find a land or sea territory.
    // sea zones often surround a land territory
    for (final String name : polygons.keySet()) {
      final Collection<Polygon> polygonsCollection = polygons.get(name);
      for (final Polygon poly : polygonsCollection) {
        if (poly.contains(p)) {
          if (name.contains(territorySeaZoneInfix)) {
            seaName = name;
          } else {
            return name;
          }
        }
      }
    }
    return seaName;
  }

  private static Map<String, Point> loadCenters() {
    centers.clear();
    try {
      Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.INFO,
          "Load Centers from " + MapXmlCreator.mapCentersFile.getAbsolutePath());
      final FileInputStream in = new FileInputStream(MapXmlCreator.mapCentersFile);
      centers = PointFileReaderWriter.readOneToOne(in);
    } catch (final FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (final IOException ex) {
      ex.printStackTrace();
    } catch (final HeadlessException ex) {
      ex.printStackTrace();
    }
    return centers;
  }

  public ImageScrollPanePanel() {
    super();
  }

}
