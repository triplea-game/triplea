package tools.map.making.ui.utilities;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import tools.image.TileImageReconstructor;
import tools.map.making.ImageShrinker;

@UtilityClass
public class OptionalUtilitiesPanel {

  public JPanel build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(Box.createVerticalStrut(30));
    panel.add(new JLabel("Other or Optional Utilities:"));
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Image Shrinker") //
            .actionListener(ImageShrinker::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    panel.add(
        new JButtonBuilder("Run the Tile Image Reconstructor") //
            .actionListener(TileImageReconstructor::run)
            .build());
    panel.add(Box.createVerticalStrut(30));
    return panel;
  }
}
