package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JButton;
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

  protected MenuPanel(final List<JButton> buttons) {
    setLayout(new BorderLayout());
    final JPanel buttonColumn = new JPanel(new GridBagLayout());
    for (int row = 0; row < buttons.size(); row++) {
      buttonColumn.add(
          buttons.get(row),
          new GridBagConstraintsBuilder(0, row)
              .anchor(GridBagConstraintsAnchor.CENTER)
              .fill(GridBagConstraintsFill.NONE)
              .insets(new Insets(10, 0, 0, 0))
              .build());
    }
    add(buttonColumn);
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
