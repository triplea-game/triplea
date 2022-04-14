package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Collection;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import org.triplea.swing.SwingComponents;

public class SelectTerritoryComponent extends JPanel {
  private static final long serialVersionUID = 3855054934860687832L;
  private final Territory battleLocation;
  private final MapPanel mapPanel;
  private final JList<Territory> list;
  private final JLabel label = new JLabel("Retreat to...");
  private final JLabel retreatTerritory = new JLabel("");

  SelectTerritoryComponent(
      final Territory battleLocation,
      final Collection<Territory> possible,
      final MapPanel mapPanel) {
    this.battleLocation = battleLocation;
    this.mapPanel = mapPanel;
    this.setLayout(new BorderLayout());
    label.setBorder(new EmptyBorder(0, 0, 10, 0));
    this.add(label, BorderLayout.NORTH);
    final JPanel imagePanel = new JPanel();
    imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    imagePanel.add(retreatTerritory);
    imagePanel.setBorder(new EmptyBorder(10, 10, 10, 0));
    this.add(imagePanel, BorderLayout.EAST);
    list = new JList<>(SwingComponents.newListModel(possible));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    if (!possible.isEmpty()) {
      list.setSelectedIndex(0);
    }
    final JScrollPane scroll = new JScrollPane(list);
    this.add(scroll, BorderLayout.CENTER);
    scroll.setBorder(new EmptyBorder(10, 0, 10, 0));
    updateImage();
    list.addListSelectionListener(e -> updateImage());
  }

  public void setLabelText(String text) {
    label.setText(text);
  }

  private void updateImage() {
    final int width = 250;
    final int height = 250;
    final Image img = mapPanel.getTerritoryImage(list.getSelectedValue(), battleLocation);
    final Image finalImage = Util.newImage(width, height, true);
    final Graphics g = finalImage.getGraphics();
    g.drawImage(img, 0, 0, width, height, this);
    g.dispose();
    retreatTerritory.setIcon(new ImageIcon(finalImage));
  }

  public Territory getSelection() {
    return list.getSelectedValue();
  }
}
