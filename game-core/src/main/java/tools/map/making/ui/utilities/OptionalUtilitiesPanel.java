package tools.map.making.ui.utilities;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import tools.image.TileImageReconstructor;
import tools.map.making.ImageShrinker;

@UtilityClass
public class OptionalUtilitiesPanel {

  public JPanel build() {
    JPanel panel4 = new JPanel();
    panel4.removeAll();
    panel4.setLayout(new BoxLayout(panel4, BoxLayout.PAGE_AXIS));
    panel4.add(Box.createVerticalStrut(30));
    panel4.add(new JLabel("Other or Optional Utilities:"));
    panel4.add(Box.createVerticalStrut(30));
    final JButton imageShrinkerButton = new JButton("Run the Image Shrinker");
    imageShrinkerButton.addActionListener(
        SwingAction.of("Run the Image Shrinker", e -> runUtility(ImageShrinker::run)));
    panel4.add(imageShrinkerButton);
    panel4.add(Box.createVerticalStrut(30));
    final JButton tileImageReconstructorButton = new JButton("Run the Tile Image Reconstructor");
    tileImageReconstructorButton.addActionListener(
        SwingAction.of(
            "Run the Tile Image Reconstructor", e -> runUtility(TileImageReconstructor::run)));
    panel4.add(tileImageReconstructorButton);
    panel4.add(Box.createVerticalStrut(30));
    panel4.validate();
    return panel4;
  }
}
