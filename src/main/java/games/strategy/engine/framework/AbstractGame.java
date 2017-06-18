package games.strategy.engine.framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.display.DefaultDisplayBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.DefaultPlayerBridge;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.vault.Vault;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.sound.ISound;

/**
 * This abstract class keeps common variables and methods from a game (ClientGame or ServerGame).
 */
public abstract class AbstractGame implements IGame {
  protected static final String DISPLAY_CHANNEL = "games.strategy.engine.framework.AbstractGame.DISPLAY_CHANNEL";
  protected static final String SOUND_CHANNEL = "games.strategy.engine.framework.AbstractGame.SOUND_CHANNEL";
  protected final GameData m_data;
  protected final IMessenger m_messenger;
  protected final IRemoteMessenger m_remoteMessenger;
  protected final IChannelMessenger m_channelMessenger;
  protected final Map<PlayerID, IGamePlayer> m_gamePlayers = new HashMap<>();
  protected volatile boolean m_isGameOver = false;
  protected final Vault m_vault;
  protected IGameModifiedChannel m_gameModifiedChannel;
  protected final PlayerManager m_playerManager;
  protected boolean m_firstRun = true;
  protected final List<GameStepListener> m_gameStepListeners = new CopyOnWriteArrayList<>();

  protected AbstractGame(final GameData data, final Set<IGamePlayer> gamePlayers,
      final Map<String, INode> remotePlayerMapping, final Messengers messengers) {
    m_data = data;
    m_messenger = messengers.getMessenger();
    m_remoteMessenger = messengers.getRemoteMessenger();
    m_channelMessenger = messengers.getChannelMessenger();
    m_vault = new Vault(m_channelMessenger);
    final Map<String, INode> allPlayers = new HashMap<>(remotePlayerMapping);
    for (final IGamePlayer player : gamePlayers) {
      // this is necessary for Server games, but not needed for client games.
      allPlayers.put(player.getName(), m_messenger.getLocalNode());
    }
    m_playerManager = new PlayerManager(allPlayers);
    if (m_playerManager == null) {
      throw new IllegalArgumentException("Player manager cant be null");
    }
    setupLocalPlayers(gamePlayers);
  }

  private void setupLocalPlayers(final Set<IGamePlayer> localPlayers) {
    final PlayerList playerList = m_data.getPlayerList();
    for (final IGamePlayer gp : localPlayers) {
      final PlayerID player = playerList.getPlayerID(gp.getName());
      m_gamePlayers.put(player, gp);
      final IPlayerBridge bridge = new DefaultPlayerBridge(this);
      gp.initialize(bridge, player);
      final RemoteName descriptor = ServerGame.getRemoteName(gp.getPlayerID(), m_data);
      m_remoteMessenger.registerRemote(gp, descriptor);
    }
  }

  /**
   * @param stepName
   *        step name.
   * @param delegateName
   *        delegate name
   * @param player
   *        playerID
   * @param round
   *        round number
   * @param displayName
   *        display name
   */
  protected void notifyGameStepListeners(final String stepName, final String delegateName, final PlayerID player,
      final int round, final String displayName) {
    for (final GameStepListener listener : m_gameStepListeners) {
      listener.gameStepChanged(stepName, delegateName, player, round, displayName);
    }
  }

  @Override
  public GameData getData() {
    return m_data;
  }

  @Override
  public Vault getVault() {
    return m_vault;
  }

  @Override
  public boolean isGameOver() {
    return m_isGameOver;
  }

  @Override
  public IRemoteMessenger getRemoteMessenger() {
    return m_remoteMessenger;
  }

  @Override
  public IChannelMessenger getChannelMessenger() {
    return m_channelMessenger;
  }

  @Override
  public IMessenger getMessenger() {
    return m_messenger;
  }

  @Override
  public PlayerManager getPlayerManager() {
    return m_playerManager;
  }

  @Override
  public void addGameStepListener(final GameStepListener listener) {
    m_gameStepListeners.add(listener);
  }

  @Override
  public void removeGameStepListener(final GameStepListener listener) {
    m_gameStepListeners.remove(listener);
  }

  public static RemoteName getDisplayChannel(final GameData data) {
    return new RemoteName(DISPLAY_CHANNEL, data.getGameLoader().getDisplayType());
  }

  @Override
  public void addDisplay(final IDisplay display) {
    display.initialize(new DefaultDisplayBridge(m_data));
    m_channelMessenger.registerChannelSubscriber(display, getDisplayChannel(getData()));
  }

  @Override
  public void removeDisplay(final IDisplay display) {
    m_channelMessenger.unregisterChannelSubscriber(display, getDisplayChannel(getData()));
  }

  public static RemoteName getSoundChannel(final GameData data) {
    return new RemoteName(SOUND_CHANNEL, data.getGameLoader().getSoundType());
  }

  @Override
  public void addSoundChannel(final ISound soundChannel) {
    m_channelMessenger.registerChannelSubscriber(soundChannel, getSoundChannel(getData()));
  }

  @Override
  public void removeSoundChannel(final ISound soundChannel) {
    m_channelMessenger.unregisterChannelSubscriber(soundChannel, getSoundChannel(getData()));
  }
}
