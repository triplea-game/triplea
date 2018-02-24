package games.strategy.triplea.image;

import javax.swing.JLabel;

import games.strategy.engine.data.Resource;
import games.strategy.util.IntegerMap;

/**
 * Used to manage resource images.
 */
public class ResourceImageFactory extends AbstractImageFactory {

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

}
