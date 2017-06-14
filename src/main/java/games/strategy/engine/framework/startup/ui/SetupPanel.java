package games.strategy.engine.framework.startup.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.ui.SwingAction;

public abstract class SetupPanel extends JPanel implements ISetupPanel {
  private static final long serialVersionUID = 4001323470187210773L;
  private final List<Observer> m_listeners = new CopyOnWriteArrayList<>();

  @Override
  public void addObserver(final Observer observer) {
    m_listeners.add(observer);
  }

  @Override
  public void removeObserver(final Observer observer) {
    m_listeners.add(observer);
  }

  @Override
  public void notifyObservers() {
    for (final Observer observer : m_listeners) {
      observer.update(null, null);
    }
  }

  /**
   * Subclasses that have chat override this.
   */
  @Override
  public IChatPanel getChatPanel() {
    return null;
  }

  /**
   * Cleanup should occur here that occurs when we cancel.
   */
  @Override
  public abstract void cancel();

  /**
   * Indicates we can start the game.
   */
  @Override
  public abstract boolean canGameStart();

  @Override
  public abstract void setWidgetActivation();

  @Override
  public void preStartGame() {}

  @Override
  public void postStartGame() {}

  @Override
  public ILauncher getLauncher() {
    throw new IllegalStateException("NOt implemented");
  }

  @Override
  public List<Action> getUserActions() {
    return new ArrayList<>();
  }

  void layoutPlayerComponents(JPanel panel, List<PlayerSelectorRow> playerRows, GameData data) {
    panel.removeAll();
    playerRows.clear();
    panel.setLayout(new GridBagLayout());
    if (data == null) {
      panel.add(new JLabel("No game selected!"));
      return;
    }
    final Collection<String> disableable = data.getPlayerList().getPlayersThatMayBeDisabled();
    final HashMap<String, Boolean> playersEnablementListing = data.getPlayerList().getPlayersEnabledListing();
    final Map<String, String> reloadSelections = PlayerID.currentPlayers(data);
    final String[] playerTypes = data.getGameLoader().getServerPlayerTypes();
    final List<PlayerID> players = data.getPlayerList().getPlayers();
    // if the xml was created correctly, this list will be in turn order. we want to keep it that way.
    int gridx = 0;
    int gridy = 1;
    if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE)) {
      final JLabel enableLabel = new JLabel("Use");
      panel.add(enableLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    final JLabel nameLabel = new JLabel("Name");
    panel.add(nameLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JLabel typeLabel = new JLabel("Type");
    panel.add(typeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JLabel allianceLabel = new JLabel("Alliance");
    panel.add(allianceLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
    JButton resourceModifiers = new JButton();
    panel.add(resourceModifiers, new GridBagConstraints(gridx, gridy - 1, 3, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    final JLabel incomeLabel = new JLabel("Income");
    panel.add(incomeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
    incomeLabel.setVisible(false);
    gridx++;
    final JLabel puIncomeBonusLabel = new JLabel("Flat Income");
    panel.add(puIncomeBonusLabel, new GridBagConstraints(gridx++, gridy, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
    puIncomeBonusLabel.setVisible(false);
    gridx++;
    for (final PlayerID player : players) {
      final PlayerSelectorRow selector =
          new PlayerSelectorRow(player, reloadSelections, disableable, playersEnablementListing,
              data.getAllianceTracker().getAlliancesPlayerIsIn(player), playerTypes, this, data.getProperties());
      playerRows.add(selector);
      if (!player.isHidden()) {
        selector.layout(++gridy, panel);
        selector.setResourceModifiersVisble(false);
      }
    }

    Action resourceModifiersAction = SwingAction.of("Resource Modifiers", e -> {
      boolean isVisible = incomeLabel.isVisible();
      incomeLabel.setVisible(!isVisible);
      puIncomeBonusLabel.setVisible(!isVisible);
      playerRows.forEach(row -> row.setResourceModifiersVisble(!isVisible));
    });
    resourceModifiers.setAction(resourceModifiersAction);

    panel.validate();
    panel.invalidate();
  }

}
