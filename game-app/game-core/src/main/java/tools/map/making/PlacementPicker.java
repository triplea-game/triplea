package tools.map.making;

import static com.google.common.base.Preconditions.checkState;

import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.SwingAction;
import org.triplea.util.PointFileReaderWriter;
import org.triplea.util.Tuple;
import tools.image.FileHelper;
import tools.image.FileOpen;
import tools.image.FileSave;
import tools.image.MapFolderLocationSystemProperty;
import tools.util.ToolArguments;
import tools.util.ToolsUtil;

/**
 * The placement picker map making tool.
 *
 * <p>This tool will allow you to manually specify unit placement locations for each territory on a
 * given map. It will generate a {@code places.txt} file containing the unit placement locations.
 */
@Slf4j
public final class PlacementPicker {
  private int placeWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int placeHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private boolean placeDimensionsSet = false;
  private double unitZoomPercent = 1;
  private int unitWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private int unitHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private Path mapFolderLocation = null;

  private PlacementPicker() {}

  /**
   * Runs the placement picker tool.
   *
   * @throws IllegalStateException If not invoked on the EDT.
   */
  public static void run() {
    checkState(SwingUtilities.isEventDispatchThread());

    try {
      new PlacementPicker().runInternal();
    } catch (final IOException e) {
      log.error("failed to run placement picker", e);
    }
  }

  private void runInternal() throws IOException {
    handleSystemProperties();
    JOptionPane.showMessageDialog(
        null,
        new JLabel(
            "<html>"
                + "This is the PlacementPicker, it will create a place.txt file for you. "
                + "<br>In order to run this, you must already have created a centers.txt file "
                + "and a polygons.txt file. "
                + "<br><br>The program will ask for unit scale (unit zoom) level [normally "
                + "between 0.5 and 1.0], "
                + "<br>Then it will ask for the unit image size when not zoomed [normally 48x48]. "
                + "<br><br>If you want to have less, or more, room around the edges of your "
                + "units, you can change the unit size. "
                + "<br><br>After it starts, you may Load an existing place.txt file, that way "
                + "you can make changes to it then save it. "
                + "<br><br>LEFT CLICK = Select a new territory. "
                + "<br><br>Holding CTRL/SHIFT + LEFT CLICK = Create a new placement for that "
                + "territory. "
                + "<br><br>RIGHT CLICK = Remove last placement for that territory. "
                + "<br><br>Holding CTRL/SHIFT + RIGHT CLICK = Save all placements for that "
                + "territory. "
                + "<br><br>Pressing the 'O' key = Toggle the direction for placement overflow for "
                + "that territory. "
                + "<br><br>It is a very good idea to check each territory using the "
                + "PlacementPicker after running the AutoPlacementFinder "
                + "<br>to make sure there are enough placements for each territory. If not, you "
                + "can always add more then save it. "
                + "<br><br>IF there are not enough placements, by default the units will Overflow "
                + "to the RIGHT of the very LAST placement made, "
                + "<br>so be sure that the last placement is on the right side of the territory "
                + "<br>or that it doesn't overflow directly on top of other placements. Can "
                + "instead toggle the overflow direction."
                + "<br><br>To show all placements, or see the overflow direction, or see which "
                + "territories you have not yet completed enough, "
                + "<br>placements for, turn on the mode options in the 'edit' menu. "
                + "</html>"));
    log.info("Select the map");
    final FileOpen mapSelection = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png");
    final Path mapName = mapSelection.getFile();
    if (mapFolderLocation == null && mapSelection.getFile() != null) {
      mapFolderLocation = mapSelection.getFile().getParent();
    }
    if (mapName != null) {
      final PlacementPickerFrame frame = new PlacementPickerFrame(mapName);
      frame.setSize(800, 600);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    } else {
      log.info("No Image Map Selected. Shutting down.");
    }
  }

  private final class PlacementPickerFrame extends JFrame {
    private static final long serialVersionUID = 953019978051420881L;

