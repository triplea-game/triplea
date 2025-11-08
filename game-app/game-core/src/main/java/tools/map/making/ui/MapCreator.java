package tools.map.making.ui;

import games.strategy.triplea.EngineImageLoader;
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
import tools.map.making.ui.panel.ValidateMapPanel;
import tools.map.making.ui.runnable.AutoPlacementFinderTask;
import tools.map.making.ui.runnable.CenterPickerTask;
import tools.map.making.ui.runnable.DecorationPlacerTask;
import tools.map.making.ui.runnable.TileImageBreakerTask;
import tools.map.making.ui.runnable.TileImageReconstructorTask;
import tools.map.making.ui.runnable.ImageShrinkerTask;
import tools.map.making.ui.runnable.MapPropertiesMakerTask;
import tools.map.making.ui.runnable.PlacementPickerTask;

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
    addButtonToSidePanel(sidePanel, "Step 2: Map Utilities", mainPanel, getMapSkinPanel());
    addButtonToSidePanel(sidePanel, "Step 3: Game XML", mainPanel, XmlUtilitiesPanel.build());
    addButtonToSidePanel(sidePanel, "Step 4: Validate Map", mainPanel, ValidateMapPanel.build());
    addButtonToSidePanel(
        sidePanel, "Other: Optional Things", mainPanel, getOptionalUtilitiesPanel());

    final JFrame frame =
        new JFrameBuilder()
            .title("TripleA Map Creator Tools")
            .iconImage(EngineImageLoader.loadFrameIcon())
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

  private static JPanel getOptionalUtilitiesPanel() {
    return MapMakingPanelFactory.get(
        "Other or Optional Utilities:",
        new MapMakingPanelFactory.ButtonSpec("Run the Image Shrinker", ImageShrinkerTask::run),
        new MapMakingPanelFactory.ButtonSpec(
            "Run the Tile Image Reconstructor", TileImageReconstructorTask::run));
  }

  private static JPanel getMapSkinPanel() {
    return MapMakingPanelFactory.get(
        "Map Skin Utilities:",
        new MapMakingPanelFactory.ButtonSpec(
            "Run the Map Properties Maker", MapPropertiesMakerTask::run),
        new MapMakingPanelFactory.ButtonSpec("Run the Center Picker", CenterPickerTask::run),
        new MapMakingPanelFactory.ButtonSpec(
            "Run the Automatic Placement Finder", AutoPlacementFinderTask::run),
        new MapMakingPanelFactory.ButtonSpec("Run the Placement Picker", PlacementPickerTask::run),
        new MapMakingPanelFactory.ButtonSpec("Run the Tile Image Breaker", TileImageBreakerTask::run),
        new MapMakingPanelFactory.ButtonSpec("Run the Decoration Placer", DecorationPlacerTask::run));
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
