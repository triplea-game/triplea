package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;
import tools.map.making.ui.properties.MapPropertiesPanel;
import tools.map.making.ui.skin.MapSkinPanel;
import tools.map.making.ui.upload.UploadMapPanel;
import tools.map.making.ui.utilities.OptionalUtilitiesPanel;
import tools.map.making.ui.xml.XmlUtilitiesPanel;

/** A frame that will run the different map making utilities we have. */
public class MapCreator extends JFrame {
  private static final long serialVersionUID = 3593102638082774498L;

  private final JPanel mainPanel;
  private final JPanel panel1 = MapPropertiesPanel.build();
  private final JPanel panel2 = MapSkinPanel.build();
  private final JPanel panel3 = XmlUtilitiesPanel.build();
  private final JPanel panel4 = OptionalUtilitiesPanel.build();

  private MapCreator() {
    super("TripleA Map Creator Tools");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    // components
    mainPanel = new JPanel();
    final JPanel sidePanel = new JPanel();
    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
    sidePanel.add(Box.createVerticalGlue());

    final JButton part1 = new JButton("Step 1: Map Properties");
    part1.addActionListener(SwingAction.of("Part 1", e -> setupMainPanel(panel1)));
    sidePanel.add(part1);
    part1.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part2 = new JButton("Step 2: Map Utilities");
    part2.addActionListener(SwingAction.of("Part 2", e -> setupMainPanel(panel2)));
    sidePanel.add(part2);
    part2.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part3 = new JButton("Step 3: Game XML");
    part3.addActionListener(SwingAction.of("Part 3", e -> setupMainPanel(panel3)));
    sidePanel.add(part3);
    part3.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part4 = new JButton("Other: Optional Things");
    part4.addActionListener(SwingAction.of("Part 4", e -> setupMainPanel(panel4)));
    sidePanel.add(part4);
    part4.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    // set up the menu actions
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(sidePanel), BorderLayout.WEST);
    this.getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);
    // now set up the main screen
    setupMainPanel(panel1);
  }

  private void setupMainPanel(final JPanel panel) {
    mainPanel.removeAll();
    mainPanel.add(panel);

    SwingAction.invokeNowOrLater(
        () -> {
          mainPanel.revalidate();
          mainPanel.repaint();
        });
  }

  /** Opens a map creator window. */
  public static void openMapCreatorWindow() {
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  final MapCreator creator = new MapCreator();
                  creator.setSize(800, 600);
                  creator.setLocationRelativeTo(null);
                  creator.setVisible(true);
                }));
  }
}
