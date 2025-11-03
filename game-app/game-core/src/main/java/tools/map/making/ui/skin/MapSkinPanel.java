package tools.map.making.ui.skin;

import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import tools.image.AutoPlacementFinder;
import tools.image.CenterPicker;
import tools.image.DecorationPlacer;
import tools.image.TileImageBreaker;
import tools.map.making.MapPropertiesMaker;
import tools.map.making.PlacementPicker;

@UtilityClass
public class MapSkinPanel {

  private static final int SPACING_HEIGHT = 30;

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
    panel.add(new JLabel("Map Skin Utilities:"));
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));

    List.of(
            new ButtonSpec("Run the Map Properties Maker", MapPropertiesMaker::run),
            new ButtonSpec("Run the Center Picker", CenterPicker::run),
            new ButtonSpec("Run the Automatic Placement Finder", AutoPlacementFinder::run),
            new ButtonSpec("Run the Placement Picker", PlacementPicker::run),
            new ButtonSpec("Run the Tile Image Breaker", TileImageBreaker::run),
            new ButtonSpec("Run the Decoration Placer", DecorationPlacer::run))
        .forEach(buttonSpec -> addButtonToPanel(panel, buttonSpec));
    return panel;
  }

  private record ButtonSpec(String labelText, Runnable runnable) {}

  private static void addButtonToPanel(JPanel panel, ButtonSpec buttonSpec) {
    panel.add(new JButtonBuilder(buttonSpec.labelText).actionListener(buttonSpec.runnable).build());
    panel.add(Box.createVerticalStrut(SPACING_HEIGHT));
  }
}
