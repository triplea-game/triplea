package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import tools.map.making.ui.properties.MapPropertiesPanel;
import tools.map.making.ui.skin.MapSkinPanel;
import tools.map.making.ui.utilities.OptionalUtilitiesPanel;
import tools.map.making.ui.xml.XmlUtilitiesPanel;

/** A frame that will run the different map making utilities we have. */
@UtilityClass
public class MapCreator {

  public static void openMapCreatorWindow() {
    final JPanel mapPropertiesPanel = MapPropertiesPanel.build();

    final JPanel mainPanel = new JPanel();
    mainPanel.add(mapPropertiesPanel);

    final JPanel sidePanel = new JPanel();
    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
    sidePanel.add(Box.createVerticalGlue());

    // set up the menu actions
    addButtonToSidePanel(sidePanel, "Step 1: Map Properties", mainPanel, mapPropertiesPanel);
    addButtonToSidePanel(sidePanel, "Step 2: Map Utilities", mainPanel, MapSkinPanel.build());
    addButtonToSidePanel(sidePanel, "Step 3: Game XML", mainPanel, XmlUtilitiesPanel.build());
    addButtonToSidePanel(
        sidePanel, "Other: Optional Things", mainPanel, OptionalUtilitiesPanel.build());

    final JFrame frame =
        new JFrameBuilder()
            .title("TripleA Map Creator Tools")
            .size(800, 600)
            .locateRelativeTo(null)
            .disposeOnClose()
            .layout(new BorderLayout())
            .build();

    final Container contentPane = frame.getContentPane();
    contentPane.add(new JScrollPane(sidePanel), BorderLayout.WEST);
    contentPane.add(new JScrollPane(mainPanel), BorderLayout.CENTER);

    frame.setVisible(true);
  }

  private static void addButtonToSidePanel(
      JPanel sidePanel, String labelText, JPanel mainPanel, JPanel panel) {
    JButton button =
        new JButtonBuilder(labelText)
            .actionListener(() -> swapContainerContents(mainPanel, panel))
            .build();
    Dimension buttonDimension = new Dimension(175, 20);
    button.setPreferredSize(buttonDimension);
    button.setMinimumSize(buttonDimension);
    button.setMaximumSize(buttonDimension);
    sidePanel.add(button);
    sidePanel.add(Box.createVerticalGlue());
  }

  private void swapContainerContents(final Container container, final JPanel panel) {
    container.removeAll();
    container.add(panel);
    SwingAction.invokeNowOrLater(() -> SwingComponents.redraw(container));
  }
}
