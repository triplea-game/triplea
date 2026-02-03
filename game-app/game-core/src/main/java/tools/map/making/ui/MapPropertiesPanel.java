package tools.map.making.ui;

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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.JPanelBuilder;
import tools.util.FileSave;
import tools.util.ToolArguments;

@UtilityClass
public class MapPropertiesPanel {

  private static final int PANEL_LINE_SPACING = 23;

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    addBlockToPanel(panel);
    addBlockToPanel(
        panel,
        new JScrollPane(
            new JTextAreaBuilder()
                .rows(15)
                .columns(45)
                .text(
                    """
                        Welcome to Veqryn's map creator program for TripleA.\


                        Before you begin, go create a folder in your directory: \
                        Users\\yourname\\triplea\\maps\

                        Name the folder with a short name of your map, do not use any special characters \
                        in the name.\

                        Next, create 5 folders inside your map folder, with these names: \
                        flags, units, baseTiles, reliefTiles, games\

                        Then, create a text file and rename it "map.properties" or use one created by \
                        this utility.\


                        To start the Map Utilities, have a png image of your map with just the \
                        territory borders and nothing else. The borders must be in black (hex: 000000) and \
                        there should not be any anti-aliasing (smoothing) of the lines or edges that stick \
                        out.\

                        Create a small image of the map (approx 250 pixels wide) and name \
                        it "smallMap.jpeg".\

                        Put these in the map's root folder. You can now start the map maker by clicking \
                        and filling in the details below, before moving on to 'Step 2' and running the \
                        map utilities.""")
                .build()));
    addBlockToPanel(
        panel,
        new JButtonBuilder()
            .title("Start Tutorial  /  Show Help Document")
            .toolTip("Open up the readme file on how to make maps")
            .actionListener(
                () ->
                    OpenFileUtility.openUrl(
                        JOptionPane.getFrameForComponent(panel), UrlConstants.MAP_MAKER_HELP))
            .build());
    addBlockToPanel(
        panel,
        new JButtonBuilder()
            .title("Select Map Folder")
            .actionListener(
                () ->
                    SwingAction.of(
                        "Select Map Folder",
                        e -> {
                          final Path mapFolder =
                              new FileSave(
                                      "Where is your map's folder?",
                                      null,
                                      ToolArguments.getPropertyMapFolderPath().orElse(null))
                                  .getFile();
                          if (mapFolder != null && Files.exists(mapFolder)) {
                            ToolArguments.MAP_FOLDER.setSystemProperty(mapFolder.toString());
                          }
                        }))
            .build());
    addInputFieldToPanel(
        panel,
        "Unit scaling: ",
        "Choose a scaling factor for the unit image.\nExamples: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5",
        "0.75",
        ToolArguments.UNIT_ZOOM);
    addInputFieldToPanel(
        panel,
        "Unit width: ",
        "Set width of the unit image",
        Integer.toString(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE),
        ToolArguments.UNIT_WIDTH);
    addInputFieldToPanel(
        panel,
        "Unit height: ",
        "Set height of the unit image",
        Integer.toString(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE),
        ToolArguments.UNIT_HEIGHT);
    return panel;
  }

  private static void addInputFieldToPanel(
      JPanel panel, String label, String tooltip, String text, ToolArguments toolsArgument) {
    final JTextField toolsArgumentText = new JTextField(text);
    Dimension fieldDimension = new Dimension(45, 20);
    toolsArgumentText.setPreferredSize(fieldDimension);
    toolsArgumentText.setMinimumSize(fieldDimension);
    toolsArgumentText.setMaximumSize(fieldDimension);
    toolsArgumentText.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(final FocusEvent e) {
            /* interface method, but nothing to be done when gaining focus */
          }

          @Override
          public void focusLost(final FocusEvent e) {
            toolsArgument.setSystemProperty(toolsArgumentText.getText());
            toolsArgumentText.setText(toolsArgument.getSystemPropertyValuePlain());
          }
        });
    toolsArgumentText.setToolTipText(tooltip);
    JLabel textFieldLabel = new JLabel(label);
    Dimension labelDimension = new Dimension(80, 20);
    textFieldLabel.setPreferredSize(labelDimension);
    textFieldLabel.setMaximumSize(labelDimension);
    textFieldLabel.setMinimumSize(labelDimension);
    addBlockToPanel(
        panel,
        new JPanelBuilder()
            .boxLayoutHorizontal()
            .add(textFieldLabel)
            .add(toolsArgumentText)
            .build());
  }

  private static void addBlockToPanel(JPanel panel, JComponent... components) {
    for (JComponent component : components) {
      component.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(component);
    }
    panel.add(Box.createVerticalStrut(PANEL_LINE_SPACING));
  }
}
