package tools.map.making.ui.utilities;

import java.util.List;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import tools.image.TileImageReconstructor;
import tools.map.making.ImageShrinker;
import tools.map.making.ui.MapMakingPanelBuilder;

@UtilityClass
public class OptionalUtilitiesPanel {

  public JPanel build() {
    return new MapMakingPanelBuilder("Other or Optional Utilities:")
        .addButtons(
            List.of(
                new MapMakingPanelBuilder.ButtonSpec("Run the Image Shrinker", ImageShrinker::run),
                new MapMakingPanelBuilder.ButtonSpec(
                    "Run the Tile Image Reconstructor", TileImageReconstructor::run)))
        .build();
  }
}
