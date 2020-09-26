package tools.map.making;

import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JButtonBuilder.AlignmentX;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import tools.map.making.ui.properties.MapPropertiesPanel;
import tools.map.making.ui.skin.MapSkinPanel;
import tools.map.making.ui.upload.UploadMapPanel;
import tools.map.making.ui.utilities.OptionalUtilitiesPanel;
import tools.map.making.ui.xml.XmlUtilitiesPanel;

/** A frame that will run the different map making utilities we have. */
@UtilityClass
public class MapCreator {

  public static void openMapCreatorWindow() {
    final JFrame frame = new JFrame("TripleA Map Creator Tools");

    frame.setSize(800, 600);
    frame.setLocationRelativeTo(null);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final JPanel panel1 = MapPropertiesPanel.build();
    final JPanel panel2 = MapSkinPanel.build();
    final JPanel panel3 = XmlUtilitiesPanel.build();
    final JPanel panel4 = OptionalUtilitiesPanel.build();
    final JPanel panel5 = UploadMapPanel.build(frame);

    final JPanel mainPanel = new JPanel();
    mainPanel.add(panel1);

    final JPanel sidePanel = new JPanel();
    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
    sidePanel.add(Box.createVerticalGlue());

    sidePanel.add(
        new JButtonBuilder("Step 1: Map Properties")
            .actionListener(() -> swapContainerContents(mainPanel, panel1))
            .alignmentX(AlignmentX.CENTER)
            .build());
    sidePanel.add(Box.createVerticalGlue());

    sidePanel.add(
        new JButtonBuilder("Step 2: Map Utilities")
            .actionListener(() -> swapContainerContents(mainPanel, panel2))
            .alignmentX(AlignmentX.CENTER)
            .build());
    sidePanel.add(Box.createVerticalGlue());

    sidePanel.add(
        new JButtonBuilder("Step 3: Game XML")
            .actionListener(() -> swapContainerContents(mainPanel, panel3))
            .alignmentX(AlignmentX.CENTER)
            .build());
    sidePanel.add(Box.createVerticalGlue());

    sidePanel.add(
        new JButtonBuilder("Other: Optional Things")
            .actionListener(() -> swapContainerContents(mainPanel, panel4))
            .alignmentX(AlignmentX.CENTER)
            .build());
    sidePanel.add(Box.createVerticalGlue());

    if (ClientSetting.showBetaFeatures.getValueOrThrow()) {
      sidePanel.add(
          new JButtonBuilder("Map Upload")
              .actionListener(() -> swapContainerContents(mainPanel, panel5))
              .alignmentX(AlignmentX.CENTER)
              .build());
      sidePanel.add(Box.createVerticalGlue());
    }

    // set up the menu actions
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(new JScrollPane(sidePanel), BorderLayout.WEST);
    frame.getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);

    // now set up the main screen
    frame.setVisible(true);
  }

  private void swapContainerContents(final Container container, final JPanel panel) {
    container.removeAll();
    container.add(panel);

    SwingAction.invokeNowOrLater(() -> SwingComponents.redraw(container));
  }
}
