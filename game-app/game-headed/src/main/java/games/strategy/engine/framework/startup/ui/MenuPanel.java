package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.triplea.EngineImageLoader;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

/**
 * A screen of navigation buttons that leads to other screens. It never starts a game, so the main
 * panel shows neither the game selector nor the Play button alongside it.
 */
public abstract class MenuPanel extends SetupPanel {
  private static final long serialVersionUID = -2349834562085738722L;

  private static final int PRIMARY_FONT_INCREASE = 10;
  private static final int SECONDARY_FONT_INCREASE = 3;

  /** The logo asset is small; the menus show it larger, at the cost of some softness. */
  private static final float LOGO_SCALE = 1.5f;

  /**
   * Lays out the logo, then {@code primary} entries as large capitalized text, then a gap and the
   * smaller {@code secondary} entries.
   */
  protected MenuPanel(final List<JButton> primary, final List<JButton> secondary) {
    setLayout(new BorderLayout());
    final JPanel buttonColumn = new JPanel(new GridBagLayout());
    int row = 0;
    buttonColumn.add(new JLabel(new ImageIcon(loadLogo())), centered(row++, 20));
    for (final JButton button : primary) {
      button.setText(button.getText().toUpperCase(Locale.ROOT));
      LaunchScreenStyle.styleAsMenuItem(button, PRIMARY_FONT_INCREASE);
      buttonColumn.add(button, centered(row++, 4));
    }
    for (int i = 0; i < secondary.size(); i++) {
      final JButton button = secondary.get(i);
      LaunchScreenStyle.styleAsMenuItem(button, SECONDARY_FONT_INCREASE);
      buttonColumn.add(button, centered(row++, i == 0 ? 24 : 2));
    }
    add(buttonColumn);
  }

  private static Image loadLogo() {
    final Image logo = EngineImageLoader.loadImage("launch_screens", "triplea-logo.png");
    return logo.getScaledInstance(
        Math.round(logo.getWidth(null) * LOGO_SCALE),
        Math.round(logo.getHeight(null) * LOGO_SCALE),
        Image.SCALE_SMOOTH);
  }

  private static GridBagConstraints centered(final int row, final int spaceAbove) {
    return new GridBagConstraintsBuilder(0, row)
        .anchor(GridBagConstraintsAnchor.CENTER)
        .fill(GridBagConstraintsFill.NONE)
        .insets(new Insets(spaceAbove, 0, 0, 0))
        .build();
  }

  @Override
  public boolean canGameStart() {
    return false;
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  @Override
  public void cancel() {
    // nothing to do
  }

  @Override
  public Optional<ILauncher> getLauncher() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void postStartGame() {
    // nothing to do
  }
}
