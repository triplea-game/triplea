package tools.map.making.ui.panel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.gameparser.GameParser;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTextAreaBuilder;
import tools.image.FileOpen;
import tools.util.ToolArguments;

@UtilityClass
public class ValidateMapPanel {

  Path gameXmlPath = null;

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(30));
    JLabel mainLabel = new JLabel("Map Validator Tool:");
    //    mainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainLabel.getPreferredSize().height));

    panel.add(mainLabel);
    //    panel.add(Box.createHorizontalGlue()); // push label left
    panel.add(Box.createVerticalStrut(30));
    final JLabel gameXmlLabel = new JLabel("");
    final JTextArea parseResultText =
        new JTextAreaBuilder()
            .rows(15)
            .columns(45)
            .text("No game XML selected for validation.")
            .build();
    parseResultText.setAlignmentX(Component.LEFT_ALIGNMENT);
    parseResultText.setMaximumSize(
        new Dimension(Integer.MAX_VALUE, parseResultText.getPreferredSize().height));
    panel.add(
        new JButtonBuilder("Validate Game XML file")
            .toolTip("This starts an attempt to parse the Game XML file and provides the result")
            .actionListener(
                e -> {
                  gameXmlPath =
                      new FileOpen(
                              "Select game XML file",
                              (gameXmlPath != null
                                  ? gameXmlPath.getParent()
                                  : ToolArguments.getPropertyMapFolderPath().orElse(null)),
                              ".xml")
                          .getFile();
                  if (gameXmlPath != null) {
                    gameXmlLabel.setText(gameXmlPath.toString());
                    try {
                      Optional<GameData> optionalGameData = GameParser.parse(gameXmlPath, false);
                      if (optionalGameData.isPresent()) {
                        parseResultText.setText("✅ Map is valid");
                      } else {
                        parseResultText.setText("❌ Validation failed");
                      }
                    } catch (InvalidPathException ex) {
                      final String message =
                          "Error: "
                              + ex.getClass().getSimpleName()
                              + " — "
                              + (ex.getMessage() != null ? ex.getMessage() : "No details");
                      parseResultText.setText("❌ Validation failed: %s".formatted(message));
                    }
                  } else {
                    parseResultText.setText("No game XML selected for validation.");
                  }
                  SwingUtilities.invokeLater(panel::repaint);
                })
            .build());
    //    panel.add(Box.createHorizontalGlue()); // push label left
    panel.add(Box.createVerticalStrut(30));
    panel.add(gameXmlLabel);
    panel.add(Box.createVerticalStrut(30));
    panel.add(parseResultText);
    panel.add(Box.createVerticalStrut(30));
    return panel;
  }
}
