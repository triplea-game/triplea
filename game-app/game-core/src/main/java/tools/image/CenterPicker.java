package tools.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.SwingAction;
import org.triplea.util.PointFileReaderWriter;
import tools.util.ToolArguments;
import tools.util.ToolRunnableTask;
import tools.util.ToolsUtil;

/**
 * The center picker map-making tool.
 *
 * <p>This tool will allow you to manually specify center locations for each territory on a given
 * map. Center locations tell the game where to put things like flags, text, unit placements, etc.
 * It will generate a {@code centers.txt} file containing the territory center locations.
 */
@Slf4j
public final class CenterPicker extends ToolRunnableTask {
  private Path mapFolderLocation = null;

  private CenterPicker() {}

  public static void run() {
    runTask(TileImageBreaker.class);
  }

  @Override
  protected void runInternal() throws IOException {
    ToolArguments.ifMapFolder(mapFolderProperty -> mapFolderLocation = mapFolderProperty);
    log.info("Select the map");
    final FileOpen mapSelection = new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png");
    final Path map = mapSelection.getFile();
    if (mapFolderLocation == null && mapSelection.getFile() != null) {
      mapFolderLocation = mapSelection.getFile().getParent();
    }
    if (map != null) {
      log.info("Map : {}", map);
      final CenterPickerFrame frame = new CenterPickerFrame(map);
      frame.setVisible(true);
      JOptionPane.showMessageDialog(
          frame,
          new JLabel(
              "<html>"
                  + "This is the CenterPicker, it will create a centers.txt file for you. "
                  + "<br>Please click on the center of every single territory and sea zone on your "
                  + "map, and give each a name. "
                  + "<br>The point you clicked on will tell TripleA where to put things like any "
                  + "flags, text, unit placements, etc, "
                  + "<br>so be sure to click in the exact middle, or slight up and left of the "
                  + "middle, of each territory "
                  + "<br>(but still within the territory borders)."
                  + "<br>Do not use special or illegal characters in territory names."
                  + "<br><br>You can also load an existing centers.txt file, then make "
                  + "modifications to it, then save it again."
                  + "<br><br>LEFT CLICK = create a new center point for a territory/zone."
                  + "<br><br>RIGHT CLICK on an existing center = delete that center point."
                  + "<br><br>When finished, save the centers and exit."
                  + "</html>"));
    } else {
      log.info("No Image Map Selected. Shutting down.");
    }
  }

  private final class CenterPickerFrame extends MapEditorFrame {
    private static final long serialVersionUID = -5633998810385136625L;

    // hash map for center points
    private Map<String, Point> centers = new HashMap<>();
    // hash map for polygon points
    private final Map<String, List<Polygon>> polygons;

    CenterPickerFrame(final Path mapFolder) throws IOException {
      super("Center Picker", mapFolder);
      final Path polygonsPath = getPolygonsPath(mapFolder);
      try {
        log.info("Polygons : {}", polygonsPath);
        polygons = PointFileReaderWriter.readOneToManyPolygons(polygonsPath);
      } catch (final IOException e) {
        log.error("Something wrong with your Polygons file: {}", polygonsPath);
        throw e;
      }
    }

    @Override
    protected JPanel createMainPanel() {
      return new JPanel() {
        private static final long serialVersionUID = -7130828419508975924L;

        @Override
        public void paint(final Graphics g) {
          g.drawImage(image, 0, 0, this);
          g.setColor(Color.red);
          for (final Entry<String, Point> center : centers.entrySet()) {
            final Point item = center.getValue();
            g.fillOval(item.x, item.y, 15, 15);
            g.drawString(center.getKey(), item.x + 17, item.y + 13);
          }
        }
      };
    }

    @Override
    protected MouseAdapter getMouseClickedAdapter() {
      return new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          mouseEvent(e.getPoint(), SwingUtilities.isRightMouseButton(e));
        }
      };
    }

    @Override
    protected void initializeLayout() {
      // set up the actions
      final Action openAction = SwingAction.of("Load Centers", e -> loadCenters());
      openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Center Points File");
      final Action saveAction = SwingAction.of("Save Centers", e -> saveCenters());
      saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Center Points To File");
      final Action exitAction =
          SwingAction.of(
              "Exit",
              e -> {
                setVisible(false);
                dispose();
              });
      exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
      setupMenuBar(openAction, saveAction, exitAction);
    }

    private void setupMenuBar(Action openAction, Action saveAction, Action exitAction) {
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
      fileMenu.setMnemonic(KeyEvent.VK_F);
      fileMenu.add(openItem);
      fileMenu.add(saveItem);
      fileMenu.addSeparator();
      fileMenu.add(exitItem);
      menuBar.add(fileMenu);
    }

    @Nullable
    private Path getPolygonsPath(Path mapFolder) {
      final Path file = FileHelper.getFileInMapRoot(mapFolderLocation, mapFolder, "polygons.txt");
      Path polygonsPath;
      if (Files.exists(file)
          && JOptionPane.showConfirmDialog(
                  new JPanel(),
                  "A polygons.txt file was found in the map's folder, do you want to use "
                      + "the file to supply the territories names?",
                  "File Suggestion",
                  JOptionPane.YES_NO_CANCEL_OPTION)
              == 0) {
        polygonsPath = file;
      } else {
        polygonsPath = new FileOpen("Select A Polygon File", mapFolderLocation, ".txt").getFile();
      }
      return polygonsPath;
    }

    /** Saves the centers to disk. */
    private void saveCenters() {
      final Path fileName =
          new FileSave("Where To Save centers.txt ?", "centers.txt", mapFolderLocation).getFile();
      if (fileName == null) {
        return;
      }
      try {
        PointFileReaderWriter.writeOneToOne(fileName, centers);
        log.info("Data written to: {}", fileName.normalize().toAbsolutePath());
      } catch (final IOException e) {
        log.error("Failed to save centers: {}", fileName, e);
      }
    }

    /** Loads a pre-defined file with map center points. */
    private void loadCenters() {
      log.info("Load a center file");
      final Path centerName =
          new FileOpen("Load A Center File", mapFolderLocation, ".txt").getFile();
      if (centerName == null) {
        return;
      }
      try {
        centers = PointFileReaderWriter.readOneToOne(centerName);
      } catch (final IOException e) {
        log.error("Failed to load centers: {}", centerName, e);
      }
      repaint();
    }

    /**
     * Finds a land territory name or some sea zone name.
     *
     * @param p A point on the map.
     */
    private String findTerritoryName(final Point p) {
      return ToolsUtil.findTerritoryName(p, polygons).orElse("unknown");
    }

    private void mouseEvent(final Point point, final boolean rightMouse) {
      if (!rightMouse) {
        String name = findTerritoryName(point);
        name = JOptionPane.showInputDialog(this, "Enter the territory name:", name);
        if (name == null || name.isBlank()) {
          return;
        }
        if (centers.containsKey(name)
            && JOptionPane.showConfirmDialog(
                    this,
                    "Another center exists with the same name. "
                        + "Are you sure you want to replace it with this one?")
                != 0) {
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
        if (centerClicked != null
            && JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this center?")
                == 0) {
          centers.remove(centerClicked);
        }
      }
      repaint();
    }
  }
}
