package tools.map.making.ui.skin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import tools.image.AutoPlacementFinder;
import tools.image.CenterPicker;
import tools.image.DecorationPlacer;
import tools.image.PolygonGrabber;
import tools.image.TileImageBreaker;
import tools.map.making.MapPropertiesMaker;
import tools.map.making.PlacementPicker;

@UtilityClass
public class MapSkinPanel {

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Map Skin Utilities:"));
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Map Properties Maker")
            .actionListener(MapPropertiesMaker::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Center Picker") //
            .actionListener(CenterPicker::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Polygon Grabber") //
            .actionListener(PolygonGrabber::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Automatic Placement Finder")
            .actionListener(AutoPlacementFinder::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Placement Picker")
            .actionListener(PlacementPicker::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Tile Image Breaker")
            .actionListener(TileImageBreaker::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Decoration Placer")
            .actionListener(DecorationPlacer::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    return panel;
  }
}
