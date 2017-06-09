package games.strategy.engine.framework.startup.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.ui.SwingAction;

/** Setup panel when hosting a local game. */
public class LocalSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 2284030734590389060L;
  private final GameSelectorModel m_gameSelectorModel;
  private final List<PlayerSelectorRow> m_playerTypes = new ArrayList<>();

  public LocalSetupPanel(final GameSelectorModel model) {
    m_gameSelectorModel = model;
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {}

  private void layoutComponents() {
    final GameData data = m_gameSelectorModel.getGameData();
    removeAll();
    m_playerTypes.clear();
    setLayout(new GridBagLayout());
    if (data == null) {
      add(new JLabel("No game selected!"));
      return;
    }
    final Collection<String> disableable = data.getPlayerList().getPlayersThatMayBeDisabled();
    final HashMap<String, Boolean> playersEnablementListing = data.getPlayerList().getPlayersEnabledListing();
    final Map<String, String> reloadSelections = PlayerID.currentPlayers(data);
    final String[] playerTypes = data.getGameLoader().getServerPlayerTypes();
    final List<PlayerID> players = data.getPlayerList().getPlayers();
    // if the xml was created correctly, this list will be in turn order. we want to keep it that way.
    int gridx = 0;
    int gridy = 0;       
    if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE)) {
      final JLabel enableLabel = new JLabel("Use");
      this.add(enableLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    final JLabel nameLabel = new JLabel("Name");
    this.add(nameLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JLabel typeLabel = new JLabel("Type");
    this.add(typeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    final JLabel allianceLabel = new JLabel("Alliance");
    this.add(allianceLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
    final JLabel bonusLabel = new JLabel("Income");
    this.add(bonusLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 20, 5, 0), 0, 0));
    bonusLabel.setVisible(false);
    JButton resourceModifiers = new JButton();   
    this.add(resourceModifiers, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.EAST,
            GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
    for (final PlayerID player : players) {
      final PlayerSelectorRow selector =
          new PlayerSelectorRow(player, reloadSelections, disableable, playersEnablementListing,
              data.getAllianceTracker().getAlliancesPlayerIsIn(player), playerTypes, this, data.getProperties());
      m_playerTypes.add(selector);
      if (!player.isHidden()) {
        selector.layout(++gridy, this);
        selector.setResourceModifiersVisble(false);
      }      
    }
    
    Action resourceModifiersAction = SwingAction.of("Resource Modifiers", e -> {
    	boolean isVisible = bonusLabel.isVisible();
    	bonusLabel.setVisible(!isVisible);
        m_playerTypes.forEach(row -> row.setResourceModifiersVisble(!isVisible));
    });
    resourceModifiers.setAction(resourceModifiersAction);
    
    validate();
    invalidate();
    setWidgetActivation();
  }

  private void setupListeners() {
    m_gameSelectorModel.addObserver(this);
  }

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

  @Override
  public void setWidgetActivation() {}

  @Override
  public boolean canGameStart() {
    if (m_gameSelectorModel.getGameData() == null) {
      return false;
    }
    // make sure at least 1 player is enabled
    for (final PlayerSelectorRow player : m_playerTypes) {
      if (player.isPlayerEnabled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void postStartGame() {
    final GameData data = m_gameSelectorModel.getGameData();
    data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, false);
  }

  @Override
  public void shutDown() {
    m_gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void cancel() {
    m_gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void update(final Observable o, final Object arg) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> layoutComponents());
      return;
    }
    layoutComponents();
  }

  @Override
  public ILauncher getLauncher() {
    final IRandomSource randomSource = new PlainRandomSource();
    final Map<String, String> playerTypes = new HashMap<>();
    final Map<String, Boolean> playersEnabled = new HashMap<>();
    for (final PlayerSelectorRow player : m_playerTypes) {
      playerTypes.put(player.getPlayerName(), player.getPlayerType());
      playersEnabled.put(player.getPlayerName(), player.isPlayerEnabled());
    }
    // we don't need the playerToNode list, the disable-able players, or the alliances
    // list, for a local game
    final PlayerListing pl =
        new PlayerListing(null, playersEnabled, playerTypes, m_gameSelectorModel.getGameData().getGameVersion(),
            m_gameSelectorModel.getGameName(), m_gameSelectorModel.getGameRound(), null, null);
    final LocalLauncher launcher = new LocalLauncher(m_gameSelectorModel, randomSource, pl);
    return launcher;
  }
}
