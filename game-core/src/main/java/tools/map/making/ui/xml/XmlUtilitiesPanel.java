package tools.map.making.ui.xml;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import tools.map.making.ConnectionFinder;

@UtilityClass
public class XmlUtilitiesPanel {

  public JPanel build() {
    final JPanel panel3 = new JPanel():
    panel3.removeAll();
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.PAGE_AXIS));
    panel3.add(Box.createVerticalStrut(30));
    panel3.add(new JLabel("Game XML Utilities:"));
    panel3.add(
        new JLabel(
            "Sorry but for now the only XML creator is Wisconsin's 'Part 2' of his map maker."));
    panel3.add(Box.createVerticalStrut(30));
    final JButton connectionFinderButton = new JButton("Run the Connection Finder");
    connectionFinderButton.addActionListener(
        SwingAction.of("Run the Connection Finder", e -> runUtility(ConnectionFinder::run)));
    panel3.add(connectionFinderButton);
    panel3.add(Box.createVerticalStrut(30));
    panel3.validate();
    return panel3;
  }
}
