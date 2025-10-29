package tools.map.making.ui.skin;

import java.util.List;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import tools.image.AutoPlacementFinder;
import tools.image.CenterPicker;
import tools.image.DecorationPlacer;
import tools.image.TileImageBreaker;
import tools.map.making.MapPropertiesMaker;
import tools.map.making.PlacementPicker;
import tools.map.making.ui.MapMakingPanelBuilder;
import tools.map.making.ui.MapMakingPanelBuilder.ButtonSpec;

@UtilityClass
public class MapSkinPanel {

  public static JPanel build() {
    return new MapMakingPanelBuilder("Map Skin Utilities:")
        .addButtons(
            List.of(
                new ButtonSpec("Run the Map Properties Maker", MapPropertiesMaker::run),
                new ButtonSpec("Run the Center Picker", CenterPicker::run),
                new ButtonSpec("Run the Automatic Placement Finder", AutoPlacementFinder::run),
                new ButtonSpec("Run the Placement Picker", PlacementPicker::run),
                new ButtonSpec("Run the Tile Image Breaker", TileImageBreaker::run),
                new ButtonSpec("Run the Decoration Placer", DecorationPlacer::run)))
        .build();
  }
}
