package tools.map.making.ui.properties;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.image.UnitImageFactory;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private static int unitWidth = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
  private static int unitHeight = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(30));
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
    panel.add(scrollText);
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Click button open up the readme file on how to make maps:"));
    final JButton helpButton = new JButton("Start Tutorial  /  Show Help Document");
    helpButton.addActionListener(e -> OpenFileUtility.openUrl(UrlConstants.MAP_MAKER_HELP));
    panel.add(helpButton);
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Click button to select where your map folder is:"));
    final JButton mapFolderButton = new JButton("Select Map Folder");
    mapFolderButton.addActionListener(
        SwingAction.of(
            "Select Map Folder",
            e -> {
              final Path mapFolder =
                  new FileSave("Where is your map's folder?", null, null).getFile();
              if (mapFolder != null && Files.exists(mapFolder)) {
                ToolArguments.MAP_FOLDER.setSystemProperty(mapFolder.toString());
              }
            }));
    panel.add(mapFolderButton);
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Set the unit scaling (unit image zoom): "));
    panel.add(new JLabel("Choose one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5"));

    final JTextField unitZoomText = new JTextField("0.75");
    unitZoomText.setMaximumSize(new Dimension(100, 20));
    unitZoomText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            try {
              final double unitZoom =
                  Math.min(4.0, Math.max(0.1, Double.parseDouble(unitZoomText.getText())));
              ToolArguments.UNIT_ZOOM.setSystemProperty(unitZoom);
              unitZoomText.setText(String.valueOf(unitZoom));
            } catch (final NumberFormatException ex) {
              // ignore malformed input
              unitZoomText.setText("");
            }
          }
        });
    panel.add(unitZoomText);
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Set the width of the unit images: "));
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
              ToolArguments.UNIT_WIDTH.setSystemProperty(unitWidth);
            } catch (final Exception ex) {
              // ignore malformed input
            }
            unitWidthText.setText("" + unitWidth);
          }
        });
    panel.add(unitWidthText);
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Set the height of the unit images: "));
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
              ToolArguments.UNIT_HEIGHT.setSystemProperty(unitHeight);
            } catch (final Exception ex) {
              // ignore malformed input
            }
            unitHeightText.setText("" + unitHeight);
          }
        });
    panel.add(unitHeightText);
    panel.add(Box.createVerticalStrut(30));
    return panel;
  }
}
