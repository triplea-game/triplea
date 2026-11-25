package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.startup.mc.HeadedPlayerTypes;
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
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * Headed Implementation of SetupConfiguration. This is the base-class for any panel that configures
 * a game.
 */
public abstract class SetupPanel extends JPanel implements SetupModel {
  private static final long serialVersionUID = 4001323470187210773L;
  private static final String SET_ALL_DEFAULT_LABEL =
      I18nEngineFramework.get().getText("startup.SetupPanel.SET_ALL_DEFAULT_LABEL");

  @Nullable private transient Consumer<SetupPanel> listener;

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
      final JPanel panel, final List<PlayerSelectorRow> playerRows, final GameState data) {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    panel.removeAll();
    playerRows.clear();
    panel.setLayout(new GridBagLayout());
    if (data == null) {
      panel.add(new JLabel(bundle.getText("startup.SetupPanel.noGameSelected.Lbl")));
      return;
    }

    final Collection<String> playersThatMayBeDisabled =
        data.getPlayerList().getPlayersThatMayBeDisabled();
    final Map<String, Boolean> playersEnablementListing =
        data.getPlayerList().getPlayersEnabledListing();
    final Map<String, String> reloadSelections = GamePlayer.currentPlayers(data);
    final List<GamePlayer> players = data.getPlayerList().getPlayers();

    int gridX = 0;
    int gridY = 1;
    if (!playersThatMayBeDisabled.isEmpty()
        || playersEnablementListing.containsValue(Boolean.FALSE)) {
      final JLabel enableLabel = new JLabel(bundle.getText("startup.SetupPanel.enable.Lbl"));
      panel.add(
          enableLabel,
          new GridBagConstraints(
              gridX++,
              gridY,
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
    final JLabel setAllTypesLabel = new JLabel(bundle.getText("startup.SetupPanel.setAllTo.Lbl"));
    panel.add(
        setAllTypesLabel,
        new GridBagConstraints(
            gridX,
            gridY - 1,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel nameLabel = new JLabel(bundle.getText("startup.SetupPanel.name.Lbl"));
    panel.add(
        nameLabel,
        new GridBagConstraints(
            gridX++,
            gridY,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 5, 5, 0),
            0,
            0));
    final PlayerTypes playerTypes = new PlayerTypes(HeadedPlayerTypes.getPlayerTypes());
    final JComboBox<String> setAllTypes = new JComboBox<>(playerTypes.getAvailablePlayerLabels());
    setAllTypes.insertItemAt(SET_ALL_DEFAULT_LABEL, 0);
    setAllTypes.setSelectedIndex(-1);
    panel.add(
        setAllTypes,
        new GridBagConstraints(
            gridX,
            gridY - 1,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel typeLabel = new JLabel(bundle.getText("startup.SetupPanel.type.Lbl"));
    panel.add(
        typeLabel,
        new GridBagConstraints(
            gridX++,
            gridY,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 5, 5, 0),
            0,
            0));
    final JLabel allianceLabel = new JLabel(bundle.getText("startup.SetupPanel.alliance.Lbl"));
    panel.add(
        allianceLabel,
        new GridBagConstraints(
            gridX++,
            gridY,
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
            gridX,
            gridY - 1,
            3,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 15, 0),
            0,
            0));
    final JLabel incomeLabel = new JLabel(bundle.getText("startup.SetupPanel.income.Lbl"));
    panel.add(
        incomeLabel,
        new GridBagConstraints(
            gridX++,
            gridY,
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
    gridX++;
    final JLabel puIncomeBonusLabel =
        new JLabel(bundle.getText("startup.SetupPanel.bonusIncome.Lbl"));
    panel.add(
        puIncomeBonusLabel,
        new GridBagConstraints(
            gridX,
            gridY,
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
              playersThatMayBeDisabled,
              playersEnablementListing,
              data.getAllianceTracker().getAlliancesPlayerIsIn(player),
              this,
              data.getProperties(),
              new PlayerTypes(HeadedPlayerTypes.getPlayerTypes()));
      playerRows.add(selector);
      if (!player.isHidden()) {
        selector.layout(++gridY, panel);
        selector.setResourceModifiersVisible(false);
      }
    }

    resourceModifiers.setAction(
        SwingAction.of(
            I18nEngineFramework.get().getText("startup.SetupPanel.resourceModifiers.Lbl"),
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
