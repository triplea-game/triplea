package games.strategy.triplea.image;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.collections.IntegerMap;

/** Used to manage resource images. */
public class ResourceImageFactory extends AbstractImageFactory {

  public static final int IMAGE_SIZE = 20;
  private static final int LARGE_IMAGE_SIZE = 48;
  @NonNls private static final String LARGE_SUFFIX = "_large";

  @Override
  protected String getFileNameBase() {
    return "resources/";
  }

  @Override
  protected Optional<Image> createMissingImage(final String baseImageName) {
    final boolean large = baseImageName.endsWith(LARGE_SUFFIX);
    final int size = large ? LARGE_IMAGE_SIZE : IMAGE_SIZE;
    final String label =
        large
            ? baseImageName.substring(0, baseImageName.length() - LARGE_SUFFIX.length())
            : baseImageName;
    return Optional.of(renderPlaceholder(label, size));
  }

  private static BufferedImage renderPlaceholder(final String name, final int size) {
    final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(new Color(230, 230, 230));
      g.fillRect(0, 0, size, size);
      g.setColor(Color.DARK_GRAY);
      g.drawRect(0, 0, size - 1, size - 1);
      final String text = name.substring(0, Math.min(2, name.length()));
      g.setFont(g.getFont().deriveFont(Font.BOLD, size * 0.5f));
      final FontMetrics fm = g.getFontMetrics();
      g.drawString(
          text, (size - fm.stringWidth(text)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
    } finally {
      g.dispose();
    }
    return image;
  }

  public JLabel getLabel(final Resource resource, final ResourceCollection resourceCollection) {
    return getLabel(resource, String.valueOf(resourceCollection.getQuantity(resource)));
  }

  public JLabel getLabel(final Resource resource, final IntegerMap<Resource> resources) {
    return getLabel(resource, String.valueOf(resources.getInt(resource)));
  }

  public JLabel getLabel(final Resource resource, final String text) {
    final JLabel label = new JLabel();
    label.setIcon(getIcon(resource.getName()));
    label.setText(text);
    label.setToolTipText(resource.getName());
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
