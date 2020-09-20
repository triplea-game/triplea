package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import tools.map.making.ui.properties.MapPropertiesPanel;
import tools.map.making.ui.skin.MapSkinPanel;
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

    final JPanel mainPanel = new JPanel();
    final JPanel sidePanel = new JPanel();
    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
    sidePanel.add(Box.createVerticalGlue());

    final JButton part1 = new JButton("Step 1: Map Properties");
    part1.addActionListener(
        SwingAction.of("Part 1", e -> swapContainerContents(mainPanel, panel1)));
    sidePanel.add(part1);
    part1.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part2 = new JButton("Step 2: Map Utilities");
    part2.addActionListener(
        SwingAction.of("Part 2", e -> swapContainerContents(mainPanel, panel2)));
    sidePanel.add(part2);
    part2.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part3 = new JButton("Step 3: Game XML");
    part3.addActionListener(
        SwingAction.of("Part 3", e -> swapContainerContents(mainPanel, panel3)));
    sidePanel.add(part3);
    part3.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    final JButton part4 = new JButton("Other: Optional Things");
    part4.addActionListener(
        SwingAction.of("Part 4", e -> swapContainerContents(mainPanel, panel4)));
    sidePanel.add(part4);
    part4.setAlignmentX(Component.CENTER_ALIGNMENT);
    sidePanel.add(Box.createVerticalGlue());

    // set up the menu actions
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(new JScrollPane(sidePanel), BorderLayout.WEST);
    frame.getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);

    // now set up the main screen
    mainPanel.add(panel1);
    frame.setVisible(true);
  }

  private void swapContainerContents(final Container container, final JPanel panel) {
    container.removeAll();
    container.add(panel);

    SwingAction.invokeNowOrLater(
        () -> {
          container.revalidate();
          container.repaint();
        });
  }
}
