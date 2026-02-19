package tools.map.making.ui.runnable;

import games.strategy.triplea.image.MapImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.util.PointFileReaderWriter;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;
import tools.map.making.ui.MapEditorFrame;
import tools.util.FileHelper;
import tools.util.FileOpen;
import tools.util.FileSave;
import tools.util.ToolsUtil;

/**
 * This is the DecorationPlacerTask, it will create a text file for you containing the points to
 * place images at. <br>
 * <br>
 * In order to begin this, you must already have the map file, as well as the centers.txt and
 * polygons.txt finished. <br>
 * To start, load you map image. Then you will be asked which kind of Image Point File you are
 * creating. <br>
 * <br>
 * There are basically 2 different kinds of image point files, and with each of those are 2
 * different subtypes. <br>
 * The 1st type is a folder full of many different images, that after being placed on the map will
 * never be changed. <br>
 * Examples of this are the decorations.txt file [misc folder] and the name_place.txt file
 * [territoryNames folder]. <br>
 * In these files the 'point' string directly corresponds to exact name of an image file in the
 * folder, with the only <br>
 * exception being whether the point string needs the .png extension or not (decorations do,
 * name_place does not). <br>
 * <br>
 * The 2nd type is single image, or small set of images, where the chosen image is determined by
 * something in the xml file. <br>
 * Examples of this are the pu_place.txt file [PUs folder] and the capitols.txt file [flags folder].
 * <br>
 * In these files, the 'point' string is the exact name of a territory, while the image file has a
 * different name, <br>
 * and is chosen by the engine based on the game data. For things like the pu_place you may want the
 * decoration placer <br>
 * to generate placements for all territories, while others like capitols are rarer, and you may
 * want to individually <br>
 * select which territories you need a placement point for. <br>
 * <br>
 * After selecting the point file type you want to make, the program will choose the default
 * selections for you, <br>
 * but it will still confirm with you by asking you the questions. Just hit 'enter' a lot if you do
 * not know the answers. <br>
 * <br>
 * Any images that this program cannot find the point for, will start in the upper left corner of
 * the map, <br>
 * and you may click on them to move them to their appropriate place. <br>
 * <br>
 * Do not forget to save the points when finished. To save and continue with another set of images,
 * choose the <br>
 * option to 'Save Current And Keep On Map And Load New'. To reset all currently image points, use
 * 'Load Image Points'.
 */
@Slf4j
public final class DecorationPlacerTask extends MapEditorRunnableTask {

  DecorationPlacerTask() {}

  public static void run() {
    runTask(DecorationPlacerTask.class);
  }

  @Override
  public MapEditorFrame getFrame(Path mapPath) throws IOException {
    return new DecorationPlacerFrame(mapPath);
  }

  @Override
  public String getWelcomeMessage() {
    return """
        <html>\
        This is the DecorationPlacerTask, it will create a text file for you containing \
        the points to place images at. \
        <br><br>In order to begin this, you must already have the map file, as well \
        as the centers.txt and polygons.txt finished. \
        <br>To start, load you map image. Then you will be asked which kind of Image \
        Point File you are creating. \
        <br><br>There are basically 2 different kinds of image point files, and with \
        each of those are 2 different sub-types. \
        <br>The 1st type is a folder full of many different images, that after being \
        placed on the map will never be changed. \
        <br>Examples of this are the decorations.txt file [misc folder] and the \
        name_place.txt file [territoryNames folder]. \
        <br>In these files the 'point' string directly corresponds to exact name of \
        an image file in the folder, with the only \
        <br>exception being whether the point string needs the .png extension or \
        not (decorations do, name_place does not). \
        <br><br>The 2nd type is single image, or small set of images, where the \
        chosen image is determined by something in the xml file. \
        <br>Examples of this are the pu_place.txt file [PUs folder] and the \
        capitols.txt file [flags folder]. \
        <br>In these files, the 'point' string is the exact name of a territory, \
        while the image file has a different name, \
        <br>and is chosen by the engine based on the game data.  For things like the \
        pu_place you may want the decoration placer \
        <br>to generate placements for all territories, while others like capitols \
        are more rare and you may want to individually \
        <br>select which territories you need a placement point for.\
        <br><br>After selecting the point file type you want to make, the program \
        will choose the default selections for you, \
        <br>but it will still confirm with you by asking you the questions. Just hit \
        'enter' a lot if you do not know the answers. \
        <br><br>Any images that this program cannot find the point for, will start \
        in the upper left corner of the map, \
        <br>and you may click on them to move them to their appropriate place.\
        <br><br>Do not forget to save the points when finished. To save and continue \
        with another set of images, choose the \
        <br>option to 'Save Current And Keep On Map And Load New'.  To reset all \
        currently image points, use 'Load Image Points'.\
        </html>""";
  }

