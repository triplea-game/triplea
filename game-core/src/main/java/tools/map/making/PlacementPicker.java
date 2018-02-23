package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import tools.image.FileOpen;
import tools.image.FileSave;
import tools.util.ToolLogger;

public class PlacementPicker extends JFrame {
  private static final long serialVersionUID = 953019978051420881L;
  private final JCheckBoxMenuItem showAllModeItem;
  private final JCheckBoxMenuItem showOverflowModeItem;
  private final JCheckBoxMenuItem showIncompleteModeItem;
  private static boolean showAllMode = false;
  private static boolean showOverflowMode = false;
  private static boolean showIncompleteMode = false;
  private static int incompleteNum = 1;
  private Point currentSquare;
  private Image image;
  private final JLabel locationLabel = new JLabel();
  private Map<String, List<Polygon>> polygons = new HashMap<>();
  private Map<String, List<Point>> placements;
  private List<Point> currentPlacements;
  private String currentCountry;
  private static int placeWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static int placeHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static boolean placeDimensionsSet = false;
  private static double unitZoomPercent = 1;
  private static int unitWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static int unitHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
  private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
  private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";

  public static String[] getProperties() {
    return new String[] {TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT};
  }

  /**
   * main(java.lang.String[])
   * Main program begins here.
   * Asks the user to select the map then runs the
   * the actual placement picker program.
   *
   * @param args the command line arguments
   */
  public static void main(final String[] args) {
    handleCommandLineArgs(args);
    JOptionPane.showMessageDialog(null,
        new JLabel("<html>" + "This is the PlacementPicker, it will create a place.txt file for you. "
            + "<br>In order to run this, you must already have created a centers.txt file and a polygons.txt file. "
            + "<br><br>The program will ask for unit scale (unit zoom) level [normally between 0.5 and 1.0], "
            + "<br>Then it will ask for the unit image size when not zoomed [normally 48x48]. "
            + "<br><br>If you want to have less, or more, room around the edges of your units, you can change the unit "
            + "size. "
            + "<br><br>After it starts, you may Load an existing place.txt file, that way you can make changes to it "
            + "then save it. "
            + "<br><br>LEFT CLICK = Select a new territory. "
            + "<br><br>Holding CTRL/SHIFT + LEFT CLICK = Create a new placement for that territory. "
            + "<br><br>RIGHT CLICK = Remove last placement for that territory. "
            + "<br><br>Holding CTRL/SHIFT + RIGHT CLICK = Save all placements for that territory. "
            + "<br><br>It is a very good idea to check each territory using the PlacementPicker after running the "
            + "AutoPlacementFinder "
            + "<br>to make sure there are enough placements for each territory. If not, you can always add more then "
            + "save it. "
            + "<br><br>IF there are not enough placements, the units will Overflow to the RIGHT of the very LAST "
            + "placement made, "
            + "<br>so be sure that the last placement is on the right side of the territory "
            + "<br>or that it does not overflow directly on top of other placements. "
            + "<br><br>To show all placements, or see the overflow direction, or see which territories you have not "
            + "yet completed enough, "
            + "<br>placements for, turn on the mode options in the 'edit' menu. " + "</html>"));
    ToolLogger.info("Select the map");
    final FileOpen mapSelection = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png");
    final String mapName = mapSelection.getPathString();
    if ((mapFolderLocation == null) && (mapSelection.getFile() != null)) {
      mapFolderLocation = mapSelection.getFile().getParentFile();
    }
    if (mapName != null) {
      final PlacementPicker picker = new PlacementPicker(mapName);
      picker.setSize(800, 600);
      picker.setLocationRelativeTo(null);
      picker.setVisible(true);
    } else {
      ToolLogger.info("No Image Map Selected. Shutting down.");
      System.exit(0);
    }
  } // end main

