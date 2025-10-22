package tools.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
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
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.FileChooser;
import org.triplea.swing.SwingAction;
import org.triplea.util.PointFileReaderWriter;
import tools.util.ToolArguments;
import tools.util.ToolRunnableTask;

/**
 * Utility to break a map into polygons. Inputs - a map with 1 pixel wide borders - a list of
 * centers - this is used to guess the territory name and to verify the - territory name entered
 * Outputs - a list of polygons for each country
 */
@Slf4j
public final class PolygonGrabber extends ToolRunnableTask {
  private Path mapFolderLocation = null;

  private PolygonGrabber() {}

  public static void run() {
    runTask(PolygonGrabber.class);
  }

  @Override
  protected void runInternal() throws IOException {
    ToolArguments.ifMapFolder(mapFolderProperty -> mapFolderLocation = mapFolderProperty);
    log.info("Select the map");
    final FileOpen mapSelection = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png");
    final Path mapName = mapSelection.getFile();
    if (mapFolderLocation == null && mapSelection.getFile() != null) {
      mapFolderLocation = mapSelection.getFile().getParent();
    }
    if (mapName != null) {
      log.info("Map : {}", mapName);
      final PolygonGrabberFrame frame = new PolygonGrabberFrame(mapName);
      frame.setSize(800, 600);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
      JOptionPane.showMessageDialog(
          frame,
          new JLabel(
              "<html>"
                  + "This is the PolygonGrabber, it will create a polygons.txt file for you. "
                  + "<br>In order to run this, you must already have created a center.txt file. "
                  + "<br>Please click near the center of every single territory and sea zone on "
                  + "your map. "
                  + "<br>The grabber will then fill in the territory based on the borders it finds."
                  + "<br><br>If the territory shape or borders do not match what you intend, then"
                  + "<br>borders might have a gap or differently colored pixel in the border."
                  + "<br>You can also using the Clean Up Image... function from the Edit menu "
                  + "first to automatically fix some problems with the original image."
                  + "<br><br>These borders will define the shape of the territory in TripleA."
                  + "<br><br>When a territory is inside of another territory, you can turn on "
                  + "'island mode' to be able to see it."
                  + "<br><br>You can also load an existing polygons.txt file, then make "
                  + "modifications to it, then save it again."
                  + "<br><br>LEFT CLICK = fill in a territory's borders."
                  + "<br><br>Holding CTRL/SHIFT while LEFT CLICKING = add multiple territories "
                  + "together (eg: islands)."
                  + "<br><br>RIGHT CLICK = save or replace those borders for that territory."
                  + "<br><br>When finished, save the polygons and exit."
                  + "</html>"));
    } else {
      log.info("No Image Map Selected. Shutting down.");
    }
  }

  private final class PolygonGrabberFrame extends JFrame {
    private static final long serialVersionUID = 6381498094805120687L;

    private boolean islandMode;

    @Deprecated(since = "2.7", forRemoval = true)
    @SuppressWarnings({"unused"})
    private transient JCheckBoxMenuItem modeItem =
        null; // legacy field retained for backward compatibility

    // the current set of polygons
    private List<Polygon> current;
    // holds the map image
    private final BufferedImage bufferedImage;
    private final JPanel imagePanel;
    // maps String -> List of polygons
    private Map<String, List<Polygon>> polygons = new HashMap<>();
    // holds the centers for the polygons
    private Map<String, Point> centers;
    private final JLabel location = new JLabel();
    private final Point testPoint = new Point();

