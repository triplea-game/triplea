package tools.map.making.ui.properties;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.image.UnitImageFactory;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingAction;
import tools.image.FileSave;
import tools.util.ToolArguments;

@UtilityClass
public class MapPropertiesPanel {

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    addBlockToPanel(panel);
    addBlockToPanel(
        panel,
        new JScrollPane(
            new JTextAreaBuilder()
                .rows(12)
                .columns(10)
                .text(
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
                        + "map utilities.")
                .build()));
    addBlockToPanel(
        panel,
        new JLabel("Click button open up the readme file on how to make maps:"),
        new JButtonBuilder()
            .title("Start Tutorial  /  Show Help Document")
            .actionListener(() -> OpenFileUtility.openUrl(UrlConstants.MAP_MAKER_HELP))
            .build());
    addBlockToPanel(
        panel,
        new JLabel("Click button to select where your map folder is:"),
        new JButtonBuilder()
            .title("Select Map Folder")
            .actionListener(
                () ->
                    SwingAction.of(
                        "Select Map Folder",
                        e -> {
                          final Path mapFolder =
                              new FileSave("Where is your map's folder?", null, null).getFile();
                          if (mapFolder != null && Files.exists(mapFolder)) {
                            ToolArguments.MAP_FOLDER.setSystemProperty(mapFolder.toString());
                          }
                        }))
            .build());

    final JTextField unitZoomText = new JTextField("0.75");
    unitZoomText.setMaximumSize(new Dimension(100, 20));
    unitZoomText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            ToolArguments.UNIT_ZOOM.setSystemProperty(unitZoomText.getText());
            unitZoomText.setText(ToolArguments.UNIT_ZOOM.getSystemPropertyValuePlain());
          }
        });
    addBlockToPanel(
        panel,
        new JLabel("Set the unit scaling (unit image zoom): "),
        new JLabel("Choose one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5"),
        unitZoomText);
    final JTextField unitWidthText = new JTextField(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
    unitWidthText.setMaximumSize(new Dimension(100, 20));
    unitWidthText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            ToolArguments.UNIT_WIDTH.setSystemProperty(unitWidthText.getText());
            unitWidthText.setText(ToolArguments.UNIT_WIDTH.getSystemPropertyValuePlain());
          }
        });
    addBlockToPanel(panel, new JLabel("Set the width of the unit images: "), unitWidthText);
    final JTextField unitHeightText = new JTextField(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
    unitHeightText.setMaximumSize(new Dimension(100, 20));
    unitHeightText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {}

          @Override
          public void focusLost(final FocusEvent e) {
            ToolArguments.UNIT_HEIGHT.setSystemProperty(unitHeightText.getText());
            unitHeightText.setText(ToolArguments.UNIT_HEIGHT.getSystemPropertyValuePlain());
          }
        });
    addBlockToPanel(panel, new JLabel("Set the height of the unit images: "), unitHeightText);
    return panel;
  }

  private static void addBlockToPanel(JPanel panel, Component... components) {
    for (Component component : components) {
      panel.add(component);
    }
    panel.add(Box.createVerticalStrut(30));
  }
}