  @Getter
  enum ImagePointType {
    DECORATIONS(
        "decorations.txt",
        "misc",
        "decorationExample.png",
        true,
        true,
        true,
        false,
        true,
        "decorations.txt will place any kind of image you want anywhere, using the 'misc' folder",
        "<html>decorations.txt will allow for multiple points per image. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create a copy of currently selected image OR closest image <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    NAME_PLACE(
        "name_place.txt",
        "territoryNames",
        "territoryName.png",
        true,
        false,
        true,
        true,
        false,
        "name_place.txt only places images with the exact name of the territories on "
            + "map, using the 'territoryNames' folder",
        "<html>name_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = nothing <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    PU_PLACE(
        "pu_place.txt",
        "PUs",
        "2.png",
        false,
        false,
        true,
        true,
        false,
        "pu_place.txt is the point where the PUs get shown,"
            + " and picks the PU images (like '2.png') from the 'PUs' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    CAPITOLS(
        "capitols.txt",
        "flags",
        "Neutral_large.png",
        false,
        false,
        false,
        false,
        false,
        "capitols.txt is the point where a capitol flag is shown,"
            + " and picks the <name>_large.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    VICTORY_CITIES(
        "vc.txt",
        "misc",
        "vc.png",
        false,
        false,
        false,
        false,
        false,
        "vc.txt is the point where a Victory City icon is shown, and picks the 'vc.png' "
            + "image from the 'misc' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    BLOCKADE(
        "blockade.txt",
        "misc",
        "blockade.png",
        false,
        false,
        false,
        false,
        false,
        "blockade.txt is the point where a blockade zone icon is shown, and picks "
            + "the 'blockade.png' image from the 'misc' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    CONVOY(
        "convoy.txt",
        "flags",
        "Neutral.png",
        false,
        false,
        false,
        false,
        false,
        "convoy.txt is the point where a nation flag is shown on any sea zone that has "
            + "production ability, and picks the <name>.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    COMMENTS(
        "comments.txt",
        "misc",
        "exampleConvoyText.png",
        false,
        false,
        false,
        true,
        false,
        "comments.txt is the point where text details about a convoy zone or route "
            + "is shown, and it does not use any image, instead it writes the text in-game",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    KAMIKAZE_PLACE(
        "kamikaze_place.txt",
        "flags",
        "Neutral_fade.png",
        false,
        false,
        false,
        false,
        false,
        "kamikaze_place.txt is the point where a kamikaze zone symbol is shown,"
            + " and it picks the <name>_fade.png image from the 'flags' folder",
        "<html>pu_place.txt only allows 1 point per image/territory. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = create an image and point for this territory if none exists yet <br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

    TERRITORY_EFFECTS(
        "territory_effects.txt",
        "territoryEffects",
        "mountain_large.png",
        false,
        false,
        false,
        false,
        true,
        "territory_effects.txt is the point where a territory effect image is shown,"
            + " and it picks the <effect>_large.png or <effect>.png image from the "
            + "'territoryEffects' folder",
        "<html>pu_place.txt will allow for multiple points per image. <br>"
            + "Left Click = select closest image  OR  place currently selected image <br>"
            + "Right click = copy selected image OR create an image for this territory<br>"
            + "CTRL/SHIFT + Right Click = delete currently selected image point</html>");

    static final int SPACE_BETWEEN_NAMES_AND_PUS = 32;

    private final String fileName;
    private final String folderName;
    private final String imageName;
    private final boolean useFolder;
    private final boolean endInPng;
    private final boolean fillAll;
    private final boolean canUseBottomLeftPoint;
    private final boolean canHaveMultiplePoints;
    private final String description;
    private final String instructions;

    ImagePointType(
        final String fileName,
        final String folderName,
        final String imageName,
        final boolean useFolder,
        final boolean endInPng,
        final boolean fillAll,
        final boolean canUseBottomLeftPoint,
        final boolean canHaveMultiplePoints,
        final String description,
        final String instructions) {
      this.fileName = fileName;
      this.folderName = folderName;
      this.imageName = imageName;
      this.useFolder = useFolder;
      this.endInPng = endInPng;
      this.fillAll = fillAll;
      this.canUseBottomLeftPoint = canUseBottomLeftPoint;
      this.canHaveMultiplePoints = canHaveMultiplePoints;
      this.description = description;
      this.instructions = instructions;
    }

    static ImagePointType[] getTypes() {
      return new ImagePointType[] {
        DECORATIONS,
        NAME_PLACE,
        PU_PLACE,
        CAPITOLS,
        VICTORY_CITIES,
        BLOCKADE,
        CONVOY,
        COMMENTS,
        KAMIKAZE_PLACE,
        TERRITORY_EFFECTS
      };
    }
  }

  private static final class DecorationPlacerFrame extends MapEditorFrame {
    private static final long serialVersionUID = 6385408390173085656L;
    // hash map for center points
    private final Map<String, Point> centers;
    // hash map for polygon points
    private final Map<String, List<Polygon>> polygons;
    // The map image will be stored here
    // hash map for image points
    private Map<String, List<Point>> currentPoints = new HashMap<>();
    private @Nullable Path currentImageFolderLocation = null;
    private Path currentImagePointsTextFile = null;
    private Point currentMousePoint = new Point(0, 0);
    private Triple<String, Image, Point> currentSelectedImage = null;
    private Map<String, Tuple<Image, List<Point>>> currentImagePoints = new HashMap<>();
    private boolean highlightAll;
    private boolean createNewImageOnRightClick = false;
    private @Nullable Image staticImageForPlacing = null;
    private boolean showFromTopLeft = true;
    private ImagePointType imagePointType = ImagePointType.DECORATIONS;
    private boolean cheapMutex = false;
    private boolean showPointNames = false;

    DecorationPlacerFrame(final Path mapFolder) throws IOException {
      super("Decoration Placer", mapFolder);
      highlightAll = false;
      final Path centersFile = getCentersFile(mapFolder);
      try {
        log.info("Centers : {}", centersFile);
        centers = PointFileReaderWriter.readOneToOne(centersFile);
      } catch (final IOException e) {
        log.error("Something wrong with Centers file", e);
        throw e;
      }
      final Path polygonsPath = getPolygonsPath(mapFolder);
      try {
        log.info("Polygons : {}", polygonsPath);
        polygons = PointFileReaderWriter.readOneToManyPolygons(polygonsPath);
      } catch (final IOException e) {
        log.error("Something wrong with your Polygons file: {}", polygonsPath);
        throw e;
      }
      initializeLayout();
      loadImagesAndPoints();
    }

    @Override
    protected Font getLocationLabelFont(Font defaultFont) {
      return new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 16);
    }

    @Override
    protected String getLocationLabelPrefix() {
      return (currentSelectedImage == null ? "" : currentSelectedImage.getFirst()) + "    ";
    }

    @Override
    protected void reactToMouseMoved(MouseEvent e) {
      currentMousePoint = new Point(e.getPoint());
      repaint();
    }

    @Override
    protected void reactToMouseClicked(MouseEvent e) {
      mouseEvent(e.isControlDown() || e.isShiftDown(), SwingUtilities.isRightMouseButton(e));
    }

    @Override
    protected void initializeLayout() {
      // set up the actions
      final Action openAction = SwingAction.of("Load Image Locations", e -> loadImagesAndPoints());
      openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Image Points File");
      final Action saveAction = SwingAction.of("Save Image Locations", e -> saveImagePoints());
      saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Image Points To File");
      final Action keepGoingAction =
          SwingAction.of(
              "Save Current and Keep Them On Map and Load New File",
              e -> {
                saveImagePoints();
                saveCurrentToMapPicture();
                loadImagesAndPoints();
              });
      keepGoingAction.putValue(
          Action.SHORT_DESCRIPTION,
          "Save current points to a file, then draw the images onto the map, then "
              + "load a new points file.");
      final Action exitAction =
          SwingAction.of(
              "Exit",
              e -> {
                setVisible(false);
                dispose();
              });
      exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
      setupMenuBar(openAction, saveAction, exitAction, keepGoingAction);
    }

    private void setupMenuBar(
        Action openAction, Action saveAction, Action exitAction, Action keepGoingAction) {
      final JCheckBoxMenuItem highlightAllModeItem = new JCheckBoxMenuItem("Highlight All", false);
      highlightAllModeItem.addActionListener(
          event -> {
            highlightAll = highlightAllModeItem.getState();
            repaint();
          });
      final JCheckBoxMenuItem showNamesModeItem = new JCheckBoxMenuItem("Show Point Names", false);
      showNamesModeItem.addActionListener(
          event -> {
            showPointNames = showNamesModeItem.getState();
            repaint();
          });
      final Action clearAction =
          SwingAction.of("Clear All Current Points.", e -> currentImagePoints.clear());
      clearAction.putValue(Action.SHORT_DESCRIPTION, "Delete all points.");
      final JMenu editMenu =
          new JMenuBuilder("Edit", KeyCode.E)
              .addMenuItem(highlightAllModeItem)
              .addMenuItem(showNamesModeItem)
              .addSeparator()
              .addMenuItem(new JMenuItemBuilder(clearAction, KeyCode.C))
              .build();
      // set up the menu bar
      final JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);
      menuBar.add(
          getFileMenu(
              openAction,
              saveAction,
              exitAction,
              new JMenuItemBuilder(keepGoingAction, KeyCode.K).build()));
      menuBar.add(editMenu);
    }