  /**
   * Constructor PlacementPicker(java.lang.String)
   * Setus up all GUI components, initializes variables with
   * default or needed values, and prepares the map for user
   * commands.
   *
   * @param mapName Name of map file.
   */
  public PlacementPicker(final String mapName) {
    super("Placement Picker");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    if (!placeDimensionsSet) {
      try {
        File file = null;
        if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
          file = new File(mapFolderLocation, "map.properties");
        }
        if ((file == null) || !file.exists()) {
          file = new File(new File(mapName).getParent() + File.separator + "map.properties");
        }
        if (file.exists()) {
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
                      Double.parseDouble(line.substring(line.indexOf(scaleProperty) + scaleProperty.length()).trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
              if (line.contains(widthProperty)) {
                try {
                  width = Integer.parseInt(line.substring(line.indexOf(widthProperty) + widthProperty.length()).trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
              if (line.contains(heightProperty)) {
                try {
                  height =
                      Integer.parseInt(line.substring(line.indexOf(heightProperty) + heightProperty.length()).trim());
                  found = true;
                } catch (final NumberFormatException ex) {
                  // ignore malformed input
                }
              }
            }
          }
          if (found) {
            final int result = JOptionPane.showConfirmDialog(new JPanel(),
                "A map.properties file was found in the map's folder, "
                    + "\r\n do you want to use the file to supply the info for the placement box size? "
                    + "\r\n Zoom = " + scale + ",  Width = " + width + ",  Height = " + height + ",    Result = ("
                    + ((int) (scale * width)) + "x" + ((int) (scale * height)) + ")",
                "File Suggestion", 1);

            if (result == 0) {
              unitZoomPercent = scale;
              placeWidth = (int) (unitZoomPercent * width);
              placeHeight = (int) (unitZoomPercent * height);
              placeDimensionsSet = true;
            }
          }
        }
      } catch (final Exception e) {
        ToolLogger.error("Failed to initialize from map properties", e);
      }
    }
    if (!placeDimensionsSet || (JOptionPane.showConfirmDialog(new JPanel(),
        "Placement Box Size already set (" + placeWidth + "x" + placeHeight + "), "
            + "do you wish to continue with this?\r\n"
            + "Select Yes to continue, Select No to override and change the size.",
        "Placement Box Size", JOptionPane.YES_NO_OPTION) == 1)) {
      try {
        final String result = getUnitsScale();
        try {
          unitZoomPercent = Double.parseDouble(result.toLowerCase());
        } catch (final NumberFormatException ex) {
          // ignore malformed input
        }
        final String width = JOptionPane.showInputDialog(null,
            "Enter the unit's image width in pixels (unscaled / without zoom).\r\n(e.g. 48)");
        if (width != null) {
          try {
            placeWidth = (int) (unitZoomPercent * Integer.parseInt(width));
          } catch (final NumberFormatException ex) {
            // ignore malformed input
          }
        }
        final String height = JOptionPane.showInputDialog(null,
            "Enter the unit's image height in pixels (unscaled / without zoom).\r\n(e.g. 48)");
        if (height != null) {
          try {
            placeHeight = (int) (unitZoomPercent * Integer.parseInt(height));
          } catch (final NumberFormatException ex) {
            // ignore malformed input
          }
        }
        placeDimensionsSet = true;
      } catch (final Exception e) {
        ToolLogger.error("Failed to initialize from user input", e);
      }
    }
    File file = null;
    if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
      file = new File(mapFolderLocation, "polygons.txt");
    }
    if ((file == null) || !file.exists()) {
      file = new File(new File(mapName).getParent() + File.separator + "polygons.txt");
    }
    if (file.exists() && (JOptionPane.showConfirmDialog(new JPanel(),
        "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories?",
        "File Suggestion", 1) == 0)) {
      try (InputStream is = new FileInputStream(file.getPath())) {
        ToolLogger.info("Polygons : " + file.getPath());
        polygons = PointFileReaderWriter.readOneToManyPolygons(is);
      } catch (final IOException e) {
        ToolLogger.error("Failed to load polygons: " + file.getAbsolutePath(), e);
        System.exit(0);
      }
    } else {
      ToolLogger.info("Select the Polygons file");
      final String polyPath = new FileOpen("Select A Polygon File", mapFolderLocation, ".txt").getPathString();
      if (polyPath != null) {
        ToolLogger.info("Polygons : " + polyPath);
        try (InputStream is = new FileInputStream(polyPath)) {
          polygons = PointFileReaderWriter.readOneToManyPolygons(is);
        } catch (final IOException e) {
          ToolLogger.error("Failed to load polygons: " + polyPath, e);
          System.exit(0);
        }
      } else {
        ToolLogger.info("Polygons file not given. Will run regardless");
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
        locationLabel.setText("x:" + e.getX() + " y:" + e.getY());
        currentSquare = new Point(e.getPoint());
        repaint();
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
    final Action exitAction = SwingAction.of("Exit", e -> System.exit(0));
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
    showAllMode = false;
    showOverflowMode = false;
    showIncompleteMode = false;
    incompleteNum = 1;
    showAllModeItem = new JCheckBoxMenuItem("Show All Placements Mode", false);
    showAllModeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        showAllMode = showAllModeItem.getState();
        repaint();
      }
    });
    showOverflowModeItem = new JCheckBoxMenuItem("Show Overflow Mode", false);
    showOverflowModeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        showOverflowMode = showOverflowModeItem.getState();
        repaint();
      }
    });
    showIncompleteModeItem = new JCheckBoxMenuItem("Show Incomplete Placements Mode", false);
    showIncompleteModeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        if (showIncompleteModeItem.getState()) {
          final String num = JOptionPane.showInputDialog(null,
              "Enter the minimum number of placements each territory must have.\r\n(examples: 1, 4, etc.)");
          try {
            incompleteNum = Math.max(1, Math.min(50, Integer.parseInt(num)));
          } catch (final Exception ex) {
            incompleteNum = 1;
          }
        }
        showIncompleteMode = showIncompleteModeItem.getState();
        repaint();
      }
    });
    final JMenu editMenu = new JMenu("Edit");
    editMenu.setMnemonic('E');
    editMenu.add(showAllModeItem);
    editMenu.add(showOverflowModeItem);
    editMenu.add(showIncompleteModeItem);
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
  } // end constructor

  /**
   * createImage(java.lang.String)
   * creates the image map and makes sure
   * it is properly loaded.
   *
   * @param mapName
   *        .lang.String mapName the path of image map
   */
  private void createImage(final String mapName) {
    image = Toolkit.getDefaultToolkit().createImage(mapName);
    Util.ensureImageLoaded(image);
  }

  /**
   * javax.swing.JPanel createMainPanel()
   * Creates the main panel and returns
   * a JPanel object.
   *
   * @return javax.swing.JPanel the panel to return
   */
  private JPanel createMainPanel() {
    final JPanel imagePanel = new JPanel() {
      private static final long serialVersionUID = -3941975573431195136L;

      @Override
      public void paint(final Graphics g) {
        // super.paint(g);
        g.drawImage(image, 0, 0, this);
        if (showAllMode) {
          g.setColor(Color.yellow);
          for (final Entry<String, List<Point>> entry : placements.entrySet()) {
            if (entry.getKey().equals(currentCountry) && (currentPlacements != null)
                && !currentPlacements.isEmpty()) {
              continue;
            }
            final Iterator<Point> pointIter = entry.getValue().iterator();
            while (pointIter.hasNext()) {
              final Point item = pointIter.next();
              g.fillRect(item.x, item.y, placeWidth, placeHeight);
              if (showOverflowMode && !pointIter.hasNext()) {
                g.setColor(Color.gray);
                g.fillRect(item.x + placeWidth, item.y + (placeHeight / 2), placeWidth, 4);
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
            final List<Point> points = placements.get(terr);
            if ((points != null) && (points.size() >= incompleteNum)) {
              terrIter.remove();
            }
          }
          for (final String terr : territories) {
            final List<Polygon> polys = polygons.get(terr);
            if ((polys == null) || polys.isEmpty()) {
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
            g.fillRect(item.x + placeWidth, item.y + (placeHeight / 2), placeWidth, 4);
            g.setColor(Color.red);
          }
        }
      } // paint
    };
    return imagePanel;
  }

  /**
   * savePlacements()
   * Saves the placements to disk.
   */
  private void savePlacements() {
    final String fileName =
        new FileSave("Where To Save place.txt ?", "place.txt", mapFolderLocation).getPathString();
    if (fileName == null) {
      return;
    }
    try (OutputStream out = new FileOutputStream(fileName)) {
      PointFileReaderWriter.writeOneToMany(out, placements);
      ToolLogger.info("Data written to :" + new File(fileName).getCanonicalPath());
    } catch (final IOException e) {
      ToolLogger.error("Failed to write placements: " + fileName, e);
    }
  }

  /**
   * loadPlacements()
   * Loads a pre-defined file with map placement points.
   */
  private void loadPlacements() {
    ToolLogger.info("Load a placement file");
    final String placeName = new FileOpen("Load A Placement File", mapFolderLocation, ".txt").getPathString();
    if (placeName == null) {
      return;
    }
    try (InputStream in = new FileInputStream(placeName)) {
      placements = PointFileReaderWriter.readOneToMany(in);
    } catch (final IOException e) {
      ToolLogger.error("Failed to load placements: " + placeName, e);
      System.exit(0);
    }
    repaint();
  }

  /**
   * mouseEvent(java.awt.Point, java.lang.boolean, java.lang.boolean)
   * Usage:
   * left button start in territory
   * left button + control, add point
   * right button and ctrl write
   * right button remove last
   *
   * @param java
   *        .awt.Point point a point clicked by mouse
   * @param java
   *        .lang.boolean ctrlDown true if ctrl key was hit
   * @param java
   *        .lang.boolean rightMouse true if the right mouse button was hit
   */
  private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse) {
    if (!rightMouse && !ctrlDown) {
      currentCountry = Util.findTerritoryName(point, polygons, "there be dragons");
      // If there isn't an existing array, create one
      if ((placements == null) || (placements.get(currentCountry) == null)) {
        currentPlacements = new ArrayList<>();
      } else {
        currentPlacements = new ArrayList<>(placements.get(currentCountry));
      }
      JOptionPane.showMessageDialog(this, currentCountry);
    } else if (!rightMouse && ctrlDown) {
      if (currentPlacements != null) {
        currentPlacements.add(point);
      }
    } else if (rightMouse && ctrlDown) {
      if (currentPlacements != null) {
        // If there isn't an existing hashmap, create one
        if (placements == null) {
          placements = new HashMap<>();
        }
        placements.put(currentCountry, currentPlacements);
        currentPlacements = new ArrayList<>();
        ToolLogger.info("done:" + currentCountry);
      }
    } else if (rightMouse) {
      if ((currentPlacements != null) && !currentPlacements.isEmpty()) {
        currentPlacements.remove(currentPlacements.size() - 1);
      }
    }
    repaint();
  }

  private static String getUnitsScale() {
    final String unitsScale = JOptionPane.showInputDialog(null,
        "Enter the unit's scale (zoom).\r\n(e.g. 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5)");
    return (unitsScale != null) ? unitsScale : "1";
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    final String[] properties = getProperties();
    if (args.length == 1) {
      final String value;
      if (args[0].startsWith(TRIPLEA_UNIT_ZOOM)) {
        value = getValue(args[0]);
      } else {
        value = args[0];
      }
      try {
        Double.parseDouble(value);
        System.setProperty(TRIPLEA_UNIT_ZOOM, value);
      } catch (final Exception ex) {
        // ignore malformed input
      }
    } else if (args.length == 2) {
      final String value0;
      if (args[0].startsWith(TRIPLEA_UNIT_WIDTH)) {
        value0 = getValue(args[0]);
      } else {
        value0 = args[0];
      }
      try {
        Integer.parseInt(value0);
        System.setProperty(TRIPLEA_UNIT_WIDTH, value0);
      } catch (final Exception ex) {
        // ignore malformed input
      }
      final String value1;
      if (args[0].startsWith(TRIPLEA_UNIT_HEIGHT)) {
        value1 = getValue(args[1]);
      } else {
        value1 = args[1];
      }
      try {
        Integer.parseInt(value1);
        System.setProperty(TRIPLEA_UNIT_HEIGHT, value1);
      } catch (final Exception ex) {
        // ignore malformed input
      }
    }
    boolean usagePrinted = false;
    for (final String arg2 : args) {
      boolean found = false;
      String arg = arg2;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        arg = arg.substring(0, indexOf);
        for (final String propertie : properties) {
          if (arg.equals(propertie)) {
            final String value = getValue(arg2);
            System.setProperty(propertie, value);
            ToolLogger.info(propertie + ":" + value);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        ToolLogger.info("Unrecogized:" + arg2);
        if (!usagePrinted) {
          usagePrinted = true;
          ToolLogger.info("Arguments\r\n" + "   " + TRIPLEA_MAP_FOLDER + "=<FILE_PATH>\r\n" + "   "
              + TRIPLEA_UNIT_ZOOM + "=<UNIT_ZOOM_LEVEL>\r\n" + "   " + TRIPLEA_UNIT_WIDTH + "=<UNIT_WIDTH>\r\n" + "   "
              + TRIPLEA_UNIT_HEIGHT + "=<UNIT_HEIGHT>\r\n");
        }
      }
    }
    final String folderString = System.getProperty(TRIPLEA_MAP_FOLDER);
    if ((folderString != null) && (folderString.length() > 0)) {
      final File mapFolder = new File(folderString);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        ToolLogger.info("Could not find directory: " + folderString);
      }
    }
    final String zoomString = System.getProperty(TRIPLEA_UNIT_ZOOM);
    if ((zoomString != null) && (zoomString.length() > 0)) {
      try {
        unitZoomPercent = Double.parseDouble(zoomString);
        ToolLogger.info("Unit Zoom Percent to use: " + unitZoomPercent);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        ToolLogger.error("Not a decimal percentage: " + zoomString);
      }
    }
    final String widthString = System.getProperty(TRIPLEA_UNIT_WIDTH);
    if ((widthString != null) && (widthString.length() > 0)) {
      try {
        unitWidth = Integer.parseInt(widthString);
        ToolLogger.info("Unit Width to use: " + unitWidth);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        ToolLogger.error("Not an integer: " + widthString);
      }
    }
    final String heightString = System.getProperty(TRIPLEA_UNIT_HEIGHT);
    if ((heightString != null) && (heightString.length() > 0)) {
      try {
        unitHeight = Integer.parseInt(heightString);
        ToolLogger.info("Unit Height to use: " + unitHeight);
        placeDimensionsSet = true;
      } catch (final Exception e) {
        ToolLogger.error("Not an integer: " + heightString);
      }
    }
    if (placeDimensionsSet) {
      placeWidth = (int) (unitZoomPercent * unitWidth);
      placeHeight = (int) (unitZoomPercent * unitHeight);
      ToolLogger.info("Place Dimensions to use: " + placeWidth + "x" + placeHeight);
    }
  }
}
