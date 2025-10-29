package tools.map.making.ui.xml;

import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import tools.map.making.ConnectionFinder;
import tools.map.making.ui.MapMakingPanelBuilder;

@UtilityClass
public class XmlUtilitiesPanel {

  public JPanel build() {
    return new MapMakingPanelBuilder("Game XML Utilities:")
        .add(
            new JLabel(
                "Sorry but for now the only XML creator is Wisconsin's 'Part 2' of his map maker."))
        .addVerticalStructDefault()
        .addButtons(
            List.of(
                new MapMakingPanelBuilder.ButtonSpec(
                    "Run the Connection Finder", ConnectionFinder::run)))
        .build();
  }
}