    private final JCheckBoxMenuItem showAllModeItem;
    private final JCheckBoxMenuItem showOverflowModeItem;
    private final JCheckBoxMenuItem showIncompleteModeItem;
    private boolean showAllMode = false;
    private boolean showOverflowMode = false;
    private boolean showIncompleteMode = false;
    private int incompleteNum = 1;
    private Point currentSquare;
    private final Image image;
    private final JLabel locationLabel = new JLabel();
    private Map<String, List<Polygon>> polygons = new HashMap<>();
    private Map<String, Tuple<List<Point>, Boolean>> placements = new HashMap<>();
    private List<Point> currentPlacements;
    private boolean currentOverflowToLeft = false;
    private String currentCountry;

    /**
     * Sets up all GUI components, initializes variables with default or needed values, and prepares
     * the map for user commands.
     *
     * @param mapFolder The {@link Path} pointing to the map folder.
     */
    PlacementPickerFrame(final Path mapFolder) throws IOException {
      super("Placement Picker");
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      if (!placeDimensionsSet) {
        try {
          final Path file =
              FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "map.properties");
          if (Files.exists(file)) {
            double scale = unitZoomPercent;
            int width = unitWidth;
            int height = unitHeight;
            boolean found = false;
            final String scaleProperty = MapData.PROPERTY_UNITS_SCALE + "=";
            final String widthProperty = MapData.PROPERTY_UNITS_WIDTH + "=";
            final String heightProperty = MapData.PROPERTY_UNITS_HEIGHT + "=";
            try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name())) {
              while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.contains(scaleProperty)) {
                  try {
                    scale =
                        Double.parseDouble(
                            line.substring(line.indexOf(scaleProperty) + scaleProperty.length())
                                .trim());
                    found = true;
                  } catch (final NumberFormatException ex) {
                    // ignore malformed input
                  }
                }
                if (line.contains(widthProperty)) {
                  try {
                    width =
                        Integer.parseInt(
                            line.substring(line.indexOf(widthProperty) + widthProperty.length())
                                .trim());
                    found = true;
                  } catch (final NumberFormatException ex) {
                    // ignore malformed input
                  }
                }
                if (line.contains(heightProperty)) {
                  try {
                    height =
                        Integer.parseInt(
                            line.substring(line.indexOf(heightProperty) + heightProperty.length())
                                .trim());
                    found = true;
                  } catch (final NumberFormatException ex) {
                    // ignore malformed input
                  }
                }
              }
            }
            if (found) {
              final int result =
                  JOptionPane.showConfirmDialog(
                      new JPanel(),
                      "A map.properties file was found in the map's folder, "
                          + "\r\n do you want to use the file to supply the info for "
                          + "the placement box size? "
                          + "\r\n Zoom = "
                          + scale
                          + ",  Width = "
                          + width
                          + ",  Height = "
                          + height
                          + ",    Result = ("
                          + ((int) (scale * width))
                          + "x"
                          + ((int) (scale * height))
                          + ")",
                      "File Suggestion",
                      JOptionPane.YES_NO_CANCEL_OPTION);

              if (result == 0) {
                unitZoomPercent = scale;
                placeWidth = (int) (unitZoomPercent * width);
                placeHeight = (int) (unitZoomPercent * height);
                placeDimensionsSet = true;
              }
            }
          }
        } catch (final Exception e) {
          log.error("Failed to initialize from map properties", e);
        }
      }
      if (!placeDimensionsSet
          || JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "Placement Box Size already set ("
                      + placeWidth
                      + "x"
                      + placeHeight
                      + "), "
                      + "do you wish to continue with this?\r\n"
                      + "Select Yes to continue, Select No to override and change the size.",
                  "Placement Box Size",
                  JOptionPane.YES_NO_OPTION)
              == 1) {
        try {
          final String result = getUnitsScale();
          try {
            unitZoomPercent = Double.parseDouble(result.toLowerCase());
          } catch (final NumberFormatException ex) {
            // ignore malformed input
          }
          final String width =
              JOptionPane.showInputDialog(
                  null,
                  "Enter the unit's image width in pixels (unscaled / without zoom).\r\n"
                      + "(e.g. 48)");
          if (width != null) {
            try {
              placeWidth = (int) (unitZoomPercent * Integer.parseInt(width));
            } catch (final NumberFormatException ex) {
              // ignore malformed input
            }
          }
          final String height =
              JOptionPane.showInputDialog(
                  null,
                  "Enter the unit's image height in pixels (unscaled / without zoom).\r\n"
                      + "(e.g. 48)");
          if (height != null) {
            try {
              placeHeight = (int) (unitZoomPercent * Integer.parseInt(height));
            } catch (final NumberFormatException ex) {
              // ignore malformed input
            }
          }
          placeDimensionsSet = true;
        } catch (final Exception e) {
          log.error("Failed to initialize from user input", e);
        }
      }
      final Path file = FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "polygons.txt");
      if (Files.exists(file)
          && JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "A polygons.txt file was found in the map's folder, do you want to "
                      + "use the file to supply the territories?",
                  "File Suggestion",
                  JOptionPane.YES_NO_CANCEL_OPTION)
              == 0) {
        try {
          log.info("Polygons : " + file);
          polygons = PointFileReaderWriter.readOneToManyPolygons(file);
        } catch (final IOException e) {
          log.error("Failed to load polygons: " + file.toAbsolutePath());
          throw e;
        }
      } else {
        log.info("Select the Polygons file");
        final Path polyPath =
            new FileOpen("Select A Polygon File", mapFolderLocation, ".txt").getFile();
        if (polyPath != null) {
          log.info("Polygons : " + polyPath);
          try {
            polygons = PointFileReaderWriter.readOneToManyPolygons(polyPath);
          } catch (final IOException e) {
            log.error("Failed to load polygons: " + polyPath);
            throw e;
          }
        } else {
          log.info("Polygons file not given. Will run regardless");
        }
      }
      image = FileHelper.newImage(mapFolder);
      final JPanel imagePanel = newMainPanel();
      /*
       * Add a mouse listener to show X : Y coordinates on the lower left corner of the screen.
       */
      imagePanel.addMouseMotionListener(
          new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
              locationLabel.setText("x:" + e.getX() + " y:" + e.getY());
              currentSquare = new Point(e.getPoint());
              repaint();
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

      this.addKeyListener(
          new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
              if (showOverflowMode && currentCountry != null && (e.getKeyCode() == KeyEvent.VK_O)) {
                currentOverflowToLeft = !currentOverflowToLeft;
                repaint();
              }
            }
          });

      // set up the image panel size dimensions ...etc
      imagePanel.setMinimumSize(new Dimension(image.getWidth(this), image.getHeight(this)));
      imagePanel.setPreferredSize(new Dimension(image.getWidth(this), image.getHeight(this)));
      imagePanel.setMaximumSize(new Dimension(image.getWidth(this), image.getHeight(this)));
      // set up the layout manager
      this.getContentPane().setLayout(new BorderLayout());
      this.getContentPane().add(new JScrollPane(imagePanel), BorderLayout.CENTER);
      this.getContentPane().add(locationLabel, BorderLayout.SOUTH);
      // set up the actions
      final Action openAction = SwingAction.of("Load Placements", e -> loadPlacements());
      openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Placement File");
      final Action saveAction = SwingAction.of("Save Placements", e -> savePlacements());
      saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Placements To File");
      final Action exitAction =
          SwingAction.of(
              "Exit",
              e -> {
                setVisible(false);
                dispose();
              });
      exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
      // set up the menu items
      final JMenuItem openItem = new JMenuItem(openAction);
      openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
      final JMenuItem saveItem = new JMenuItem(saveAction);
      saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
      final JMenuItem exitItem = new JMenuItem(exitAction);
      // set up the menu bar
      final JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);
      final JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic('F');
      fileMenu.add(openItem);
      fileMenu.add(saveItem);
      fileMenu.addSeparator();
      fileMenu.add(exitItem);
      showAllModeItem = new JCheckBoxMenuItem("Show All Placements Mode", false);
      showAllModeItem.addActionListener(
          event -> {
            showAllMode = showAllModeItem.getState();
            repaint();
          });
      showOverflowModeItem = new JCheckBoxMenuItem("Show Overflow Mode", false);
      showOverflowModeItem.addActionListener(
          event -> {
            showOverflowMode = showOverflowModeItem.getState();
            repaint();
          });
      showIncompleteModeItem = new JCheckBoxMenuItem("Show Incomplete Placements Mode", false);
      showIncompleteModeItem.addActionListener(
          event -> {
            if (showIncompleteModeItem.getState()) {
              final String num =
                  JOptionPane.showInputDialog(
                      null,
                      "Enter the minimum number of placements each territory must have.\r\n"
                          + "(examples: 1, 4, etc.)");
              try {
                incompleteNum = Math.max(1, Math.min(50, Integer.parseInt(num)));
              } catch (final Exception ex) {
                incompleteNum = 1;
              }
            }
            showIncompleteMode = showIncompleteModeItem.getState();
            repaint();
          });
      final JMenu editMenu = new JMenu("Edit");
      editMenu.setMnemonic('E');
      editMenu.add(showAllModeItem);
      editMenu.add(showOverflowModeItem);
      editMenu.add(showIncompleteModeItem);
      menuBar.add(fileMenu);
      menuBar.add(editMenu);
    }

    /** Creates the main panel and returns a JPanel object. */
    private JPanel newMainPanel() {
      return new JPanel() {
        private static final long serialVersionUID = -3941975573431195136L;

        @Override
        public void paint(final Graphics g) {
          // super.paint(g);
          g.drawImage(image, 0, 0, this);
          if (showAllMode) {
            g.setColor(Color.yellow);
            for (final Entry<String, Tuple<List<Point>, Boolean>> entry : placements.entrySet()) {
              if (entry.getKey().equals(currentCountry)
                  && currentPlacements != null
                  && !currentPlacements.isEmpty()) {
                continue;
              }
              final Iterator<Point> pointIter = entry.getValue().getFirst().iterator();
              while (pointIter.hasNext()) {
                final Point item = pointIter.next();
                g.fillRect(item.x, item.y, placeWidth, placeHeight);
                if (showOverflowMode && !pointIter.hasNext()) {
                  g.setColor(Color.gray);
                  if (entry.getValue().getSecond()) {
                    g.fillRect(item.x - placeWidth, item.y + placeHeight / 2, placeWidth, 4);
                  } else {
                    g.fillRect(item.x + placeWidth, item.y + placeHeight / 2, placeWidth, 4);
                  }
                  g.setColor(Color.yellow);
                }
              }
            }
          }
          if (showIncompleteMode) {
            g.setColor(Color.green);
            final Set<String> territories = new HashSet<>(polygons.keySet());
            final Iterator<String> terrIter = territories.iterator();
            while (terrIter.hasNext()) {
              final String terr = terrIter.next();
              final List<Point> points = placements.get(terr).getFirst();
              if (points != null && points.size() >= incompleteNum) {
                terrIter.remove();
              }
            }
            for (final String terr : territories) {
              final List<Polygon> polys = polygons.get(terr);
              if (polys == null || polys.isEmpty()) {
                continue;
              }
              for (final Polygon poly : polys) {
                g.fillPolygon(poly);
              }
            }
          }
          g.setColor(Color.red);
          if (currentSquare != null) {
            g.drawRect(currentSquare.x, currentSquare.y, placeWidth, placeHeight);
          }
          if (currentPlacements == null) {
            return;
          }
          final Iterator<Point> pointIter = currentPlacements.iterator();
          while (pointIter.hasNext()) {
            final Point item = pointIter.next();
            g.fillRect(item.x, item.y, placeWidth, placeHeight);
            if (showOverflowMode && !pointIter.hasNext()) {
              g.setColor(Color.gray);
              if (currentOverflowToLeft) {
                g.fillRect(item.x - placeWidth, item.y + placeHeight / 2, placeWidth, 4);
              } else {
                g.fillRect(item.x + placeWidth, item.y + placeHeight / 2, placeWidth, 4);
              }
              g.setColor(Color.red);
            }
          }
        } // paint
      };
    }

    /** Saves the placements to disk. */
    private void savePlacements() {
      final Path fileName =
          new FileSave("Where To Save place.txt ?", "place.txt", mapFolderLocation).getFile();
      if (fileName == null) {
        return;
      }
      try {
        PointFileReaderWriter.writeOneToManyPlacements(fileName, placements);
        log.info("Data written to :" + fileName.normalize().toAbsolutePath());
      } catch (final IOException e) {
        log.error("Failed to write placements: " + fileName, e);
      }
    }

    /** Loads a pre-defined file with map placement points. */
    private void loadPlacements() {
      log.info("Load a placement file");
      final Path placeName =
          new FileOpen("Load A Placement File", mapFolderLocation, ".txt").getFile();
      if (placeName == null) {
        return;
      }
      try {
        placements = PointFileReaderWriter.readOneToManyPlacements(placeName);
      } catch (final IOException e) {
        log.error("Failed to load placements: " + placeName, e);
      }
      repaint();
    }

    /**
     * Updates tool state based on the specified mouse event.
     *
     * <ul>
     *   <li>Left button: Start in territory.
     *   <li>Left button + control: Add point.
     *   <li>Right button and ctrl: Write.
     *   <li>Right button: Remove last.
     * </ul>
     */
    private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse) {
      if (!rightMouse && !ctrlDown) {
        currentCountry = ToolsUtil.findTerritoryName(point, polygons).orElse("there be dragons");
        // If there isn't an existing array, create one
        if (placements.get(currentCountry) == null) {
          currentPlacements = new ArrayList<>();
          currentOverflowToLeft = false;
        } else {
          currentPlacements = new ArrayList<>(placements.get(currentCountry).getFirst());
          currentOverflowToLeft = placements.get(currentCountry).getSecond();
        }
        JOptionPane.showMessageDialog(this, currentCountry);
      } else if (!rightMouse) {
        if (currentPlacements != null) {
          currentPlacements.add(point);
        }
      } else if (ctrlDown) {
        if (currentPlacements != null) {
          placements.put(currentCountry, Tuple.of(currentPlacements, currentOverflowToLeft));
          currentPlacements = new ArrayList<>();
          log.info("done:" + currentCountry);
        }
      } else {
        if (currentPlacements != null && !currentPlacements.isEmpty()) {
          currentPlacements.remove(currentPlacements.size() - 1);
        }
      }
      repaint();
    }

    private String getUnitsScale() {
      final String unitsScale =
          JOptionPane.showInputDialog(
              null,
              "Enter the unit's scale (zoom).\r\n"
                  + "(e.g. 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5)");
      return (unitsScale != null) ? unitsScale : "1";
    }
  }

  private void handleSystemProperties() {
    mapFolderLocation = MapFolderLocationSystemProperty.read();
    final String zoomString = System.getProperty(ToolArguments.UNIT_ZOOM);
    if (zoomString != null && zoomString.length() > 0) {
      try {
        unitZoomPercent = Double.parseDouble(zoomString);
      } catch (final Exception e) {
        log.error("Not a decimal percentage: " + zoomString);
      }
    }
    final String widthString = System.getProperty(ToolArguments.UNIT_WIDTH);
    if (widthString != null && widthString.length() > 0) {
      try {
        unitWidth = Integer.parseInt(widthString);
        placeWidth = (int) (unitZoomPercent * unitWidth);
      } catch (final Exception e) {
        log.error("Not an integer: " + widthString);
      }
    }
    final String heightString = System.getProperty(ToolArguments.UNIT_HEIGHT);
    if (heightString != null && heightString.length() > 0) {
      try {
        unitHeight = Integer.parseInt(heightString);
        placeHeight = (int) (unitZoomPercent * unitHeight);
      } catch (final Exception e) {
        log.error("Not an integer: " + heightString);
      }
    }
  }
}
