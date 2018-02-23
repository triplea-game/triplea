package tools.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import tools.util.ToolLogger;

/**
 * Utility to break a map into polygons.
 * Inputs - a map with 1 pixel wide borders
 * - a list of centers - this is used to guess the territory name and to verify the
 * - territory name entered
 * Outputs - a list of polygons for each country
 */
public class PolygonGrabber extends JFrame {
  private static final long serialVersionUID = 6381498094805120687L;
  private static boolean islandMode;
  private final JCheckBoxMenuItem modeItem;
  // the current set of polyongs
  private List<Polygon> current;
  // holds the map image
  // private Image m_image;
  private BufferedImage bufferedImage;
  // maps String -> List of polygons
  private Map<String, List<Polygon>> polygons = new HashMap<>();
  // holds the centers for the polygons
  private Map<String, Point> centers;
  private final JLabel location = new JLabel();
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";

  /**
   * Main program begins here.
   * Asks the user to select the map then runs the
   * the actual polygon grabber program.
   *
   * @param args The command line arguments.
   */
  public static void main(final String[] args) {
    handleCommandLineArgs(args);
    ToolLogger.info("Select the map");
    final FileOpen mapSelection = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png");
    final String mapName = mapSelection.getPathString();
    if ((mapFolderLocation == null) && (mapSelection.getFile() != null)) {
      mapFolderLocation = mapSelection.getFile().getParentFile();
    }
    if (mapName != null) {
      ToolLogger.info("Map : " + mapName);
      final PolygonGrabber grabber = new PolygonGrabber(mapName);
      grabber.setSize(800, 600);
      grabber.setLocationRelativeTo(null);
      grabber.setVisible(true);
      JOptionPane.showMessageDialog(grabber,
          new JLabel("<html>" + "This is the PolygonGrabber, it will create a polygons.txt file for you. "
              + "<br>In order to run this, you must already have created a center.txt file. "
              + "<br>Please click near the center of every single territory and sea zone on your map. "
              + "<br>The grabber will then fill in the territory based on the borders it finds."
              + "<br>If the territory shape or borders do not match what you intend, then your borders "
              + "<br>might have a gap or differently colored pixel in the border."
              + "<br>These borders will define the shape of the territory in TripleA."
              + "<br><br>When a territory is inside of another territory, you can turn on 'island mode' to be able to "
              + "see it."
              + "<br><br>You can also load an existing polygons.txt file, then make modifications to it, then save it "
              + "again."
              + "<br><br>LEFT CLICK = fill in a territory's borders."
              + "<br><br>Holding CTRL/SHIFT while LEFT CLICKING = add multiple territories together (eg: islands)."
              + "<br><br>RIGHT CLICK = save or replace those borders for that territory."
              + "<br><br>When finished, save the polygons and exit." + "</html>"));
    } else {
      ToolLogger.info("No Image Map Selected. Shutting down.");
      System.exit(0);
    }
  }

