package games.strategy.engine.framework.startup.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.ui.SwingAction;

abstract class SetupPanel extends JPanel implements ISetupPanel {
  private static final long serialVersionUID = 4001323470187210773L;
  private static final String SET_ALL_DEFAULT_LABEL = "Default";

  private final List<Observer> listeners = new ArrayList<>();

  @Override
  public void addObserver(final Observer observer) {
    listeners.add(observer);
  }

  @Override
  public void removeObserver(final Observer observer) {
    listeners.add(observer);
  }

  @Override
  public void notifyObservers() {
    for (final Observer observer : listeners) {
      observer.update(null, null);
    }
  }

  @Override
  public IChatPanel getChatPanel() {
    return null;
  }

  @Override
  public abstract void cancel();

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

  void layoutPlayerComponents(final JPanel panel, final List<PlayerSelectorRow> playerRows, final GameData data) {
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

    int gridx = 0;
    int gridy = 1;
    if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE)) {
      final JLabel enableLabel = new JLabel("Use");
      panel.add(enableLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    final JLabel setAllTypesLabel = new JLabel("Set All To:");
    panel.add(setAllTypesLabel, new GridBagConstraints(gridx, gridy - 1, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 5, 15, 0), 0, 0));
    final JLabel nameLabel = new JLabel("Name");
    panel.add(nameLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JComboBox<String> setAllTypes = new JComboBox<>(playerTypes);
    setAllTypes.insertItemAt(SET_ALL_DEFAULT_LABEL, 0);
    setAllTypes.setSelectedIndex(-1);
    panel.add(setAllTypes, new GridBagConstraints(gridx, gridy - 1, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 15, 0), 0, 0));
    final JLabel typeLabel = new JLabel("Type");
    panel.add(typeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JLabel allianceLabel = new JLabel("Alliance");
    panel.add(allianceLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
    final JButton resourceModifiers = new JButton();
    panel.add(resourceModifiers, new GridBagConstraints(gridx, gridy - 1, 3, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 15, 0), 0, 0));
    final JLabel incomeLabel = new JLabel("Income");
    panel.add(incomeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
    incomeLabel.setVisible(false);
    gridx++;
    final JLabel puIncomeBonusLabel = new JLabel("Bonus Income");
    panel.add(puIncomeBonusLabel, new GridBagConstraints(gridx++, gridy, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
    puIncomeBonusLabel.setVisible(false);
    gridx++;

    // Add players in the order they were defined in the XML
    for (final PlayerID player : players) {
      final PlayerSelectorRow selector =
          new PlayerSelectorRow(playerRows, player, reloadSelections, disableable, playersEnablementListing,
              data.getAllianceTracker().getAlliancesPlayerIsIn(player), playerTypes, this, data.getProperties());
      playerRows.add(selector);
      if (!player.isHidden()) {
        selector.layout(++gridy, panel);
        selector.setResourceModifiersVisble(false);
      }
    }

    final Action resourceModifiersAction = SwingAction.of("Resource Modifiers", e -> {
      final boolean isVisible = incomeLabel.isVisible();
      incomeLabel.setVisible(!isVisible);
      puIncomeBonusLabel.setVisible(!isVisible);
      playerRows.forEach(row -> row.setResourceModifiersVisble(!isVisible));
    });
    resourceModifiers.setAction(resourceModifiersAction);

    final ActionListener setAllTypesAction = e -> {
      final String selectedType = setAllTypes.getSelectedItem().toString();
      if (SET_ALL_DEFAULT_LABEL.equals(selectedType)) {
        playerRows.forEach(PlayerSelectorRow::setDefaultPlayerType);
      } else {
        playerRows.forEach(row -> row.setPlayerType(selectedType));
      }
    };
    setAllTypes.addActionListener(setAllTypesAction);

    panel.validate();
    panel.repaint();
  }
}
