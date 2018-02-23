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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
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

import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import tools.util.ToolLogger;

public class CenterPicker extends JFrame {
  private static final long serialVersionUID = -5633998810385136625L;
  // The map image will be stored here
  private Image image;
  // hash map for center points
  private Map<String, Point> centers = new HashMap<>();
  // hash map for polygon points
  private Map<String, List<Polygon>> polygons = new HashMap<>();
  private final JLabel locationLabel = new JLabel();
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";

  /**
   * main(java.lang.String[])
   * Main program begins here.
   * Asks the user to select the map then runs the
   * the actual picker.
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
      final CenterPicker picker = new CenterPicker(mapName);
      picker.setSize(800, 600);
      picker.setLocationRelativeTo(null);
      picker.setVisible(true);
      JOptionPane.showMessageDialog(picker,
          new JLabel("<html>" + "This is the CenterPicker, it will create a centers.txt file for you. "
              + "<br>Please click on the center of every single territory and sea zone on your map, and give each a "
              + "name. "
              + "<br>The point you clicked on will tell TripleA where to put things like any flags, text, unit "
              + "placements, etc, "
              + "<br>so be sure to click in the exact middle, or slight up and left of the middle, of each territory "
              + "<br>(but still within the territory borders)."
              + "<br>Do not use special or illegal characters in territory names."
              + "<br><br>You can also load an existing centers.txt file, then make modifications to it, then save it "
              + "again."
              + "<br><br>LEFT CLICK = create a new center point for a territory/zone."
              + "<br><br>RIGHT CLICK on an existing center = delete that center point."
              + "<br><br>When finished, save the centers and exit." + "</html>"));
    } else {
      ToolLogger.info("No Image Map Selected. Shutting down.");
      System.exit(0);
    }
  } // end main

  /**
   * Constructor CenterPicker(java.lang.String)
   * Setus up all GUI components, initializes variables with
   * default or needed values, and prepares the map for user
   * commands.
   *
   * @param mapName Name of map file.
   */
  public CenterPicker(final String mapName) {
    super("Center Picker");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    File file = null;
    if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
      file = new File(mapFolderLocation, "polygons.txt");
    }
    if ((file == null) || !file.exists()) {
      file = new File(new File(mapName).getParent() + File.separator + "polygons.txt");
    }
    if (file.exists() && (JOptionPane.showConfirmDialog(new JPanel(),
        "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories "
            + "names?",
        "File Suggestion", 1) == 0)) {
      try (InputStream is = new FileInputStream(file.getPath())) {
        polygons = PointFileReaderWriter.readOneToManyPolygons(is);
      } catch (final IOException e) {
        ToolLogger.error("Something wrong with your Polygons file: " + file.getAbsolutePath(), e);
        System.exit(0);
      }
    } else {
      final String polyPath = new FileOpen("Select A Polygon File", mapFolderLocation, ".txt").getPathString();
      if (polyPath != null) {
        try (InputStream is = new FileInputStream(polyPath)) {
          polygons = PointFileReaderWriter.readOneToManyPolygons(is);
        } catch (final IOException e) {
          ToolLogger.error("Something wrong with your Polygons file: " + polyPath, e);
          System.exit(0);
        }
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
      }
    });
    // Add a mouse listener to monitor for right mouse button being clicked.
    imagePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseEvent(e.getPoint(), SwingUtilities.isRightMouseButton(e));
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
    final Action openAction = SwingAction.of("Load Centers", e -> loadCenters());
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Center Points File");
    final Action saveAction = SwingAction.of("Save Centers", e -> saveCenters());
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Center Points To File");
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
    menuBar.add(fileMenu);
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
      private static final long serialVersionUID = -7130828419508975924L;

      @Override
      public void paint(final Graphics g) {
        g.drawImage(image, 0, 0, this);
        g.setColor(Color.red);
        for (final String centerName : centers.keySet()) {
          final Point item = centers.get(centerName);
          g.fillOval(item.x, item.y, 15, 15);
          g.drawString(centerName, item.x + 17, item.y + 13);
        }
      }
    };
    return imagePanel;
  }

  /**
   * saveCenters()
   * Saves the centers to disk.
   */
  private void saveCenters() {
    final String fileName =
        new FileSave("Where To Save centers.txt ?", "centers.txt", mapFolderLocation).getPathString();
    if (fileName == null) {
      return;
    }
    try (OutputStream out = new FileOutputStream(fileName)) {
      PointFileReaderWriter.writeOneToOne(out, centers);
      ToolLogger.info("Data written to :" + new File(fileName).getCanonicalPath());
    } catch (final IOException e) {
      ToolLogger.error("Failed to save centers: " + fileName, e);
    }
  }

  /**
   * loadCenters()
   * Loads a pre-defined file with map center points.
   */
  private void loadCenters() {
    ToolLogger.info("Load a center file");
    final String centerName = new FileOpen("Load A Center File", mapFolderLocation, ".txt").getPathString();
    if (centerName == null) {
      return;
    }
    try (InputStream in = new FileInputStream(centerName)) {
      centers = PointFileReaderWriter.readOneToOne(in);
    } catch (final IOException e) {
      ToolLogger.error("Failed to load centers: " + centerName, e);
    }
    repaint();
  }

  /**
   * java.lang.String findTerritoryName(java.awt.Point)
   * Finds a land territory name or
   * some sea zone name.
   *
   * @param java
   *        .awt.point p a point on the map
   */
  private String findTerritoryName(final Point p) {
    return Util.findTerritoryName(p, polygons, "unknown");
  }

  /**
   * mouseEvent(java.awt.Point, java.lang.boolean, java.lang.boolean)
   *
   * @param java
   *        .awt.Point point a point clicked by mouse
   * @param java
   *        .lang.boolean ctrlDown true if ctrl key was hit
   * @param java
   *        .lang.boolean rightMouse true if the right mouse button was hit
   */
  private void mouseEvent(final Point point, final boolean rightMouse) {
    if (!rightMouse) {
      String name = findTerritoryName(point);
      name = JOptionPane.showInputDialog(this, "Enter the territory name:", name);
      if ((name == null) || (name.trim().length() == 0)) {
        return;
      }
      if (centers.containsKey(name) && (JOptionPane.showConfirmDialog(this,
          "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0)) {
        return;
      }
      centers.put(name, point);
    } else {
      String centerClicked = null;
      for (final Entry<String, Point> cur : centers.entrySet()) {
        if (new Rectangle(cur.getValue(), new Dimension(15, 15))
            .intersects(new Rectangle(point, new Dimension(1, 1)))) {
          centerClicked = cur.getKey();
        }
      }
      if ((centerClicked != null)
          && (JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this center?") == 0)) {
        centers.remove(centerClicked);
      }
    }
    repaint();
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