  /**
   * Constructor PolygonGrabber(java.lang.String)
   * Asks user to specify a file with center points. If not
   * program will exit. We setup the mouse listenrs and toolbars
   * and load the actual image of the map here.
   *
   * @param mapName Path to image map.
   */
  public PolygonGrabber(final String mapName) {
    super("Polygon grabber");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    File file = null;
    if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
      file = new File(mapFolderLocation, "centers.txt");
    }
    if ((file == null) || !file.exists()) {
      file = new File(new File(mapName).getParent() + File.separator + "centers.txt");
    }
    if (file.exists() && (JOptionPane.showConfirmDialog(new JPanel(),
        "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories "
            + "names?",
        "File Suggestion", 1) == 0)) {
      try (InputStream is = new FileInputStream(file.getPath())) {
        ToolLogger.info("Centers : " + file.getPath());
        centers = PointFileReaderWriter.readOneToOne(is);
      } catch (final IOException e) {
        ToolLogger.error("Something wrong with Centers file", e);
      }
    } else {
      try {
        ToolLogger.info("Select the Centers file");
        final String centerPath = new FileOpen("Select A Center File", mapFolderLocation, ".txt").getPathString();
        if (centerPath != null) {
          ToolLogger.info("Centers : " + centerPath);
          try (InputStream is = new FileInputStream(centerPath)) {
            centers = PointFileReaderWriter.readOneToOne(is);
          }
        } else {
          ToolLogger.info("You must specify a centers file.");
          ToolLogger.info("Shutting down.");
          System.exit(0);
        }
      } catch (final IOException e) {
        ToolLogger.error("Something wrong with Centers file", e);
        System.exit(0);
      }
    }
    createImage(mapName);
    final JPanel imagePanel = createMainPanel();
    /*
     * Add a mouse listener to show
     * X : Y coordinates on the lower
     * left corner of the screen.
     */
    imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        location.setText("x:" + e.getX() + " y:" + e.getY());
      }
    });
    /*
     * Add a mouse listener to monitor
     * for right mouse button being
     * clicked.
     */
    imagePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseEvent(e.getPoint(), e.isControlDown() || e.isShiftDown(), SwingUtilities.isRightMouseButton(e));
      }
    });
    // set up the image panel size dimensions ...etc
    imagePanel.setMinimumSize(new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
    imagePanel.setPreferredSize(new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
    imagePanel.setMaximumSize(new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
    // set up the layout manager
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(imagePanel), BorderLayout.CENTER);
    this.getContentPane().add(location, BorderLayout.SOUTH);
    // set up the actions
    final Action openAction = SwingAction.of("Load Polygons", e -> loadPolygons());
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Polygon Points FIle");
    final Action saveAction = SwingAction.of("Save Polygons", e -> savePolygons());
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Polygon Points To File");
    final Action exitAction = SwingAction.of("Exit", e -> System.exit(0));
    exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
    final Action autoAction = SwingAction.of("Auto Find Polygons", e -> {
      JOptionPane.showMessageDialog(null,
          new JLabel("<html>"
              + "You will need to check and go back and do some polygons manually, as Auto does not catch them all. "
              + "<br>Also, if a territory has more than 1 part (like an island chain), you will need to go back and "
              + "<br>redo the entire territory chain using CTRL + Click in order to capture each part of the territory."
              + "</html>"));
      current = new ArrayList<>();
      final BufferedImage imageCopy = new BufferedImage(bufferedImage.getWidth(null),
          bufferedImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      final Graphics g = imageCopy.getGraphics();
      g.drawImage(bufferedImage, 0, 0, null);
      for (final String territoryName : centers.keySet()) {
        final Point center = centers.get(territoryName);
        ToolLogger.info("Detecting Polygon for:" + territoryName);
        final Polygon p = findPolygon(center.x, center.y);
        // test if the poly contains the center point (this often fails when there is an island right above (because
        // findPolygon will grab
        // the island instead)
        if (!p.contains(center)) {
          continue;
        }
        // test if this poly contains any other centers, and if so do not do this one. let the user manually do it to
        // make sure it gets
        // done properly
        boolean hasIslands = false;
        for (final Point otherCenterPoint : centers.values()) {
          if (center.equals(otherCenterPoint)) {
            continue;
          }
          if (p.contains(otherCenterPoint)) {
            hasIslands = true;
            break;
          }
        }
        if (hasIslands) {
          continue;
        }
        // some islands do not have centers on them because they are island chains that are also part of an island or
        // territory touching a
        // sidewall or outside of this polygon. we should still skip them.
        if (doesPolygonContainAnyBlackInside(p, imageCopy, g)) {
          continue;
        }
        final List<Polygon> polys = new ArrayList<>();
        polys.add(p);
        polygons.put(territoryName, polys);
      }
      g.dispose();
      imageCopy.flush();
      repaint();

    });
    autoAction.putValue(Action.SHORT_DESCRIPTION, "Autodetect Polygons around Centers");
    // set up the menu items
    final JMenuItem openItem = new JMenuItem(openAction);
    openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    final JMenuItem saveItem = new JMenuItem(saveAction);
    saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    final JMenuItem exitItem = new JMenuItem(exitAction);
    islandMode = false;
    modeItem = new JCheckBoxMenuItem("Island Mode", false);
    modeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        islandMode = modeItem.getState();
        repaint();
      }
    });
    // set up the menu bar
    final JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');
    fileMenu.add(openItem);
    fileMenu.add(saveItem);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);
    final JMenu editMenu = new JMenu("Edit");
    final JMenuItem autoItem = new JMenuItem(autoAction);
    editMenu.setMnemonic('E');
    editMenu.add(modeItem);
    editMenu.add(autoItem);
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
  } // end constructor

  /**
   * We create the image of the map here and
   * assure that it is loaded properly.
   *
   * @param mapName
   *        .lang.String mapName the path of the image map
   */
  private void createImage(final String mapName) {
    final Image image = Toolkit.getDefaultToolkit().createImage(mapName);
    Util.ensureImageLoaded(image);
    bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    final Graphics g = bufferedImage.getGraphics();
    g.drawImage(image, 0, 0, this);
    g.dispose();
  }

  /**
   * Creates a JPanel to be used. Dictates how the map is
   * painted. Current problem is that islands inside sea
   * zones are not recognized when filling in the sea zone
   * with a color, so we just outline in red instead of
   * filling. We fill for selecting territories only for
   * ease of use. We use var "islandMode" to dictate how
   * to paint the map.
   *
   * @return javax.swing.JPanel the newly create panel
   */
  private JPanel createMainPanel() {
    final JPanel imagePanel = new JPanel() {
      private static final long serialVersionUID = 4106539186003148628L;

      @Override
      public void paint(final Graphics g) {
        // super.paint(g);
        g.drawImage(bufferedImage, 0, 0, this);
        g.setColor(Color.red);
        for (final Entry<String, List<Polygon>> entry : polygons.entrySet()) {
          if (islandMode) {
            for (final Polygon item : entry.getValue()) {
              g.drawPolygon(item.xpoints, item.ypoints, item.npoints);
            } // while
          } else {
            for (final Polygon item : entry.getValue()) {
              g.setColor(Color.yellow);
              g.fillPolygon(item.xpoints, item.ypoints, item.npoints);
              g.setColor(Color.black);
              g.drawPolygon(item.xpoints, item.ypoints, item.npoints);
            } // while
          }
        } // while
        g.setColor(Color.red);
        if (current != null) {
          for (final Polygon item : current) {
            g.fillPolygon(item.xpoints, item.ypoints, item.npoints);
          } // while
        } // if
      } // paint
    };
    return imagePanel;
  }

  /**
   * Saves the polygons to disk.
   */
  private void savePolygons() {
    final String polyName =
        new FileSave("Where To Save Polygons.txt ?", "polygons.txt", mapFolderLocation).getPathString();
    if (polyName == null) {
      return;
    }
    try (OutputStream out = new FileOutputStream(polyName)) {
      PointFileReaderWriter.writeOneToManyPolygons(out, polygons);
      ToolLogger.info("Data written to :" + new File(polyName).getCanonicalPath());
    } catch (final IOException e) {
      ToolLogger.error("Failed to save polygons: " + polyName, e);
    }
  }

  /**
   * Loads a pre-defined file with map polygon points.
   */
  private void loadPolygons() {
    ToolLogger.info("Load a polygon file");
    final String polyName = new FileOpen("Load A Polygon File", mapFolderLocation, ".txt").getPathString();
    if (polyName == null) {
      return;
    }
    try (InputStream in = new FileInputStream(polyName)) {
      polygons = PointFileReaderWriter.readOneToManyPolygons(in);
    } catch (final IOException e) {
      ToolLogger.error("Failed to load polygons: " + polyName, e);
      System.exit(0);
    }
    repaint();
  }

  private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse) {
    final Polygon p = findPolygon(point.x, point.y);
    if (p == null) {
      return;
    }
    if (rightMouse && (current != null)) { // right click and list of polys is not empty
      doneCurrentGroup();
    } else if (pointInCurrentPolygon(point)) { // point clicked is already highlighted
      ToolLogger.info("rejecting");
      return;
    } else if (ctrlDown) {
      if (current == null) {
        current = new ArrayList<>();
      }
      current.add(p);
    } else {
      current = new ArrayList<>();
      current.add(p);
    }
    repaint();
  }

  /**
   * returns false if there is no points in a current polygon.
   * returns true if there is.
   */
  private boolean pointInCurrentPolygon(final Point p) {
    if (current == null) {
      return false;
    }
    for (final Polygon item : current) {
      if (item.contains(p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Does something with respect to check if the name of a territory is valid or not.
   */
  private void doneCurrentGroup() {
    final JTextField text = new JTextField();
    guessCountryName(text, centers.entrySet());
    final int option = JOptionPane.showConfirmDialog(this, text);
    // cancel = 2
    // no = 1
    // yes = 0
    if (option == 0) {
      if (!centers.keySet().contains(text.getText())) {
        // not a valid name
        JOptionPane.showMessageDialog(this, "not a valid name");
        current = null;
        return;
      }
      polygons.put(text.getText(), new ArrayList<>(current));
      current = null;
    } else if (option > 0) {
      current = null;
    } else {
      ToolLogger.info("something very invalid");
    }
  }

  /**
   * Guess the country name based on the location of the previous centers.
   */
  private void guessCountryName(final JTextField text, final Iterable<Entry<String, Point>> centersiter) {
    final List<String> options = new ArrayList<>();
    for (final Entry<String, Point> item : centersiter) {
      final Point p = new Point(item.getValue());
      for (final Polygon polygon : current) {
        if (polygon.contains(p)) {
          options.add(item.getKey());
        } // if
      } // while
    } // while
    if (!options.isEmpty()) {
      Collections.shuffle(options);
      text.setText(options.get(0));
    }
  }

  /**
   * Checks to see if the given point is of color black.
   */
  private boolean isBlack(final Point p) {
    return isBlack(p.x, p.y);
  }

  /**
   * Checks to see if the x/y coordinates from a given point
   * are inbounds and if so is it black.
   */
  private boolean isBlack(final int x, final int y) {
    if (!inBounds(x, y)) {
      // not inbounds, can't be black
      return false;
    }
    // gets ARGB integer value and we LOGICAL AND mask it
    // with ARGB value of 00,FF,FF,FF to determine if it
    // it black or not.
    // maybe here ?
    return (bufferedImage.getRGB(x, y) & 0x00FFFFFF) == 0;
  }

  private static boolean isBlack(final int x, final int y, final BufferedImage bufferedImage) {
    if (!inBounds(x, y, bufferedImage)) {
      // not inbounds, can't be black
      return false;
    }
    // gets ARGB integer value and we LOGICAL AND mask it
    // with ARGB value of 00,FF,FF,FF to determine if it
    // it black or not.
    // maybe here ?
    return (bufferedImage.getRGB(x, y) & 0x00FFFFFF) == 0;
  }

  /**
   * Checks if the given x/y coordinate point is inbounds or not.
   */
  private boolean inBounds(final int x, final int y) {
    return (x >= 0) && (x < bufferedImage.getWidth(null)) && (y >= 0) && (y < bufferedImage.getHeight(null));
  }

  private static boolean inBounds(final int x, final int y, final Image image) {
    return (x >= 0) && (x < image.getWidth(null)) && (y >= 0) && (y < image.getHeight(null));
  }

  /**
   * Moves to a specified direction.
   * Directions
   * 0 - North
   * 1 - North east
   * 2 - East
   * 3 - South east
   * 4 - South
   * 5 - South west
   * 6 - West
   * 7 - North west
   */
  private static void move(final Point p, final int direction) {
    if ((direction < 0) || (direction > 7)) {
      throw new IllegalArgumentException("Not a direction :" + direction);
    }
    if ((direction == 1) || (direction == 2) || (direction == 3)) {
      p.x++;
    } else if ((direction == 5) || (direction == 6) || (direction == 7)) {
      p.x--;
    }
    if ((direction == 5) || (direction == 4) || (direction == 3)) {
      p.y++;
    } else if ((direction == 7) || (direction == 0) || (direction == 1)) {
      p.y--;
    }
  }

  // used below
  private final Point testPoint = new Point();

  /**
   * Checks to see if the direction we're going is on the edge.
   */
  private boolean isOnEdge(final int direction, final Point currentPoint) {
    testPoint.setLocation(currentPoint);
    move(testPoint, direction);
    return (testPoint.x == 0) || (testPoint.y == 0) || (testPoint.y == bufferedImage.getHeight(this))
        || (testPoint.x == bufferedImage.getWidth(this)) || isBlack(testPoint);
  }

  private static boolean doesPolygonContainAnyBlackInside(final Polygon poly, final BufferedImage imageCopy,
      final Graphics imageCopyGraphics) {
    // we would like to just test if each point is both black and contained within the polygon, but contains counts the
    // borders,
    // so we have to turn the border edges a different color (then later back to black again) using a copy of the image
    // final BufferedImage testImage = new BufferedImage(bufferedImage.getWidth(null),
    // bufferedImage.getHeight(null),
    // BufferedImage.TYPE_INT_ARGB);
    imageCopyGraphics.setColor(Color.GREEN);
    imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
    final Rectangle rect = poly.getBounds();
    for (int x = rect.x; x < (rect.x + rect.width); x++) {
      for (int y = rect.y; y < (rect.y + rect.height); y++) {
        if (isBlack(x, y, imageCopy)) {
          if (poly.contains(new Point(x, y))) {
            imageCopyGraphics.setColor(Color.BLACK);
            imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
            // testImage.flush();
            return true;
          }
        }
      }
    }
    imageCopyGraphics.setColor(Color.BLACK);
    imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
    // testImage.flush();
    return false;
  }

  /**
   * Algorithm to find a polygon given a x/y coordinates and
   * returns the found polygon.
   */
  private Polygon findPolygon(final int x, final int y) {
    // walk up, find the first black point
    final Point startPoint = new Point(x, y);
    while (inBounds(startPoint.x, startPoint.y - 1) && !isBlack(startPoint.x, startPoint.y)) {
      startPoint.y--;
    }
    final List<Point> points = new ArrayList<>(100);
    points.add(new Point(startPoint));
    int currentDirection = 2;
    Point currentPoint = new Point(startPoint);
    int iterCount = 0;
    while (!currentPoint.equals(startPoint) || (points.size() == 1)) {
      iterCount++;
      if (iterCount > 100000) {
        JOptionPane.showMessageDialog(this,
            "Failed to grab the polygon. Failed at point: " + currentPoint.getX() + "," + currentPoint.getY() + "\r\n"
                + "Note that this is a common error and can usually be fixed by 'smoothing out' the territory border "
                + "and removing any anti-aliasing.");
        return null;
      }
      for (int i = 2; i >= -3; i--) {
        final int tempDirection = Math.floorMod(currentDirection + i, 8);
        if (isOnEdge(tempDirection, currentPoint)) {
          // if we need to change our course
          if (i != 0) {
            points.add(currentPoint);
            currentPoint = new Point(currentPoint);
            move(currentPoint, tempDirection);
            currentDirection = tempDirection;
          } else {
            move(currentPoint, currentDirection);
          }
          break;
        }
      }
    }
    final int[] xpoints = new int[points.size()];
    final int[] ypoints = new int[points.size()];
    int i = 0;
    for (final Point item : points) {
      xpoints[i] = item.x;
      ypoints[i] = item.y;
      i++;
    }
    ToolLogger.info("Done finding polygon. total points;" + xpoints.length);
    return new Polygon(xpoints, ypoints, xpoints.length);
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    // arg can only be the map folder location.
    if (args.length == 1) {
      final String value;
      if (args[0].startsWith(TRIPLEA_MAP_FOLDER)) {
        value = getValue(args[0]);
      } else {
        value = args[0];
      }
      final File mapFolder = new File(value);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        ToolLogger.info("Could not find directory: " + value);
      }
    } else if (args.length > 1) {
      ToolLogger.info("Only argument allowed is the map directory.");
    }
    // might be set by -D
    if ((mapFolderLocation == null) || (mapFolderLocation.length() < 1)) {
      final String value = System.getProperty(TRIPLEA_MAP_FOLDER);
      if ((value != null) && (value.length() > 0)) {
        final File mapFolder = new File(value);
        if (mapFolder.exists()) {
          mapFolderLocation = mapFolder;
        } else {
          ToolLogger.info("Could not find directory: " + value);
        }
      }
    }
  }
}
