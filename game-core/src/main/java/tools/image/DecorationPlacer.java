package tools.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.io.FileUtils;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;
import tools.util.ToolLogger;

/**
 * This is the DecorationPlacer, it will create a text file for you containing the points to place images at. <br>
 * <br>
 * In order to begin this, you must already have the map file, as well as the centers.txt and polygons.txt finished.
 * <br>
 * To start, load you map image. Then you will be asked which kind of Image Point File you are creating. <br>
 * <br>
 * There are basically 2 different kinds of image point files, and with each of those are 2 different sub-types. <br>
 * The 1st type is a folder full of many different images, that after being placed on the map will never be changed.
 * <br>
 * Examples of this are the decorations.txt file [misc folder] and the name_place.txt file [territoryNames folder]. <br>
 * In these files the 'point' string directly corresponds to exact name of an image file in the folder, with the only
 * <br>
 * exception being whether the point string needs the .png extension or not (decorations do, name_place does not). <br>
 * <br>
 * The 2nd type is single image, or small set of images, where the chosen image is determined by something in the xml
 * file. <br>
 * Examples of this are the pu_place.txt file [PUs folder] and the capitols.txt file [flags folder]. <br>
 * In these files, the 'point' string is the exact name of a territory, while the image file has a different name, <br>
 * and is chosen by the engine based on the game data. For things like the pu_place you may want the decoration placer
 * <br>
 * to generate placements for all territories, while others like capitols are more rare and you may want to individually
 * <br>
 * select which territories you need a placement point for. <br>
 * <br>
 * After selecting the point file type you want to make, the program will choose the default selections for you, <br>
 * but it will still confirm with you by asking you the questions. Just hit 'enter' a lot if you do not know the
 * answers. <br>
 * <br>
 * Any images that this program cannot find the point for, will start in the upper left corner of the map, <br>
 * and you may click on them to move them to their appropriate place." <br>
 * <br>
 * Do not forget to save the points when finished. To save and continue with another set of images, choose the <br>
 * option to 'Save Current And Keep On Map And Load New'. To reset all currently image points, use 'Load Image Points'.
 */
