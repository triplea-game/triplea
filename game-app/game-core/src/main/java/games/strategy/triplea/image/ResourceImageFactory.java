package games.strategy.triplea.image;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.java.collections.IntegerMap;

/** Used to manage resource images. */
public class ResourceImageFactory extends AbstractImageFactory {

  public static final int IMAGE_SIZE = 20;

  @Override
  protected String getFileNameBase() {
    return "resources/";
  }

  public JLabel getLabel(final Resource resource, final ResourceCollection resourceCollection) {
    return getLabel(resource, String.valueOf(resourceCollection.getQuantity(resource)));
  }

  public JLabel getLabel(final Resource resource, final IntegerMap<Resource> resources) {
    return getLabel(resource, String.valueOf(resources.getInt(resource)));
  }

  /** Returns label with icon and text. If icon is missing then sets resource name. */
  public JLabel getLabel(final Resource resource, final String text) {
    final JLabel label = new JLabel();
    try {
      label.setIcon(getIcon(resource.getName()));
      label.setText(text);
      label.setToolTipText(resource.getName());
    } catch (final IllegalStateException e) {
      label.setText(resource.getName() + " " + text);
    }
    return label;
  }

  public JPanel getResourcesPanel(final ResourceCollection resources) {
    return getResourcesPanel(resources, false, null);
  }

  public JPanel getResourcesPanel(final ResourceCollection resources, final GamePlayer player) {
    return getResourcesPanel(resources, true, player);
  }

  private JPanel getResourcesPanel(
      final ResourceCollection resources, final boolean showEmpty, final GamePlayer player) {
    final JPanel resourcePanel = new JPanel();
    final Collection<Resource> resourcesInOrder;
    final GameData data = resources.getData();
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      resourcesInOrder = data.getResourceList().getResources();
    }
    int count = 0;
    for (final Resource resource : resourcesInOrder) {
      if ((player != null && !resource.isDisplayedFor(player))
          || (!showEmpty && resources.getQuantity(resource) == 0)) {
        continue;
      }
      final JLabel resourceLabel = getLabel(resource, resources);
      resourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
      resourcePanel.add(
          resourceLabel,
          new GridBagConstraints(
              count++,
              0,
              1,
              1,
              0,
              0,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(0, 0, 0, 0),
              0,
              0));
    }
    return resourcePanel;
  }

  /**
   * Returns button with resource amounts and given text. If resources is empty then returns button
   * with just the text.
   */
  public JButton getResourcesButton(final ResourceCollection resources, final String text) {
    if (resources.isEmpty()) {
      return new JButton(text);
    }
    final JPanel panel = getResourcesPanel(resources);
    panel.setOpaque(false);
    panel.add(new JLabel(text));
    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
    final BufferedImage image =
        new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    panel.paint(g);
    g.dispose();
    return new JButton(new ImageIcon(image));
  }
}
