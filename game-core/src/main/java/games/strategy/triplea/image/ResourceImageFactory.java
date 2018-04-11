package games.strategy.triplea.image;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.util.IntegerMap;

/**
 * Used to manage resource images.
 */
public class ResourceImageFactory extends AbstractImageFactory {

  public static final int IMAGE_SIZE = 20;
  private static final String FILE_NAME_BASE = "resources/";

  public ResourceImageFactory() {}

  @Override
  protected String getFileNameBase() {
    return FILE_NAME_BASE;
  }

  public JLabel getLabel(final Resource resource, final IntegerMap<Resource> resources) {
    return getLabel(resource, String.valueOf(resources.getInt(resource)));
  }

  /**
   * Returns label with icon and text. If icon is missing then sets resource name.
   */
  public JLabel getLabel(final Resource resource, final String text) {
    final JLabel label = new JLabel();
    try {
      label.setIcon(getIcon(resource, false));
      label.setText(text);
      label.setToolTipText(resource.getName());
    } catch (final IllegalStateException e) {
      label.setText(resource.getName() + " " + text);
    }
    return label;
  }

  public JPanel getResourcesPanel(final ResourceCollection resources, final GameData data) {
    return getResourcesPanel(resources, false, null, data);
  }

  public JPanel getResourcesPanel(final ResourceCollection resources, final PlayerID player, final GameData data) {
    return getResourcesPanel(resources, true, player, data);
  }

  private JPanel getResourcesPanel(final ResourceCollection resources, final boolean showEmpty, final PlayerID player,
      final GameData data) {
    final JPanel resourcePanel = new JPanel();
    final List<Resource> resourcesInOrder;
    data.acquireReadLock();
    try {
      resourcesInOrder = data.getResourceList().getResources();
    } finally {
      data.releaseReadLock();
    }
    int count = 0;
    for (final Resource resource : resourcesInOrder) {
      if ((player != null && !resource.isDisplayedFor(player))
          || (!showEmpty && resources.getQuantity(resource) == 0)) {
        continue;
      }
      final JLabel resourceLabel = getLabel(resource, resources.getResourcesCopy());
      resourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
      resourcePanel.add(resourceLabel,
          new GridBagConstraints(count++, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
              new Insets(0, 0, 0, 0), 0, 0));
    }
    return resourcePanel;
  }

}