public class DecorationPlacer extends JFrame {
  private static final long serialVersionUID = 6385408390173085656L;
  // The map image will be stored here
  private Image image;
  // hash map for image points
  private Map<String, List<Point>> currentPoints = new HashMap<>();
  // hash map for center points
  private Map<String, Point> centers = new HashMap<>();
  // hash map for polygon points
  private Map<String, List<Polygon>> polygons = new HashMap<>();
  private final JLabel locationLabel = new JLabel();
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static File currentImageFolderLocation = null;
  private static File currentImagePointsTextFile = null;
  private Point currentMousePoint = new Point(0, 0);
  private Triple<String, Image, Point> currentSelectedImage = null;
  private Map<String, Tuple<Image, List<Point>>> currentImagePoints = new HashMap<>();
  private static boolean highlightAll = false;
  private static boolean createNewImageOnRightClick = false;
  private static Image staticImageForPlacing = null;
  private static boolean showFromTopLeft = true;
  private static ImagePointType imagePointType = ImagePointType.decorations;
  private static boolean cheapMutex = false;
  private static boolean showPointNames = false;

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
      final DecorationPlacer picker = new DecorationPlacer(mapName);
      picker.setSize(800, 600);
      picker.setLocationRelativeTo(null);
      picker.setVisible(true);
      JOptionPane.showMessageDialog(picker,
          new JLabel("<html>"
              + "This is the DecorationPlacer, it will create a text file for you containing the points to place "
              + "images at. "
              + "<br><br>In order to begin this, you must already have the map file, as well as the centers.txt and "
              + "polygons.txt finished. "
              + "<br>To start, load you map image. Then you will be asked which kind of Image Point File you are "
              + "creating. "
              + "<br><br>There are basically 2 different kinds of image point files, and with each of those are 2 "
              + "different sub-types. "
              + "<br>The 1st type is a folder full of many different images, that after being placed on the map will "
              + "never be changed. "
              + "<br>Examples of this are the decorations.txt file [misc folder] and the name_place.txt file "
              + "[territoryNames folder]. "
              + "<br>In these files the 'point' string directly corresponds to exact name of an image file in the "
              + "folder, with the only "
              + "<br>exception being whether the point string needs the .png extension or not (decorations do, "
              + "name_place does not). "
              + "<br><br>The 2nd type is single image, or small set of images, where the chosen image is determined by "
              + "something in the xml file. "
              + "<br>Examples of this are the pu_place.txt file [PUs folder] and the capitols.txt file [flags folder]. "
              + "<br>In these files, the 'point' string is the exact name of a territory, while the image file has a "
              + "different name, "
              + "<br>and is chosen by the engine based on the game data.  For things like the pu_place you may want "
              + "the decoration placer "
              + "<br>to generate placements for all territories, while others like capitols are more rare and you may "
              + "want to individually "
              + "<br>select which territories you need a placement point for."
              + "<br><br>After selecting the point file type you want to make, the program will choose the default "
              + "selections for you, "
              + "<br>but it will still confirm with you by asking you the questions. Just hit 'enter' a lot if you do "
              + "not know the answers. "
              + "<br><br>Any images that this program cannot find the point for, will start in the upper left corner "
              + "of the map, "
              + "<br>and you may click on them to move them to their appropriate place."
              + "<br><br>Do not forget to save the points when finished. To save and continue with another set of "
              + "images, choose the "
              + "<br>option to 'Save Current And Keep On Map And Load New'.  To reset all currently image points, use "
              + "'Load Image Points'."
              + "</html>"));
      picker.loadImagesAndPoints();
    } else {
      ToolLogger.info("No Image Map Selected. Shutting down.");
      System.exit(0);
    }
  } // end main

  private DecorationPlacer(final String mapName) {
    super("Decoration Placer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    highlightAll = false;
    File fileCenters = null;
    if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
      fileCenters = new File(mapFolderLocation, "centers.txt");
    }
    if ((fileCenters == null) || !fileCenters.exists()) {
      fileCenters = new File(new File(mapName).getParent() + File.separator + "centers.txt");
    }
    if (fileCenters.exists() && (JOptionPane.showConfirmDialog(new JPanel(),
        "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories "
            + "centers?",
        "File Suggestion", 1) == 0)) {
      try (InputStream is = new FileInputStream(fileCenters.getPath())) {
        ToolLogger.info("Centers : " + fileCenters.getPath());
        centers = PointFileReaderWriter.readOneToOne(is);
      } catch (final IOException e) {
        ToolLogger.error("Something wrong with Centers file", e);
        System.exit(0);
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
    File filePoly = null;
    if ((mapFolderLocation != null) && mapFolderLocation.exists()) {
      filePoly = new File(mapFolderLocation, "polygons.txt");
    }
    if ((filePoly == null) || !filePoly.exists()) {
      filePoly = new File(new File(mapName).getParent() + File.separator + "polygons.txt");
    }
    if (filePoly.exists() && (JOptionPane.showConfirmDialog(new JPanel(),
        "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories "
            + "polygons?",
        "File Suggestion", 1) == 0)) {
      try (InputStream is = new FileInputStream(filePoly.getPath())) {
        ToolLogger.info("Polygons : " + filePoly.getPath());
        polygons = PointFileReaderWriter.readOneToManyPolygons(is);
      } catch (final IOException e) {
        ToolLogger.error("Something wrong with your Polygons file: " + filePoly.getAbsolutePath(), e);
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
          ToolLogger.error("Something wrong with your Polygons file: " + polyPath, e);
          System.exit(0);
        }
      } else {
        ToolLogger.info("You must specify a Polgyon file.");
        ToolLogger.info("Shutting down.");
        System.exit(0);
      }
    }
    image = createImage(mapName);
    final JPanel imagePanel = createMainPanel();
    /*
     * Add a mouse listener to show
     * X : Y coordinates on the lower
     * left corner of the screen.
     */
    imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        locationLabel.setText((currentSelectedImage == null ? "" : currentSelectedImage.getFirst()) + "    x:"
            + e.getX() + " y:" + e.getY());
        currentMousePoint = new Point(e.getPoint());
        DecorationPlacer.this.repaint();
      }
    });
    locationLabel.setFont(new Font("Ariel", Font.BOLD, 16));
    /*
     * Add a mouse listener to monitor
     * for right mouse button being
     * clicked.
     */
    imagePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseEvent(e.isControlDown() || e.isShiftDown(), SwingUtilities.isRightMouseButton(e));
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
    final Action openAction = SwingAction.of("Load Image Locations", e -> loadImagesAndPoints());
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Image Points File");
    final Action saveAction = SwingAction.of("Save Image Locations", e -> saveImagePoints());
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Image Points To File");
    final Action keepGoingAction = SwingAction.of("Save Current and Keep Them On Map and Load New File", e -> {
      saveImagePoints();
      saveCurrentToMapPicture();
      loadImagesAndPoints();
    });
    keepGoingAction.putValue(Action.SHORT_DESCRIPTION,
        "Save current points to a file, then draw the images onto the map, then load a new points file.");
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
    fileMenu.add(keepGoingAction);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);
    final JCheckBoxMenuItem highlightAllModeItem = new JCheckBoxMenuItem("Highlight All", false);
    highlightAllModeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        highlightAll = highlightAllModeItem.getState();
        DecorationPlacer.this.repaint();
      }
    });
    final JCheckBoxMenuItem showNamesModeItem = new JCheckBoxMenuItem("Show Point Names", false);
    showNamesModeItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        showPointNames = showNamesModeItem.getState();
        DecorationPlacer.this.repaint();
      }
    });
    final Action clearAction = SwingAction.of("Clear All Current Points.", e -> currentImagePoints.clear());
    clearAction.putValue(Action.SHORT_DESCRIPTION, "Delete all points.");
    final JMenu editMenu = new JMenu("Edit");
    editMenu.setMnemonic('E');
    editMenu.add(highlightAllModeItem);
    editMenu.add(showNamesModeItem);
    editMenu.addSeparator();
    editMenu.add(clearAction);
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
  } // end constructor

  /**
   * createImage(java.lang.String)
   * creates the image map and makes sure
   * it is properly loaded.
   *
   * @param java
   *        .lang.String mapName the path of image map
   */
  private static Image createImage(final String mapName) {
    final Image image = Toolkit.getDefaultToolkit().createImage(mapName);
    Util.ensureImageLoaded(image);
    return image;
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
        // super.paint(g);
        paintToG(g);
      }
    };
    return imagePanel;
  }

  private void paintToG(final Graphics g) {
    if (cheapMutex) {
      return;
    }
    g.drawImage(image, 0, 0, this);
    g.setColor(Color.red);
    for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
      for (final Point p : entry.getValue().getSecond()) {
        g.drawImage(entry.getValue().getFirst(), p.x,
            p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)), null);
        if ((currentSelectedImage != null) && currentSelectedImage.getThird().equals(p)) {
          g.setColor(Color.green);
          g.drawRect(p.x, p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)),
              entry.getValue().getFirst().getWidth(null), entry.getValue().getFirst().getHeight(null));
          g.setColor(Color.red);
        } else if (highlightAll) {
          g.drawRect(p.x, p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)),
              entry.getValue().getFirst().getWidth(null), entry.getValue().getFirst().getHeight(null));
        }
        if (showPointNames) {
          g.drawString(entry.getKey(), p.x,
              p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)));
        }
      }
    }
    if (currentSelectedImage != null) {
      g.setColor(Color.green);
      g.drawImage(currentSelectedImage.getSecond(), currentMousePoint.x,
          currentMousePoint.y - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)), null);
      if (highlightAll) {
        g.drawRect(currentMousePoint.x,
            currentMousePoint.y - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)),
            currentSelectedImage.getSecond().getWidth(null), currentSelectedImage.getSecond().getHeight(null));
      }
      if (showPointNames) {
        g.drawString(currentSelectedImage.getFirst(), currentMousePoint.x,
            currentMousePoint.y - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)));
      }
    }
  }

  private void saveCurrentToMapPicture() {
    final BufferedImage bufferedImage =
        new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    final Graphics g = bufferedImage.getGraphics();
    final boolean saveHighlight = highlightAll;
    final boolean saveNames = showPointNames;
    highlightAll = false;
    showPointNames = false;
    paintToG(g);
    g.dispose();
    highlightAll = saveHighlight;
    showPointNames = saveNames;
    image = bufferedImage;
  }

  /**
   * saveCenters()
   * Saves the centers to disk.
   */
  private void saveImagePoints() {
    currentPoints = new HashMap<>();
    for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
      // remove duplicates
      final LinkedHashSet<Point> pointSet = new LinkedHashSet<>();
      pointSet.addAll(entry.getValue().getSecond());
      entry.getValue().getSecond().clear();
      entry.getValue().getSecond().addAll(pointSet);
      currentPoints.put(entry.getKey(), entry.getValue().getSecond());
    }
    final String fileName = new FileSave("Where To Save Image Points Text File?", JFileChooser.FILES_ONLY,
        currentImagePointsTextFile, mapFolderLocation).getPathString();
    if (fileName == null) {
      return;
    }
    try (OutputStream out = new FileOutputStream(fileName)) {
      PointFileReaderWriter.writeOneToMany(out, currentPoints);
      ToolLogger.info("Data written to :" + new File(fileName).getCanonicalPath());
    } catch (final IOException e) {
      ToolLogger.error("Failed to save points: " + fileName, e);
    }
  }

  private void selectImagePointType() {
    ToolLogger.info("Select Which type of image points file are we making?");
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(new JLabel("Which type of image points file are we making?"));
    final ButtonGroup group = new ButtonGroup();
    for (final ImagePointType type : ImagePointType.getTypes()) {
      final JRadioButton button = new JRadioButton(type.toString() + "      :      " + type.getDescription());
      button.setActionCommand(type.toString());
      if (imagePointType == type) {
        button.setSelected(true);
      } else {
        button.setSelected(false);
      }
      group.add(button);
      panel.add(button);
    }
    JOptionPane.showMessageDialog(this, panel, "Which type of image points file are we making?",
        JOptionPane.QUESTION_MESSAGE);
    final ButtonModel selected = group.getSelection();
    final String choice = selected.getActionCommand();
    for (final ImagePointType type : ImagePointType.getTypes()) {
      if (type.toString().equals(choice)) {
        imagePointType = type;
        ToolLogger.info("Selected Type: " + choice);
        break;
      }
    }
  }

  private void topLeftOrBottomLeft() {
    ToolLogger.info("Select Show images from top left or bottom left point?");
    final Object[] options = {"Point is Top Left", "Point is Bottom Left"};
    showFromTopLeft = JOptionPane.showOptionDialog(this,
        "Are the images shown from the top left, or from the bottom left point? \r\n"
            + "All images are shown from the top left, except for 'name_place.txt', 'pu_place.txt', and "
            + "'comments.txt'. \r\n"
            + "For these 3 files, whether they are top left or bottom left is determined by the \r\n"
            + "'map.properties' property: 'map.drawNamesFromTopLeft', which defaults to false if not specified "
            + "[meaning bottom left]. \r\n"
            + "Do NOT change this from whatever the default has choosen, unless you know exactly what you are doing!",
        "Show images from top left or bottom left point?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, options,
        options[(imagePointType.isCanUseBottomLeftPoint() ? 1 : 0)]) != JOptionPane.NO_OPTION;
  }

  private void loadImagesAndPoints() {
    cheapMutex = true;
    currentImagePoints = new HashMap<>();
    currentSelectedImage = null;
    selectImagePointType();
    ToolLogger.info("Select Folder full of images OR Text file full of points?");
    final Object[] miscOrNamesOptions = {"Folder Full of Images", "Text File Full of Points"};
    if (JOptionPane.showOptionDialog(this,
        "Are you doing a folder full of different images (decorations.txt [misc] and name_place.txt [territoryNames]) "
            + "\r\n"
            + "Or are we doing a per territory static or dynamic image based on game data (pu_place.txt [PUs], "
            + "capitols.txt [flags], etc, basically all others) ?",
        "Folder full of images OR Text file full of points?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, miscOrNamesOptions,
        miscOrNamesOptions[(imagePointType.isUseFolder() ? 0 : 1)]) != JOptionPane.NO_OPTION) {
      // points are 'territory name' as opposed to exactly an image file name, like 'territory name.png' (everything but
      // decorations is a
      // territory name, while decorations are image file names)
      loadImageFolder();
      if (currentImageFolderLocation == null) {
        return;
      }
      loadImagePointTextFile();
      topLeftOrBottomLeft();
      // decorations.txt (misc folder) and name_place.txt (territoryNames folder) use a different image for each point,
      // while everything else (like pu_place.txt (PUs folder)) will use either a static image or a dynamically chosen
      // image based on some
      // in game property in the game data.
      ToolLogger.info("Points end in .png OR they do not?");
      final Object[] pointsAreNamesOptions = {"Points end in .png", "Points do NOT end in .png"};
      fillCurrentImagePointsBasedOnImageFolder(JOptionPane.showOptionDialog(this,
          "Does the text file use the exact image file name, including the .png extension (decorations.txt) \r\n"
              + "Or does the text file not use the full file name with no extension, just a territory name "
              + "(name_place.txt) ?",
          "Points end in .png OR they do not?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
          pointsAreNamesOptions,
          pointsAreNamesOptions[(imagePointType.isEndInPng() ? 0 : 1)]) == JOptionPane.NO_OPTION);
      createNewImageOnRightClick = false;
      staticImageForPlacing = null;
    } else {
      loadImagePointTextFile();
      topLeftOrBottomLeft();
      // load all territories? things like pu_place.txt should have all or most territories, while things like
      // blockade.txt and
      // kamikaze_place.txt and capitols.txt will only have a small number of territories
      ToolLogger.info("Select Fill in all territories OR let you select them?");
      final Object[] fillAllOptions = {"Fill In All Territories", "Let Me Select Territories"};
      fillCurrentImagePointsBasedOnTextFile(JOptionPane.showOptionDialog(this,
          "Are you going to do a point for every single territory (pu_place.txt) \r\n"
              + "Or are you going to do just a few territories (capitols.txt, convoy.txt, vc.txt, etc, most others) ? "
              + "\r\n"
              + "(If you choose the later option, you must Right Click on a territory to create an image for that "
              + "territory.)",
          "Fill in all territories OR let you select them?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
          null, fillAllOptions, fillAllOptions[(imagePointType.isFillAll() ? 0 : 1)]) != JOptionPane.NO_OPTION);
    }
    cheapMutex = false;
    repaint();
    JOptionPane.showMessageDialog(this, new JLabel(imagePointType.getInstructions()));
  }

  private static void loadImageFolder() {
    ToolLogger.info("Load an image folder (eg: 'misc' or 'territoryNames', etc)");
    File folder = new File(mapFolderLocation, imagePointType.getFolderName());
    if (!folder.exists()) {
      folder = mapFolderLocation;
    }
    final FileSave imageFolder = new FileSave("Load an Image Folder", null, folder);
    if ((imageFolder.getPathString() == null) || (imageFolder.getFile() == null)
        || !imageFolder.getFile().exists()) {
      currentImageFolderLocation = null;
    } else {
      currentImageFolderLocation = imageFolder.getFile();
    }
  }

  private void loadImagePointTextFile() {
    ToolLogger.info("Load the points text file (eg: decorations.txt or pu_place.txt, etc)");
    final FileOpen centerName = new FileOpen("Load an Image Points Text File", mapFolderLocation,
        new File(mapFolderLocation, imagePointType.getFileName()), ".txt");
    currentImagePointsTextFile = centerName.getFile();
    if ((centerName.getFile() != null) && centerName.getFile().exists() && (centerName.getPathString() != null)) {
      try (InputStream in = new FileInputStream(centerName.getPathString())) {
        currentPoints = PointFileReaderWriter.readOneToMany(in);
      } catch (final IOException e) {
        ToolLogger.error("Failed to load image points: " + centerName.getPathString(), e);
        System.exit(0);
      }
    } else {
      currentPoints = new HashMap<>();
    }
  }

  private void fillCurrentImagePointsBasedOnTextFile(final boolean fillInAllTerritories) {
    staticImageForPlacing = null;
    File image = new File(mapFolderLocation + File.separator + imagePointType.getFolderName(),
        imagePointType.getImageName());
    if (!image.exists()) {
      image = new File(ClientFileSystemHelper.getRootFolder() + File.separator + ResourceLoader.RESOURCE_FOLDER
          + File.separator + imagePointType.getFolderName(), imagePointType.getImageName());
    }
    if (!image.exists()) {
      image = null;
    }
    while (staticImageForPlacing == null) {
      final FileOpen imageSelection = new FileOpen("Select Example Image To Use",
          ((image == null) ? mapFolderLocation : new File(image.getParent())), image, ".gif", ".png");
      if ((imageSelection.getFile() == null) || !imageSelection.getFile().exists()) {
        continue;
      }
      staticImageForPlacing = createImage(imageSelection.getPathString());
    }
    final int width = staticImageForPlacing.getWidth(null);
    final int height = staticImageForPlacing.getHeight(null);
    final int addY = ((imagePointType == ImagePointType.comments) ? ((-ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS))
        : ((imagePointType == ImagePointType.pu_place) ? (ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS) : 0));
    if (fillInAllTerritories) {
      for (final Entry<String, Point> entry : centers.entrySet()) {
        List<Point> points = currentPoints.get(entry.getKey());
        if (points == null) {
          ToolLogger.info("Did NOT find point for: " + entry.getKey());
          points = new ArrayList<>();
          final Point p = new Point(entry.getValue().x - (width / 2),
              entry.getValue().y + addY + ((showFromTopLeft ? -1 : 1) * (height / 2)));
          points.add(p);
        } else {
          ToolLogger.info("Found point for: " + entry.getKey());
        }
        currentImagePoints.put(entry.getKey(), Tuple.of(staticImageForPlacing, points));
      }
    } else {
      for (final Entry<String, List<Point>> entry : currentPoints.entrySet()) {
        currentImagePoints.put(entry.getKey(),
            Tuple.of(staticImageForPlacing, entry.getValue()));
      }
    }
    // !fillInAllTerritories;
    createNewImageOnRightClick = true;
  }

  private void fillCurrentImagePointsBasedOnImageFolder(final boolean pointsAreExactlyTerritoryNames) {
    final int addY = ((imagePointType == ImagePointType.comments) ? ((-ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS))
        : ((imagePointType == ImagePointType.pu_place) ? (ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS) : 0));
    final List<String> allTerritories = new ArrayList<>(centers.keySet());
    for (final File file : FileUtils.listFiles(currentImageFolderLocation)) {
      if (!file.getPath().endsWith(".png") && !file.getPath().endsWith(".gif")) {
        continue;
      }
      final String imageName = file.getName();
      final String possibleTerritoryName = imageName.substring(0, imageName.length() - 4);
      final Image image = createImage(file.getPath());
      List<Point> points = ((currentPoints != null)
          ? currentPoints.get((pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName))
          : null);
      if (points == null) {
        points = new ArrayList<>();
        Point p = centers.get(possibleTerritoryName);
        if (p == null) {
          ToolLogger.info("Did NOT find point for: " + possibleTerritoryName);
          points.add(new Point(50, 50));
        } else {
          p = new Point(p.x - (image.getWidth(null) / 2),
              p.y + addY + ((showFromTopLeft ? -1 : 1) * (image.getHeight(null) / 2)));
          points.add(p);
          allTerritories.remove(possibleTerritoryName);
          ToolLogger.info("Found point for: " + possibleTerritoryName);
        }
      } else {
        allTerritories.remove(possibleTerritoryName);
      }
      currentImagePoints.put((pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName),
          Tuple.of(image, points));
    }
    if (!allTerritories.isEmpty() && (imagePointType == ImagePointType.name_place)) {
      JOptionPane.showMessageDialog(this, new JLabel("Territory images not found in folder: " + allTerritories));
      ToolLogger.info("Territory images not found in folder: " + allTerritories);
    }
  }

  /**
   * mouseEvent(java.lang.boolean, java.lang.boolean)
   *
   * @param java
   *        .lang.boolean ctrlDown true if ctrl key was hit
   * @param java
   *        .lang.boolean rightMouse true if the right mouse button was hit
   */
  private void mouseEvent(final boolean ctrlDown, final boolean rightMouse) {
    if (cheapMutex) {
      return;
    }
    if (!rightMouse && !ctrlDown && (currentSelectedImage == null)) {
      // find whatever image we are left clicking on
      Point testPoint = null;
      for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
        for (final Point p : entry.getValue().getSecond()) {
          if ((testPoint == null) || (p.distance(currentMousePoint) < testPoint.distance(currentMousePoint))) {
            testPoint = p;
            currentSelectedImage = Triple.of(entry.getKey(), entry.getValue().getFirst(), p);
          }
        }
      }
    } else if (!rightMouse && !ctrlDown && (currentSelectedImage != null)) {
      // save the image
      final Tuple<Image, List<Point>> imagePoints = currentImagePoints.get(currentSelectedImage.getFirst());
      final List<Point> points = imagePoints.getSecond();
      points.remove(currentSelectedImage.getThird());
      points.add(new Point(currentMousePoint));
      currentImagePoints.put(currentSelectedImage.getFirst(),
          Tuple.of(currentSelectedImage.getSecond(), points));
      currentSelectedImage = null;
    } else if (rightMouse && !ctrlDown && createNewImageOnRightClick && (staticImageForPlacing != null)
        && (currentSelectedImage == null)) {
      // create a new point here in this territory
      final Optional<String> territoryName = Util.findTerritoryName(currentMousePoint, polygons);
      if (territoryName.isPresent()) {
        final List<Point> points = new ArrayList<>();
        points.add(new Point(currentMousePoint));
        currentImagePoints.put(territoryName.get(), Tuple.of(staticImageForPlacing, points));
      }
    } else if (rightMouse && !ctrlDown && imagePointType.isCanHaveMultiplePoints()) {
      // if none selected find the image we are clicking on, and duplicate it (not replace/move it)
      if (currentSelectedImage == null) {
        Point testPoint = null;
        for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
          for (final Point p : entry.getValue().getSecond()) {
            if ((testPoint == null) || (p.distance(currentMousePoint) < testPoint.distance(currentMousePoint))) {
              testPoint = p;
              currentSelectedImage =
                  Triple.of(entry.getKey(), entry.getValue().getFirst(), null);
            }
          }
        }
      } else {
        currentSelectedImage = Triple.of(currentSelectedImage.getFirst(),
            currentSelectedImage.getSecond(), null);
      }
      // then save (same code as above for saving)
      final Tuple<Image, List<Point>> imagePoints = currentImagePoints.get(currentSelectedImage.getFirst());
      final List<Point> points = imagePoints.getSecond();
      points.remove(currentSelectedImage.getThird());
      points.add(new Point(currentMousePoint));
      currentImagePoints.put(currentSelectedImage.getFirst(),
          Tuple.of(currentSelectedImage.getSecond(), points));
      currentSelectedImage = null;
    } else if (rightMouse && ctrlDown) {
      // must be right click AND ctrl down to delete an image
      if (currentSelectedImage == null) {
        return;
      }
      final Tuple<Image, List<Point>> current = currentImagePoints.get(currentSelectedImage.getFirst());
      final List<Point> points = current.getSecond();
      points.remove(currentSelectedImage.getThird());
      if (points.isEmpty()) {
        currentImagePoints.remove(currentSelectedImage.getFirst());
      }
      currentSelectedImage = null;
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

  enum ImagePointType {
    decorations(
        "decorations.txt", "misc", "decorationExample.png", true, true, true, false, true, true,
        "decorations.txt will place any kind of image you want anywhere, using the 'misc' folder",
        "<html>decorations.txt will allow for multiple points per image. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create a copy of currently selected image OR closest image <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    name_place(
        "name_place.txt", "territoryNames", "territoryName.png", true, false, true, true, false, false,
        "name_place.txt only places images with the exact name of the territories on map, using the 'territoryNames' "
            + "folder",
        "<html>name_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = nothing <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    pu_place(
        "pu_place.txt", "PUs", "2.png", false, false, true, true, false, false,
        "pu_place.txt is the point where the PUs get shown,"
            + " and picks the PU images (like '2.png') from the 'PUs' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    capitols(
        "capitols.txt", "flags", "Neutral_large.png", false, false, false, false, false, true,
        "capitols.txt is the point where a capitol flag is shown,"
            + " and picks the <name>_large.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    vc(
        "vc.txt", "misc", "vc.png", false, false, false, false, false, true,
        "vc.txt is the point where a Victory City icon is shown, and picks the 'vc.png' image from the 'misc' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    blockade(
        "blockade.txt", "misc", "blockade.png", false, false, false, false, false, true,
        "blockade.txt is the point where a blockade zone icon is shown, and picks the 'blockade.png' image from the "
            + "'misc' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    convoy(
        "convoy.txt", "flags", "Neutral.png", false, false, false, false, false, true,
        "convoy.txt is the point where a nation flag is shown on any sea zone that has production ability,"
            + " and picks the <name>.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    comments(
        "comments.txt", "misc", "exampleConvoyText.png", false, false, false, true, false, false,
        "comments.txt is the point where text details about a convoy zone or route is shown, and it does not use any "
            + "image, instead it writes the text in-game",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    kamikaze_place(
        "kamikaze_place.txt", "flags", "Neutral_fade.png", false, false, false, false, false, true,
        "kamikaze_place.txt is the point where a kamikaze zone symbol is shown,"
            + " and it picks the <name>_fade.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    territory_effects(
        "territory_effects.txt", "territoryEffects", "mountain.png", false, false, false, false, true, true,
        "territory_effects.txt is the point where a territory effect image is shown,"
            + " and it picks the <effect>.png image from the 'territoryEffects' folder",
        "<html>pu_place.txt will allow for multiple points per image. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = copy selected image OR create an image for this territory<br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>");

    public static final int SPACE_BETWEEN_NAMES_AND_PUS = 32;
    private final String fileName;
    private final String folderName;
    private final String imageName;
    private final boolean useFolder;
    private final boolean endInPng;
    private final boolean fillAll;
    private final boolean canUseBottomLeftPoint;
    private final boolean canHaveMultiplePoints;
    private final boolean usesCentersPoint;
    private final String description;
    private final String instructions;

    protected static ImagePointType[] getTypes() {
      return new ImagePointType[] {decorations, name_place, pu_place, capitols, vc, blockade, convoy, comments,
          kamikaze_place, territory_effects};
    }

    ImagePointType(final String fileName, final String folderName, final String imageName, final boolean useFolder,
        final boolean endInPng, final boolean fillAll, final boolean canUseBottomLeftPoint,
        final boolean canHaveMultiplePoints, final boolean usesCentersPoint, final String description,
        final String instructions) {
      this.fileName = fileName;
      this.folderName = folderName;
      this.imageName = imageName;
      this.useFolder = useFolder;
      this.endInPng = endInPng;
      this.fillAll = fillAll;
      this.canUseBottomLeftPoint = canUseBottomLeftPoint;
      this.canHaveMultiplePoints = canHaveMultiplePoints;
      this.usesCentersPoint = usesCentersPoint;
      this.description = description;
      this.instructions = instructions;
    }

    public String getFileName() {
      return fileName;
    }

    public String getFolderName() {
      return folderName;
    }

    public String getImageName() {
      return imageName;
    }

    public boolean isUseFolder() {
      return useFolder;
    }

    public boolean isEndInPng() {
      return endInPng;
    }

    public boolean isFillAll() {
      return fillAll;
    }

    public boolean isCanUseBottomLeftPoint() {
      return canUseBottomLeftPoint;
    }

    public boolean isCanHaveMultiplePoints() {
      return canHaveMultiplePoints;
    }

    public boolean isUsesCentersPoint() {
      return usesCentersPoint;
    }

    public String getDescription() {
      return description;
    }

    public String getInstructions() {
      return instructions;
    }
  }
}
