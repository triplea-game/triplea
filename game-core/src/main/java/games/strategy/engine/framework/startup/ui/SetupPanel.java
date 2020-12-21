package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.startup.SetupModel;
import org.triplea.injection.Injections;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * Headed Implementation of SetupConfiguration. This is the base-class for any panel that configures
 * a game.
 */
public abstract class SetupPanel extends JPanel implements SetupModel {
  private static final long serialVersionUID = 4001323470187210773L;
  private static final String SET_ALL_DEFAULT_LABEL = "Default";

  @Nullable private Consumer<SetupPanel> listener;

  public void setPanelChangedListener(final Consumer<SetupPanel> listener) {
    this.listener = listener;
  }

  public void fireListener() {
    if (listener != null) {
      listener.accept(this);
    }
  }

  @Nullable
  @Override
  public ChatModel getChatModel() {
    return null;
  }

  public abstract List<Action> getUserActions();

  public void layoutPlayerComponents(
      final JPanel panel, final List<PlayerSelectorRow> playerRows, final GameDataInjections data) {
    panel.removeAll();
    playerRows.clear();
    panel.setLayout(new GridBagLayout());
    if (data == null) {
      panel.add(new JLabel("No game selected!"));
      return;
    }

    final Collection<String> disableable = data.getPlayerList().getPlayersThatMayBeDisabled();
    final Map<String, Boolean> playersEnablementListing =
        data.getPlayerList().getPlayersEnabledListing();
    final Map<String, String> reloadSelections = GamePlayer.currentPlayers(data);
    final List<GamePlayer> players = data.getPlayerList().getPlayers();

    int gridx = 0;
    int gridy = 1;
    if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE)) {
      final JLabel enableLabel = new JLabel("Use");
      panel.add(
          enableLabel,
          new GridBagConstraints(
              gridx++,
              gridy,
              1,
              1,
              0,
              0,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(0, 5, 5, 0),
              0,
              0));
    }
    final JLabel setAllTypesLabel = new JLabel("Set All To:");
    panel.add(
        setAllTypesLabel,
        new GridBagConstraints(
            gridx,
            gridy - 1,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel nameLabel = new JLabel("Name");
    panel.add(
        nameLabel,
        new GridBagConstraints(
            gridx++,
            gridy,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 5, 5, 0),
            0,
            0));
    final PlayerTypes playerTypes = new PlayerTypes(Injections.getInstance().getPlayerTypes());
    final JComboBox<String> setAllTypes = new JComboBox<>(playerTypes.getAvailablePlayerLabels());
    setAllTypes.insertItemAt(SET_ALL_DEFAULT_LABEL, 0);
    setAllTypes.setSelectedIndex(-1);
    panel.add(
        setAllTypes,
        new GridBagConstraints(
            gridx,
            gridy - 1,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel typeLabel = new JLabel("Type");
    panel.add(
        typeLabel,
        new GridBagConstraints(
            gridx++,
            gridy,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 5, 5, 0),
            0,
            0));
    final JLabel allianceLabel = new JLabel("Alliance");
    panel.add(
        allianceLabel,
        new GridBagConstraints(
            gridx++,
            gridy,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 7, 5, 5),
            0,
            0));
    final JButton resourceModifiers = new JButton();
    panel.add(
        resourceModifiers,
        new GridBagConstraints(
            gridx,
            gridy - 1,
            3,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel incomeLabel = new JLabel("Income");
    panel.add(
        incomeLabel,
        new GridBagConstraints(
            gridx++,
            gridy,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 20, 5, 0),
            0,
            0));
    incomeLabel.setVisible(false);
    gridx++;
    final JLabel puIncomeBonusLabel = new JLabel("Bonus Income");
    panel.add(
        puIncomeBonusLabel,
        new GridBagConstraints(
            gridx,
            gridy,
            2,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 20, 5, 0),
            0,
            0));
    puIncomeBonusLabel.setVisible(false);

    // Add players in the order they were defined in the XML
    for (final GamePlayer player : players) {
      final PlayerSelectorRow selector =
          new PlayerSelectorRow(
              playerRows,
              player,
              reloadSelections,
              disableable,
              playersEnablementListing,
              data.getAllianceTracker().getAlliancesPlayerIsIn(player),
              this,
              data.getProperties(),
              new PlayerTypes(Injections.getInstance().getPlayerTypes()));
      playerRows.add(selector);
      if (!player.isHidden()) {
        selector.layout(++gridy, panel);
        selector.setResourceModifiersVisible(false);
      }
    }

    resourceModifiers.setAction(
        SwingAction.of(
            "Resource Modifiers",
            e -> {
              final boolean isVisible = incomeLabel.isVisible();
              incomeLabel.setVisible(!isVisible);
              puIncomeBonusLabel.setVisible(!isVisible);
              playerRows.forEach(row -> row.setResourceModifiersVisible(!isVisible));
            }));

    setAllTypes.addActionListener(
        e -> {
          final String selectedType = (String) setAllTypes.getSelectedItem();
          if (selectedType != null) {
            if (SET_ALL_DEFAULT_LABEL.equals(selectedType)) {
              playerRows.forEach(PlayerSelectorRow::setDefaultPlayerType);
            } else {
              playerRows.forEach(row -> row.setPlayerType(selectedType));
            }
          }
        });
    SwingComponents.redraw(panel);
  }

  public abstract boolean isCancelButtonVisible();
}