    /**
     * Asks user to specify a file with center points. If not program will exit. We set up the mouse
     * listeners and toolbars and load the actual image of the map here.
     *
     * @param mapFolder The {@link Path} pointing to the map folder.
     */
    PolygonGrabberFrame(final Path mapFolder) throws IOException {
      super("Polygon grabber");
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      final Path file = FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "centers.txt");
      if (Files.exists(file)
          && JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "A centers.txt file was found in the map's folder, do you want to use "
                      + "the file to supply the territories names?",
                  "File Suggestion",
                  JOptionPane.YES_NO_CANCEL_OPTION)
              == 0) {
        try {
          log.info("Centers : {}", file);
          centers = PointFileReaderWriter.readOneToOne(file);
        } catch (final IOException e) {
          log.error("Something wrong with Centers file", e);
        }
      } else {
        try {
          log.info("Select the Centers file");
          final Path centerPath =
              new FileOpen("Select A Center File", mapFolderLocation, ".txt").getFile();
          if (centerPath != null) {
            log.info("Centers : {}", centerPath);
            centers = PointFileReaderWriter.readOneToOne(centerPath);
          } else {
            log.info("You must specify a centers file.");
            log.info("Shutting down.");
            throw new IOException("no centers file specified");
          }
        } catch (final IOException e) {
          log.error("Something wrong with Centers file");
          throw e;
        }
      }
      bufferedImage = newBufferedImage(mapFolder);
      imagePanel = newMainPanel();
      /*
       * Add a mouse listener to show X : Y coordinates in the lower left corner of the screen.
       */
      imagePanel.addMouseMotionListener(
          new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
              location.setText("x: " + e.getX() + " y: " + e.getY());
            }
          });
      /*
       * Add a mouse listener to monitor for right mouse button being clicked.
       */
      imagePanel.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
              mouseEvent(
                  e.getPoint(),
                  e.isControlDown() || e.isShiftDown(),
                  SwingUtilities.isRightMouseButton(e));
            }
          });
      // set up the image panel size dimensions ...etc
      imagePanel.setMinimumSize(
          new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
      imagePanel.setPreferredSize(
          new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
      imagePanel.setMaximumSize(
          new Dimension(bufferedImage.getWidth(this), bufferedImage.getHeight(this)));
      // set up the layout manager
      this.getContentPane().setLayout(new BorderLayout());
      this.getContentPane().add(new JScrollPane(imagePanel), BorderLayout.CENTER);
      this.getContentPane().add(location, BorderLayout.SOUTH);
      // set up the actions
      final Action openAction = SwingAction.of("Load Polygons", e -> loadPolygons());
      openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Polygon Points FIle");
      final Action saveAction = SwingAction.of("Save Polygons", e -> savePolygons());
      saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Polygon Points To File");
      final Action exitAction =
          SwingAction.of(
              "Exit",
              e -> {
                setVisible(false);
                dispose();
              });
      exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
      final Action autoAction =
          SwingAction.of(
              "Auto Find Polygons",
              e -> {
                JOptionPane.showMessageDialog(
                    null,
                    new JLabel(
                        "<html>"
                            + "You will need to check and go back and do some polygons manually, "
                            + "as Auto does not catch them all. "
                            + "<br>Also, if a territory has more than 1 part (like an island "
                            + "chain), you will need to go back and "
                            + "<br>redo the entire territory chain using CTRL + Click in order "
                            + "to capture each part of the territory."
                            + "</html>"));
                current = new ArrayList<>();
                final BufferedImage imageCopy =
                    new BufferedImage(
                        bufferedImage.getWidth(null),
                        bufferedImage.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB);
                final Graphics g = imageCopy.getGraphics();
                g.drawImage(bufferedImage, 0, 0, null);
                for (Entry<String, Point> center : centers.entrySet()) {
                  final String territoryName = center.getKey();
                  final Point centerPoint = center.getValue();
                  log.info("Detecting Polygon for: {}", territoryName);
                  final @Nullable Polygon p = findPolygon(centerPoint.x, centerPoint.y);
                  // test if the poly contains the center point (this often fails when there is an
                  // island right above (because findPolygon will grab the island instead)
                  if (p == null || !p.contains(centerPoint)) {
                    continue;
                  }
                  // test if this poly contains any other centers, and if so do not do this one. let
                  // the user manually do it to make sure it gets done properly
                  boolean hasIslands = false;
                  for (final Point otherCenterPoint : centers.values()) {
                    if (centerPoint.equals(otherCenterPoint)) {
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
                  // some islands do not have centers on them because they are island chains that
                  // are also part of an island or
                  // territory touching a sidewall or outside of this polygon. we should still skip
                  // them.
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
      islandMode = false;
      setupMenuBar(openAction, saveAction, exitAction, autoAction);
    }

    private void setupMenuBar(
        Action openAction, Action saveAction, Action exitAction, Action autoAction) {
      // set up the menu items
      final JMenuItem openItem = new JMenuItem(openAction);
      openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
      final JMenuItem saveItem = new JMenuItem(saveAction);
      saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
      final JMenuItem exitItem = new JMenuItem(exitAction);
      final JCheckBoxMenuItem islandModeItem = new JCheckBoxMenuItem("Island Mode", false);
      islandModeItem.addActionListener(
          event -> {
            islandMode = islandModeItem.getState();
            repaint();
          });
      // set up the menu bar
      final JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);
      final JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic('F');
      fileMenu.add(openItem);
      fileMenu.add(saveItem);
      final JMenuItem saveImageItem = new JMenuItem("Save Image...");
      saveImageItem.addActionListener(e -> saveImage());
      fileMenu.add(saveImageItem);
      fileMenu.addSeparator();
      fileMenu.add(exitItem);
      final JMenu editMenu = new JMenu("Edit");
      final JMenuItem autoItem = new JMenuItem(autoAction);
      editMenu.setMnemonic('E');
      editMenu.add(islandModeItem);
      editMenu.add(autoItem);
      final JMenuItem cleanImageItem = new JMenuItem("Clean Up Image...");
      cleanImageItem.addActionListener(e -> cleanImage());
      editMenu.add(cleanImageItem);
      menuBar.add(fileMenu);
      menuBar.add(editMenu);
    }

    private void saveImage() {
      final Path target =
          FileChooser.builder().fileExtension("png").build().chooseFile().orElse(null);
      if (target == null) {
        return;
      }
      try {
        ImageIO.write(bufferedImage, "PNG", target.toFile());
        JOptionPane.showMessageDialog(null, "Saved to: " + target);
      } catch (IOException e) {
        log.error("Writing the image to {} failed", target, e);
      }
    }

    private void cleanImage() {
      final String input =
          JOptionPane.showInputDialog(
              getParent(),
              new JLabel(
                  "<html>"
                      + "The image cleaning tool will update the source image to do the following "
                      + "transformations:<br>"
                      + "1. Normalizes colors by changing pixels that aren't white to black.<br>"
                      + "2. \"Fills in\" small regions with black pixels.<br>"
                      + "3. Removes \"unnecessary\" black pixels by turning them to white, making "
                      + "the resulting lines between regions have a thickness of 1 pixel.<br><br>"
                      + "Note: You should double check that the result is as intended. If there "
                      + "are any gaps in territory borders, the clean up may completely remove "
                      + "them.<br><br>"
                      + "After clean up, you can save the updated image from the File menu.<br><br>"
                      + "Please select the minimum region size for eliminating regions:"),
              30);
      if (input == null) {
        return;
      }
      final int minimumRegionSize;
      try {
        minimumRegionSize = Integer.parseInt(input);
      } catch (final NumberFormatException e) {
        JOptionPane.showMessageDialog(getParent(), "Minimum region size must be a number");
        return;
      }
      new MapImageCleaner(bufferedImage, minimumRegionSize).cleanUpImage();
      imagePanel.repaint();
    }

    /**
     * We create the image of the map here and assure that it is loaded properly.
     *
     * @param mapFolder The {@link Path} pointing to the map folder.
     */
    private BufferedImage newBufferedImage(final Path mapFolder) {
      final Image newImage = FileHelper.newImage(mapFolder);
      final BufferedImage newBufferedImage =
          new BufferedImage(
              newImage.getWidth(null), newImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      final Graphics g = newBufferedImage.getGraphics();
      g.drawImage(newImage, 0, 0, this);
      g.dispose();
      return newBufferedImage;
    }

    /**
     * Creates a JPanel to be used. Dictates how the map is painted. Current problem is that islands
     * inside sea zones are not recognized when filling in the sea zone with a color, so we just
     * outline in red instead of filling. We fill for selecting territories only for ease of use. We
     * use var "islandMode" to dictate how to paint the map.
     *
     * @return The newly created panel.
     */
    private JPanel newMainPanel() {
      return new JPanel() {
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
    }

    /** Saves the polygons to disk. */
    private void savePolygons() {
      final Path polyName =
          new FileSave("Where To Save Polygons.txt ?", "polygons.txt", mapFolderLocation).getFile();
      if (polyName == null) {
        return;
      }
      try {
        PointFileReaderWriter.writeOneToManyPolygons(polyName, polygons);
        log.info("Data written to: {}", polyName.normalize().toAbsolutePath());
      } catch (final IOException e) {
        log.error("Failed to save polygons: {}", polyName, e);
      }
    }

    /** Loads a pre-defined file with map polygon points. */
    private void loadPolygons() {
      log.info("Load a polygon file");
      final @Nullable Path polyName =
          new FileOpen("Load A Polygon File", mapFolderLocation, ".txt").getFile();
      if (polyName == null) {
        return;
      }
      try {
        polygons = PointFileReaderWriter.readOneToManyPolygons(polyName);
      } catch (final IOException e) {
        log.error("Failed to load polygons: {}", polyName, e);
      }
      repaint();
    }

    private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse) {
      final @Nullable Polygon p = findPolygon(point.x, point.y);
      if (p == null) {
        return;
      }
      if (rightMouse && current != null) { // right click and list of polys is not empty
        doneCurrentGroup();
      } else if (pointInCurrentPolygon(point)) { // point clicked is already highlighted
        log.info("rejecting");
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

    /** returns false if there is no points in a current polygon. returns true if there is. */
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

    /** Does something with respect to check if the name of a territory is valid or not. */
    private void doneCurrentGroup() {
      final JTextField text = new JTextField();
      guessCountryName(text, centers.entrySet());
      final int option = JOptionPane.showConfirmDialog(this, text);
      // cancel = 2
      // no = 1
      // yes = 0
      if (option == 0) {
        if (!centers.containsKey(text.getText())) {
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
        log.info("something very invalid");
      }
    }

    /** Guess the country name based on the location of the previous centers. */
    private void guessCountryName(
        final JTextField text, final Iterable<Entry<String, Point>> centersIterator) {
      final List<String> options = new ArrayList<>();
      for (final Entry<String, Point> item : centersIterator) {
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

    /** Checks to see if the given point is of color black. */
    private boolean isBlack(final Point p) {
      return isBlack(p.x, p.y, bufferedImage);
    }

    private boolean isBlack(final int x, final int y, final BufferedImage bufferedImage) {
      if (!inBounds(x, y, bufferedImage)) {
        // not inbounds, can't be black
        return false;
      }
      // gets ARGB integer value and we LOGICAL AND mask it
      // with ARGB value of 00,FF,FF,FF to determine if it black or not.
      // maybe here ?
      return (bufferedImage.getRGB(x, y) & 0x00FFFFFF) == 0;
    }

    private boolean inBounds(final int x, final int y, final BufferedImage image) {
      return x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight();
    }

    /**
     * Moves to a specified direction. Directions 0 - North 1 - North east 2 - East 3 - South east 4
     * - South 5 - South west 6 - West 7 - North west
     */
    private void move(final Point p, final int direction) {
      if (direction < 0 || direction > 7) {
        throw new IllegalArgumentException("Not a direction : " + direction);
      }
      if (direction == 1 || direction == 2 || direction == 3) {
        p.x++;
      } else if (direction == 5 || direction == 6 || direction == 7) {
        p.x--;
      }
      if (direction == 5 || direction == 4 || direction == 3) {
        p.y++;
      } else if (direction == 7 || direction == 0 || direction == 1) {
        p.y--;
      }
    }

    /** Checks to see if the direction we're going is on the edge. */
    private boolean isOnEdge(final int direction, final Point currentPoint) {
      testPoint.setLocation(currentPoint);
      move(testPoint, direction);
      return testPoint.x == 0
          || testPoint.y == 0
          || testPoint.y == bufferedImage.getHeight(this)
          || testPoint.x == bufferedImage.getWidth(this)
          || isBlack(testPoint);
    }

    private boolean doesPolygonContainAnyBlackInside(
        final Polygon poly, final BufferedImage imageCopy, final Graphics imageCopyGraphics) {
      // we would like to just test if each point is both black and contained within the polygon,
      // but contains counts
      // the borders, so we have to turn the border edges a different color (then later back to
      // black again) using a
      // copy of the image
      imageCopyGraphics.setColor(Color.GREEN);
      imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
      final Rectangle rect = poly.getBounds();
      for (int x = rect.x; x < rect.x + rect.width; x++) {
        for (int y = rect.y; y < rect.y + rect.height; y++) {
          if (isBlack(x, y, imageCopy) && poly.contains(new Point(x, y))) {
            imageCopyGraphics.setColor(Color.BLACK);
            imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
            return true;
          }
        }
      }
      imageCopyGraphics.setColor(Color.BLACK);
      imageCopyGraphics.drawPolygon(poly.xpoints, poly.ypoints, poly.npoints);
      return false;
    }

    /** Algorithm to find a polygon given an x/y coordinates and returns the found polygon. */
    private @Nullable Polygon findPolygon(final int x, final int y) {
      // walk up, find the first black point
      final Point startPoint = new Point(x, y);
      while (inBounds(startPoint.x, startPoint.y - 1, bufferedImage)
          && !isBlack(startPoint.x, startPoint.y, bufferedImage)) {
        startPoint.y--;
      }
      final List<Point> points = new ArrayList<>(100);
      points.add(new Point(startPoint));
      int currentDirection = 2;
      Point currentPoint = new Point(startPoint);
      int iterCount = 0;
      while (!currentPoint.equals(startPoint) || points.size() == 1) {
        iterCount++;
        if (iterCount > 100000) {
          JOptionPane.showMessageDialog(
              this,
              "Failed to grab the polygon. Failed at point: "
                  + currentPoint.getX()
                  + ","
                  + currentPoint.getY()
                  + "\r\n"
                  + "Note that this is a common error and can usually be fixed by 'smoothing out' "
                  + "the territory border and removing any anti-aliasing.");
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
      final int[] xPoints = new int[points.size()];
      final int[] yPoints = new int[points.size()];
      int i = 0;
      for (final Point item : points) {
        xPoints[i] = item.x;
        yPoints[i] = item.y;
        i++;
      }
      log.info("Done finding polygon. total points;{}", xPoints.length);
      return new Polygon(xPoints, yPoints, xPoints.length);
    }
  }
}
