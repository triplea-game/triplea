package games.strategy.engine.framework.startup.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;

/** Setup panel when hosting a local game. */
public class LocalSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 2284030734590389060L;
  private final GameSelectorModel m_gameSelectorModel;
  private final List<PlayerSelectorRow> m_playerTypes = new ArrayList<>();

  public LocalSetupPanel(final GameSelectorModel model) {
    m_gameSelectorModel = model;
    createComponents();
    layoutPlayerComponents(this, m_playerTypes, m_gameSelectorModel.getGameData());
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {}

  private void setupListeners() {
    m_gameSelectorModel.addObserver(this);
  }

  @Override
  public void setWidgetActivation() {}

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

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
      SwingUtilities.invokeLater(() -> layoutPlayerComponents(this, m_playerTypes, m_gameSelectorModel.getGameData()));
      return;
    }
    layoutPlayerComponents(this, m_playerTypes, m_gameSelectorModel.getGameData());
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
