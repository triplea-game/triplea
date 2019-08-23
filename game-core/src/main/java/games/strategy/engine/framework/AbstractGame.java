package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.player.DefaultPlayerBridge;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.engine.player.IRemotePlayer;
import games.strategy.engine.vault.Vault;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.sound.ISound;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This abstract class keeps common variables and methods from a game (ClientGame or ServerGame).
 */
public abstract class AbstractGame implements IGame {
  protected static final String DISPLAY_CHANNEL =
      "games.strategy.engine.framework.AbstractGame.DISPLAY_CHANNEL";
  protected static final String SOUND_CHANNEL =
      "games.strategy.engine.framework.AbstractGame.SOUND_CHANNEL";
  protected final GameData gameData;
  protected final Messengers messengers;
  protected final Map<PlayerId, IRemotePlayer> gamePlayers = new HashMap<>();
  protected volatile boolean isGameOver = false;
  protected final Vault vault;
  protected IGameModifiedChannel gameModifiedChannel;
  protected final PlayerManager playerManager;
  protected boolean firstRun = true;

  protected AbstractGame(
      final GameData data,
      final Set<IRemotePlayer> gamePlayers,
      final Map<String, INode> remotePlayerMapping,
      final Messengers messengers) {
    gameData = data;
    this.messengers = messengers;
    vault = new Vault(messengers);
    final Map<String, INode> allPlayers = new HashMap<>(remotePlayerMapping);
    for (final IRemotePlayer player : gamePlayers) {
      // this is necessary for Server games, but not needed for client games.
      allPlayers.put(player.getName(), messengers.getLocalNode());
    }
    playerManager = new PlayerManager(allPlayers);
    setupLocalPlayers(gamePlayers);
  }

  private void setupLocalPlayers(final Set<IRemotePlayer> localPlayers) {
    final PlayerList playerList = gameData.getPlayerList();
    for (final IRemotePlayer gp : localPlayers) {
      final PlayerId player = playerList.getPlayerId(gp.getName());
      gamePlayers.put(player, gp);
      final IPlayerBridge bridge = new DefaultPlayerBridge(this);
      gp.initialize(bridge, player);
      final RemoteName descriptor = ServerGame.getRemoteName(gp.getPlayerId(), gameData);
      messengers.registerRemote(gp, descriptor);
    }
  }

  @Override
  public GameData getData() {
    return gameData;
  }

  @Override
  public Vault getVault() {
    return vault;
  }

  @Override
  public boolean isGameOver() {
    return isGameOver;
  }

  @Override
  public Messengers getMessengers() {
    return messengers;
  }

  @Override
  public PlayerManager getPlayerManager() {
    return playerManager;
  }

  public static RemoteName getDisplayChannel(final GameData data) {
    return new RemoteName(DISPLAY_CHANNEL, data.getGameLoader().getDisplayType());
  }

  @Override
  public void addDisplay(final IDisplay display) {
    messengers.registerChannelSubscriber(display, getDisplayChannel(getData()));
  }

  @Override
  public void removeDisplay(final IDisplay display) {
    messengers.unregisterChannelSubscriber(display, getDisplayChannel(getData()));
  }

  public static RemoteName getSoundChannel(final GameData data) {
    return new RemoteName(SOUND_CHANNEL, data.getGameLoader().getSoundType());
  }

  @Override
  public void addSoundChannel(final ISound soundChannel) {
    messengers.registerChannelSubscriber(soundChannel, getSoundChannel(getData()));
  }

  @Override
  public void removeSoundChannel(final ISound soundChannel) {
    messengers.unregisterChannelSubscriber(soundChannel, getSoundChannel(getData()));
  }
}
