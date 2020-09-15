package tools.map.making.ui.properties;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.image.UnitImageFactory;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.SwingAction;
import tools.image.FileSave;
import tools.util.ToolArguments;

@UtilityClass
public class MapPropertiesPanel {
  private static long memoryInBytes = Runtime.getRuntime().maxMemory();
  private static File mapFolderLocation = null;
  private static double unitZoom = 0.75;
  private static int unitWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static int unitHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;

  public JPanel build() {
    final JPanel panel1 = new JPanel();
    panel1.removeAll();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
    panel1.add(Box.createVerticalStrut(30));
    final JTextArea text = new JTextArea(12, 10);
    text.setWrapStyleWord(true);
    text.setLineWrap(true);
    text.setText(
        "Welcome to Veqryn's map creator program for TripleA."
            + "\n\nBefore you begin, go create a folder in your directory: "
            + "Users\\yourname\\triplea\\maps"
            + "\nName the folder with a short name of your map, do not use any special characters "
            + "in the name."
            + "\nNext, create 5 folders inside your map folder, with these names: "
            + "flags, units, baseTiles, reliefTiles, games"
            + "\nThen, create a text file and rename it \"map.properties\" or use one created by "
            + "this utility."
            + "\n\nTo start the Map Utilities, have a png image of your map with just the "
            + "territory borders and nothing else. The borders must be in black (hex: 000000) and "
            + "there should not be any anti-aliasing (smoothing) of the lines or edges that stick "
            + "out."
            + "\nCreate a small image of the map (approx 250 pixels wide) and name "
            + "it \"smallMap.jpeg\"."
            + "\nPut these in the map's root folder. You can now start the map maker by clicking "
            + "and filling in the details below, before moving on to 'Step 2' and running the "
            + "map utilities.");
    final JScrollPane scrollText = new JScrollPane(text);
    panel1.add(scrollText);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(new JLabel("Click button open up the readme file on how to make maps:"));
    final JButton helpButton = new JButton("Start Tutorial  /  Show Help Document");
    helpButton.addActionListener(e -> OpenFileUtility.openUrl(UrlConstants.MAP_MAKER_HELP));
    panel1.add(helpButton);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(new JLabel("Click button to select where your map folder is:"));
    final JButton mapFolderButton = new JButton("Select Map Folder");
    mapFolderButton.addActionListener(
        SwingAction.of(
            "Select Map Folder",
            e -> {
              final String path =
                  new FileSave("Where is your map's folder?", null, mapFolderLocation)
                      .getPathString();
              if (path != null) {
                final File mapFolder = new File(path);
                if (mapFolder.exists()) {
                  mapFolderLocation = mapFolder;
                  System.setProperty(ToolArguments.MAP_FOLDER, mapFolderLocation.getPath());
                }
              }
            }));
    panel1.add(mapFolderButton);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(new JLabel("Set the unit scaling (unit image zoom): "));
    panel1.add(new JLabel("Choose one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5"));
    final JTextField unitZoomText = new JTextField("" + unitZoom);
    unitZoomText.setMaximumSize(new Dimension(100, 20));
    unitZoomText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            try {
              unitZoom = Math.min(4.0, Math.max(0.1, Double.parseDouble(unitZoomText.getText())));
              System.setProperty(ToolArguments.UNIT_ZOOM, "" + unitZoom);
            } catch (final Exception ex) {
              // ignore malformed input
            }
            unitZoomText.setText("" + unitZoom);
          }
        });
    panel1.add(unitZoomText);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(new JLabel("Set the width of the unit images: "));
    final JTextField unitWidthText = new JTextField("" + unitWidth);
    unitWidthText.setMaximumSize(new Dimension(100, 20));
    unitWidthText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            try {
              unitWidth = Math.min(400, Math.max(1, Integer.parseInt(unitWidthText.getText())));
              System.setProperty(ToolArguments.UNIT_WIDTH, "" + unitWidth);
            } catch (final Exception ex) {
              // ignore malformed input
            }
            unitWidthText.setText("" + unitWidth);
          }
        });
    panel1.add(unitWidthText);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(new JLabel("Set the height of the unit images: "));
    final JTextField unitHeightText = new JTextField("" + unitHeight);
    unitHeightText.setMaximumSize(new Dimension(100, 20));
    unitHeightText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            try {
              unitHeight = Math.min(400, Math.max(1, Integer.parseInt(unitHeightText.getText())));
              System.setProperty(ToolArguments.UNIT_HEIGHT, "" + unitHeight);
            } catch (final Exception ex) {
              // ignore malformed input
            }
            unitHeightText.setText("" + unitHeight);
          }
        });
    panel1.add(unitHeightText);
    panel1.add(Box.createVerticalStrut(30));
    panel1.add(
        new JLabel(
            "<html>Here you can set the 'max memory' that utilities like the Polygon "
                + "Grabber will use.<br>"
                + "This is useful is you have a very large map, or ever get any "
                + "Java Heap Space errors.</html>"));
    panel1.add(
        new JLabel(
            "Set the amount of memory to use when running new processes (in megabytes [mb]):"));
    final JTextField memoryText = new JTextField("" + (memoryInBytes / (1024 * 1024)));
    memoryText.setMaximumSize(new Dimension(100, 20));
    memoryText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            try {
              memoryInBytes =
                  (long) 1024
                      * 1024
                      * Math.min(4096, Math.max(256, Integer.parseInt(memoryText.getText())));
            } catch (final Exception ex) {
              // ignore malformed input
            }
            memoryText.setText("" + (memoryInBytes / (1024 * 1024)));
          }
        });
    panel1.add(memoryText);
    panel1.add(Box.createVerticalStrut(30));
    panel1.validate();
    return panel1;
  }
}
