package tools.map.making.ui.skin;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
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
    final JPanel panel2 = new JPanel();
    panel2.removeAll();
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.PAGE_AXIS));
    panel2.add(Box.createVerticalStrut(30));
    panel2.add(new JLabel("Map Skin Utilities:"));
    panel2.add(Box.createVerticalStrut(30));
    final JButton mapPropertiesMakerButton = new JButton("Run the Map Properties Maker");
    mapPropertiesMakerButton.addActionListener(
        SwingAction.of("Run the Map Properties Maker", e -> runUtility(MapPropertiesMaker::run)));
    panel2.add(mapPropertiesMakerButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton centerPickerButton = new JButton("Run the Center Picker");
    centerPickerButton.addActionListener(
        SwingAction.of("Run the Center Picker", e -> runUtility(CenterPicker::run)));
    panel2.add(centerPickerButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton polygonGrabberButton = new JButton("Run the Polygon Grabber");
    polygonGrabberButton.addActionListener(
        SwingAction.of("Run the Polygon Grabber", e -> runUtility(PolygonGrabber::run)));
    panel2.add(polygonGrabberButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton autoPlacerButton = new JButton("Run the Automatic Placement Finder");
    autoPlacerButton.addActionListener(
        SwingAction.of(
            "Run the Automatic Placement Finder", e -> runUtility(AutoPlacementFinder::run)));
    panel2.add(autoPlacerButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton placementPickerButton = new JButton("Run the Placement Picker");
    placementPickerButton.addActionListener(
        SwingAction.of("Run the Placement Picker", e -> runUtility(PlacementPicker::run)));
    panel2.add(placementPickerButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton tileBreakerButton = new JButton("Run the Tile Image Breaker");
    tileBreakerButton.addActionListener(
        SwingAction.of("Run the Tile Image Breaker", e -> runUtility(TileImageBreaker::run)));
    panel2.add(tileBreakerButton);
    panel2.add(Box.createVerticalStrut(30));
    final JButton decorationPlacerButton = new JButton("Run the Decoration Placer");
    decorationPlacerButton.addActionListener(
        SwingAction.of("Run the Decoration Placer", e -> runUtility(DecorationPlacer::run)));
    panel2.add(decorationPlacerButton);
    panel2.add(Box.createVerticalStrut(30));
    panel2.validate();
    return panel2;
  }
}
