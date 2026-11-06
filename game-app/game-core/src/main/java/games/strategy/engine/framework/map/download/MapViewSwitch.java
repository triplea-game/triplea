package games.strategy.engine.framework.map.download;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

/**
 * The view button next to the search, as file managers have it: the icon of the shown view with a
 * small arrow; a click opens the views to choose from, List, List with pictures or Tiles. Only the
 * player's choice reports a change; {@link #showChosen} does not.
 */
final class MapViewSwitch extends JPanel {
  @Serial private static final long serialVersionUID = -3270517386431021844L;

  private static final int ICON_SIZE = 16;

  private final JButton button;
  private final Map<MapBrowserView, JRadioButtonMenuItem> items =
      new EnumMap<>(MapBrowserView.class);

  MapViewSwitch(final MapBrowserView view, final Consumer<MapBrowserView> viewChosen) {
    super(new BorderLayout());
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

    final JPopupMenu menu = new JPopupMenu();
    final ButtonGroup group = new ButtonGroup();
    for (final MapBrowserView each : MapBrowserView.values()) {
      final JRadioButtonMenuItem item =
          new JRadioButtonMenuItem(each.getLabel(), new ViewIcon(each));
      item.addActionListener(
          e -> {
            showChosen(each);
            viewChosen.accept(each);
          });
      group.add(item);
      items.put(each, item);
      menu.add(item);
    }

    button = newMenuButton(menu);
    // the small arrow says that a menu opens
    button.setText("▾");
    button.setIconTextGap(4);
    button.getAccessibleContext().setAccessibleName("View");
    add(button, BorderLayout.CENTER);
    showChosen(view);
  }

  /**
   * A button of the bar above the maps that opens the menu below it, without a frame until the
   * mouse is over it. A click leaves no focus ring on it; Tab still reaches it.
   */
  static JButton newMenuButton(final JPopupMenu menu) {
    final JButton button = new JButton();
    button.putClientProperty(
        FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    button.setRequestFocusEnabled(false);
    button.addActionListener(e -> menu.show(button, 0, button.getHeight()));
    return button;
  }

  /** Shows the view as the chosen one, eg after the switch of the other tab changed it. */
  void showChosen(final MapBrowserView view) {
    items.get(view).setSelected(true);
    button.setIcon(new ViewIcon(view));
    button.setToolTipText("View: " + view.getLabel());
  }

  /** A small drawing of the view: lines for the list, picture and line for pictures, a grid. */
  private record ViewIcon(MapBrowserView view) implements Icon {
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Color color = UIManager.getColor("Label.foreground");
      g2.setColor(color == null ? c.getForeground() : color);
      g2.translate(x, y);
      switch (view) {
        case LIST -> {
          for (int line = 0; line < 4; line++) {
            g2.fillRoundRect(1, 2 + line * 4, 14, 2, 1, 1);
          }
        }
        case LIST_WITH_PICTURES -> {
          for (int row = 0; row < 3; row++) {
            g2.fillRoundRect(1, 1 + row * 5, 5, 4, 1, 1);
            g2.fillRoundRect(8, 2 + row * 5, 7, 2, 1, 1);
          }
        }
        case TILES -> {
          for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
              g2.fillRoundRect(1 + column * 8, 1 + row * 8, 6, 6, 2, 2);
            }
          }
        }
        default -> throw new IllegalStateException("Unknown view: " + view);
      }
      g2.dispose();
    }

    @Override
    public int getIconWidth() {
      return ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
      return ICON_SIZE;
    }
  }
}