    @Nonnull
    private Path getPolygonsPath(Path mapFolder) throws IOException {
      final Path polygonsPath;
      final Path filePoly =
          FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "polygons.txt");
      if (Files.exists(filePoly)
          && JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "A polygons.txt file was found in the map's folder, "
                      + "do you want to use the file to supply the territories polygons?",
                  "File Suggestion",
                  JOptionPane.YES_NO_CANCEL_OPTION)
              == 0) {
        polygonsPath = filePoly;
      } else {
        log.info("Select the Polygons file");
        final Path polyPath =
            new FileOpen("Select A Polygon File", mapFolderLocation, ".txt").getFile();
        if (polyPath != null) {
          polygonsPath = polyPath;
        } else {
          log.info("You must specify a Polygon file.");
          log.info("Shutting down.");
          throw new IOException("no polygons file specified");
        }
      }
      return polygonsPath;
    }

    @Nonnull
    private Path getCentersFile(Path mapFolder) throws IOException {
      Path centersFile;
      final Path fileCenters =
          FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "centers.txt");
      if (Files.exists(fileCenters)
          && JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "A centers.txt file was found in the map's folder, do you want to use "
                      + "the file to supply the territories centers?",
                  "File Suggestion",
                  JOptionPane.YES_NO_CANCEL_OPTION)
              == 0) {
        centersFile = fileCenters;
      } else {
        log.info("Select the Centers file");
        final Path centerPath =
            new FileOpen("Select A Center File", mapFolderLocation, ".txt").getFile();
        if (centerPath != null) {
          centersFile = centerPath;
        } else {
          log.info("You must specify a centers file.");
          log.info("Shutting down.");
          throw new IOException("no centers file specified");
        }
      }
      return centersFile;
    }

    @Override
    protected JPanel createMainPanel() {
      return new JPanel() {
        private static final long serialVersionUID = -7130828419508975924L;

        @Override
        public void paint(final Graphics g) {
          paintToG(g);
        }
      };
    }

    private void paintToG(final Graphics g) {
      if (cheapMutex) {
        return;
      }
      g.drawImage(image, 0, 0, this);
      g.setColor(Color.red);
      for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
        for (final Point p : entry.getValue().getSecond()) {
          g.drawImage(
              entry.getValue().getFirst(),
              p.x,
              p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)),
              null);
          if (currentSelectedImage != null && currentSelectedImage.getThird().equals(p)) {
            g.setColor(Color.green);
            g.drawRect(
                p.x,
                p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)),
                entry.getValue().getFirst().getWidth(null),
                entry.getValue().getFirst().getHeight(null));
            g.setColor(Color.red);
          } else if (highlightAll) {
            g.drawRect(
                p.x,
                p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)),
                entry.getValue().getFirst().getWidth(null),
                entry.getValue().getFirst().getHeight(null));
          }
          if (showPointNames) {
            g.drawString(
                entry.getKey(),
                p.x,
                p.y - (showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)));
          }
        }
      }
      if (currentSelectedImage != null) {
        g.setColor(Color.green);
        g.drawImage(
            currentSelectedImage.getSecond(),
            currentMousePoint.x,
            currentMousePoint.y
                - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)),
            null);
        if (highlightAll) {
          g.drawRect(
              currentMousePoint.x,
              currentMousePoint.y
                  - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)),
              currentSelectedImage.getSecond().getWidth(null),
              currentSelectedImage.getSecond().getHeight(null));
        }
        if (showPointNames) {
          g.drawString(
              currentSelectedImage.getFirst(),
              currentMousePoint.x,
              currentMousePoint.y
                  - (showFromTopLeft ? 0 : currentSelectedImage.getSecond().getHeight(null)));
        }
      }
    }

    private void saveCurrentToMapPicture() {
      final BufferedImage bufferedImage =
          new BufferedImage(
              image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
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

    /** Saves the centers to disk. */
    private void saveImagePoints() {
      currentPoints = new HashMap<>();
      for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
        // remove duplicates
        final LinkedHashSet<Point> pointSet = new LinkedHashSet<>(entry.getValue().getSecond());
        entry.getValue().getSecond().clear();
        entry.getValue().getSecond().addAll(pointSet);
        currentPoints.put(entry.getKey(), entry.getValue().getSecond());
      }
      final Path fileName =
          new FileSave(
                  "Where To Save Image Points Text File?",
                  JFileChooser.FILES_ONLY,
                  currentImagePointsTextFile,
                  mapFolderLocation)
              .getFile();
      if (fileName == null) {
        return;
      }
      try {
        PointFileReaderWriter.writeOneToMany(fileName, currentPoints);
        log.info("Data written to: {}", fileName.normalize().toAbsolutePath());
      } catch (final IOException e) {
        log.error("Failed to save points: {}", fileName, e);
      }
    }

    private void selectImagePointType() {
      log.info("Select Which type of image points file are we making?");
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.add(new JLabel("Which type of image points file are we making?"));
      final ButtonGroup group = new ButtonGroup();
      for (final ImagePointType type : ImagePointType.getTypes()) {
        final JRadioButton button =
            new JRadioButton(type.toString() + "      :      " + type.getDescription());
        button.setActionCommand(type.toString());
        button.setSelected(imagePointType == type);
        group.add(button);
        panel.add(button);
      }
      JOptionPane.showMessageDialog(
          this,
          panel,
          "Which type of image points file are we making?",
          JOptionPane.QUESTION_MESSAGE);
      final ButtonModel selected = group.getSelection();
      final String choice = selected.getActionCommand();
      for (final ImagePointType type : ImagePointType.getTypes()) {
        if (type.toString().equals(choice)) {
          imagePointType = type;
          log.info("Selected Type: {}", choice);
          break;
        }
      }
    }

    private void topLeftOrBottomLeft() {
      log.info("Select Show images from top left or bottom left point?");
      final Object[] options = {"Point is Top Left", "Point is Bottom Left"};
      showFromTopLeft =
          JOptionPane.showOptionDialog(
                  this,
                  """
                  Are the images shown from the top left, or from the bottom left point? \r
                  All images are shown from the top left, except for 'name_place.txt', \
                  'pu_place.txt', and 'comments.txt'. \r
                  For these 3 files, whether they are top left or bottom left is determined \
                  by the \r
                  'map.properties' property: 'map.drawNamesFromTopLeft', which defaults to \
                  false if not specified [meaning bottom left]. \r
                  Do NOT change this from whatever the default has chosen, unless you know \
                  exactly what you are doing!""",
                  "Show images from top left or bottom left point?",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[(imagePointType.isCanUseBottomLeftPoint() ? 1 : 0)])
              != JOptionPane.NO_OPTION;
    }

    private void loadImagesAndPoints() {
      cheapMutex = true;
      currentImagePoints = new HashMap<>();
      currentSelectedImage = null;
      selectImagePointType();
      log.info("Select Folder full of images OR Text file full of points?");
      final Object[] miscOrNamesOptions = {"Folder Full of Images", "Text File Full of Points"};
      if (JOptionPane.showOptionDialog(
              this,
              "Are you doing a folder full of different images (decorations.txt [misc] "
                  + "and name_place.txt [territoryNames])\r\n"
                  + "Or are we doing a per territory static or dynamic image based on game data "
                  + "(pu_place.txt [PUs], capitols.txt [flags], etc, basically all others) ?",
              "Folder full of images OR Text file full of points?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              miscOrNamesOptions,
              miscOrNamesOptions[(imagePointType.isUseFolder() ? 0 : 1)])
          != JOptionPane.NO_OPTION) {
        // points are 'territory name' as opposed to exactly an image file name, like 'territory
        // name.png' (everything
        // but decorations is a territory name, while decorations are image file names)
        loadImageFolder();
        if (currentImageFolderLocation == null) {
          return;
        }
        loadImagePointTextFile();
        topLeftOrBottomLeft();
        // decorations.txt (misc folder) and name_place.txt (territoryNames folder) use a different
        // image for each
        // point, while everything else (like pu_place.txt (PUs folder)) will use either a static
        // image or a dynamically
        // chosen image based on some in game property in the game data.
        log.info("Points end in .png OR they do not?");
        final Object[] pointsAreNamesOptions = {"Points end in .png", "Points do NOT end in .png"};
        fillCurrentImagePointsBasedOnImageFolder(
            JOptionPane.showOptionDialog(
                    this,
                    "Does the text file use the exact image file name, including the "
                        + ".png extension (decorations.txt) \r\n"
                        + "Or does the text file not use the full file name with no extension, "
                        + "just a territory name (name_place.txt) ?",
                    "Points end in .png OR they do not?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    pointsAreNamesOptions,
                    pointsAreNamesOptions[(imagePointType.isEndInPng() ? 0 : 1)])
                == JOptionPane.NO_OPTION);
        createNewImageOnRightClick = false;
        staticImageForPlacing = null;
      } else {
        loadImagePointTextFile();
        topLeftOrBottomLeft();
        // load all territories? things like pu_place.txt should have all or most territories,
        // while things like blockade.txt and kamikaze_place.txt and capitols.txt will only
        // have a small number of territories
        log.info("Select Fill in all territories OR let you select them?");
        final Object[] fillAllOptions = {"Fill In All Territories", "Let Me Select Territories"};
        fillCurrentImagePointsBasedOnTextFile(
            JOptionPane.showOptionDialog(
                    this,
                    """
                    Are you going to do a point for every single territory (pu_place.txt) \r
                    Or are you going to do just a few territories (capitols.txt, \
                    convoy.txt, vc.txt, etc, most others)?\r
                    (If you choose the later option, you must Right Click on a territory \
                    to create an image for that territory.)""",
                    "Fill in all territories OR let you select them?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    fillAllOptions,
                    fillAllOptions[(imagePointType.isFillAll() ? 0 : 1)])
                != JOptionPane.NO_OPTION);
      }
      cheapMutex = false;
      repaint();
      JOptionPane.showMessageDialog(this, new JLabel(imagePointType.getInstructions()));
    }

    private void loadImageFolder() {
      log.info("Load an image folder (eg: 'misc' or 'territoryNames', etc)");
      Path folder = mapFolderLocation.resolve(imagePointType.getFolderName());
      if (!Files.exists(folder)) {
        folder = mapFolderLocation;
      }
      final FileSave imageFolder = new FileSave("Load an Image Folder", null, folder);
      if (imageFolder.getFile() == null || !Files.exists(imageFolder.getFile())) {
        currentImageFolderLocation = null;
      } else {
        currentImageFolderLocation = imageFolder.getFile();
      }
    }

    private void loadImagePointTextFile() {
      log.info("Load the points text file (eg: decorations.txt or pu_place.txt, etc)");
      final FileOpen centerName =
          new FileOpen(
              "Load an Image Points Text File",
              mapFolderLocation,
              mapFolderLocation.resolve(imagePointType.getFileName()),
              ".txt");
      currentImagePointsTextFile = centerName.getFile();
      if (centerName.getFile() != null && Files.exists(centerName.getFile())) {
        try {
          currentPoints = PointFileReaderWriter.readOneToMany(centerName.getFile());
        } catch (final IOException e) {
          log.error("Failed to load image points: {}", centerName.getFile(), e);
          currentPoints = new HashMap<>();
        }
      } else {
        currentPoints = new HashMap<>();
      }
    }

    private void fillCurrentImagePointsBasedOnTextFile(final boolean fillInAllTerritories) {
      staticImageForPlacing = null;
      Path imagePath =
          mapFolderLocation
              .resolve(imagePointType.getFolderName())
              .resolve(imagePointType.getImageName());
      if (!Files.exists(imagePath)) {
        imagePath = null;
      }
      while (staticImageForPlacing == null) {
        final FileOpen imageSelection =
            new FileOpen(
                "Select Example Image To Use",
                imagePath == null ? mapFolderLocation : imagePath.getParent(),
                imagePath,
                ".gif",
                ".png");
        if (imageSelection.getFile() == null || !Files.exists(imageSelection.getFile())) {
          continue;
        }
        staticImageForPlacing = FileHelper.newImage(imageSelection.getFile());
      }
      final int width = staticImageForPlacing.getWidth(null);
      final int height = staticImageForPlacing.getHeight(null);
      final int addY =
          (imagePointType == ImagePointType.COMMENTS
              ? -ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS
              : (imagePointType == ImagePointType.PU_PLACE
                  ? ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS
                  : 0));
      if (fillInAllTerritories) {
        for (final Entry<String, Point> entry : centers.entrySet()) {
          List<Point> points = currentPoints.get(entry.getKey());
          if (points == null) {
            log.info("Did NOT find point for: {}", entry.getKey());
            points = new ArrayList<>();
            final Point p =
                new Point(
                    entry.getValue().x - (width / 2),
                    entry.getValue().y + addY + ((showFromTopLeft ? -1 : 1) * (height / 2)));
            points.add(p);
          } else {
            log.info("Found point for: {}", entry.getKey());
          }
          currentImagePoints.put(entry.getKey(), Tuple.of(staticImageForPlacing, points));
        }
      } else {
        for (final Entry<String, List<Point>> entry : currentPoints.entrySet()) {
          currentImagePoints.put(entry.getKey(), Tuple.of(staticImageForPlacing, entry.getValue()));
        }
      }
      createNewImageOnRightClick = true;
    }

    private void fillCurrentImagePointsBasedOnImageFolder(
        final boolean pointsAreExactlyTerritoryNames) {
      final int addY =
          (imagePointType == ImagePointType.COMMENTS
              ? -ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS
              : (imagePointType == ImagePointType.PU_PLACE
                  ? ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS
                  : 0));
      final List<String> allTerritories = new ArrayList<>(centers.keySet());
      for (final Path path : FileUtils.listFiles(currentImageFolderLocation)) {
        if (!path.toString().endsWith(".png") && !path.toString().endsWith(".gif")) {
          continue;
        }
        final String imageName = path.getFileName().toString();
        final String possibleTerritoryName = imageName.substring(0, imageName.length() - 4);
        final Image territoryImage = FileHelper.newImage(path);
        List<Point> points =
            (currentPoints != null
                ? currentPoints.get(
                    (pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName))
                : null);
        if (points == null) {
          points = new ArrayList<>();
          Point p = centers.get(possibleTerritoryName);
          if (p == null) {
            log.info("Did NOT find point for: {}", possibleTerritoryName);
            points.add(new Point(50, 50));
          } else {
            p =
                new Point(
                    p.x - (territoryImage.getWidth(null) / 2),
                    p.y
                        + addY
                        + ((showFromTopLeft ? -1 : 1) * (territoryImage.getHeight(null) / 2)));
            points.add(p);
            allTerritories.remove(possibleTerritoryName);
            log.info("Found point for: {}", possibleTerritoryName);
          }
        } else {
          allTerritories.remove(possibleTerritoryName);
        }
        currentImagePoints.put(
            (pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName),
            Tuple.of(territoryImage, points));
      }
      if (!allTerritories.isEmpty() && imagePointType == ImagePointType.NAME_PLACE) {
        JOptionPane.showMessageDialog(
            this, new JLabel("Territory images not found in folder: " + allTerritories));
        log.info("Territory images not found in folder: {}", allTerritories);
      }
    }

    private void mouseEvent(final boolean ctrlDown, final boolean rightMouse) {
      if (cheapMutex) {
        return;
      }
      if (!rightMouse && !ctrlDown && currentSelectedImage == null) {
        // find whatever image we are left-clicking on
        Point testPoint = null;
        for (final Entry<String, Tuple<Image, List<Point>>> entry : currentImagePoints.entrySet()) {
          for (final Point p : entry.getValue().getSecond()) {
            if (testPoint == null
                || p.distance(currentMousePoint) < testPoint.distance(currentMousePoint)) {
              testPoint = p;
              currentSelectedImage = Triple.of(entry.getKey(), entry.getValue().getFirst(), p);
            }
          }
        }
      } else if (!rightMouse && !ctrlDown) {
        // save the image
        final Tuple<Image, List<Point>> imagePoints =
            currentImagePoints.get(currentSelectedImage.getFirst());
        final List<Point> points = imagePoints.getSecond();
        points.remove(currentSelectedImage.getThird());
        points.add(new Point(currentMousePoint));
        currentImagePoints.put(
            currentSelectedImage.getFirst(), Tuple.of(currentSelectedImage.getSecond(), points));
        currentSelectedImage = null;
      } else if (rightMouse
          && !ctrlDown
          && createNewImageOnRightClick
          && staticImageForPlacing != null
          && currentSelectedImage == null) {
        // create a new point here in this territory
        final Optional<String> territoryName =
            ToolsUtil.findTerritoryName(currentMousePoint, polygons);
        if (territoryName.isPresent()) {
          final List<Point> points = new ArrayList<>();
          points.add(new Point(currentMousePoint));
          currentImagePoints.put(territoryName.get(), Tuple.of(staticImageForPlacing, points));
        }
      } else if (rightMouse && !ctrlDown && imagePointType.isCanHaveMultiplePoints()) {
        // if none selected find the image we are clicking on, and duplicate it (not replace/move
        // it)
        if (currentSelectedImage == null) {
          Point testPoint = null;
          for (final Entry<String, Tuple<Image, List<Point>>> entry :
              currentImagePoints.entrySet()) {
            for (final Point p : entry.getValue().getSecond()) {
              if (testPoint == null
                  || p.distance(currentMousePoint) < testPoint.distance(currentMousePoint)) {
                testPoint = p;
                currentSelectedImage = Triple.of(entry.getKey(), entry.getValue().getFirst(), null);
              }
            }
          }
        } else {
          currentSelectedImage =
              Triple.of(currentSelectedImage.getFirst(), currentSelectedImage.getSecond(), null);
        }
        // then save (same code as above for saving)
        final Tuple<Image, List<Point>> imagePoints =
            currentImagePoints.get(currentSelectedImage.getFirst());
        final List<Point> points = imagePoints.getSecond();
        points.remove(currentSelectedImage.getThird());
        points.add(new Point(currentMousePoint));
        currentImagePoints.put(
            currentSelectedImage.getFirst(), Tuple.of(currentSelectedImage.getSecond(), points));
        currentSelectedImage = null;
      } else if (rightMouse && ctrlDown) {
        // must be right click AND ctrl down to delete an image
        if (currentSelectedImage == null) {
          return;
        }
        final Tuple<Image, List<Point>> current =
            currentImagePoints.get(currentSelectedImage.getFirst());
        final List<Point> points = current.getSecond();
        points.remove(currentSelectedImage.getThird());
        if (points.isEmpty()) {
          currentImagePoints.remove(currentSelectedImage.getFirst());
        }
        currentSelectedImage = null;
      }
      repaint();
    }
  }
}
