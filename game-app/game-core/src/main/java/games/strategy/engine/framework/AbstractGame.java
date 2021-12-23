package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.player.DefaultPlayerBridge;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.engine.player.Player;
import games.strategy.engine.vault.Vault;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.triplea.sound.ISound;

/**
 * This abstract class keeps common variables and methods from a game (ClientGame or ServerGame).
 */
public abstract class AbstractGame implements IGame {
  private static final String DISPLAY_CHANNEL =
      "games.strategy.engine.framework.AbstractGame.DISPLAY_CHANNEL";
  private static final String SOUND_CHANNEL =
      "games.strategy.engine.framework.AbstractGame.SOUND_CHANNEL";
  protected final GameData gameData;
  protected final Messengers messengers;
  protected volatile boolean isGameOver = false;
  protected final Vault vault;
  protected boolean firstRun = true;

  IGameModifiedChannel gameModifiedChannel;

  final Map<GamePlayer, Player> gamePlayers = new HashMap<>();
  final PlayerManager playerManager;

  // TODO: Project#20 - clientNetworkBridge will be used for Websocket network bridge.
  //   Usages will basically be to add listeners to map message types to methods calls.
  //   The mapped method calls will replace the existing RMI-style based network code.
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final ClientNetworkBridge clientNetworkBridge;

  @Nullable private IDisplay display;
  @Nullable private ISound sound;

  AbstractGame(
      final GameData data,
      final Set<Player> gamePlayers,
      final Map<String, INode> remotePlayerMapping,
      final Messengers messengers,
      final ClientNetworkBridge clientNetworkBridge) {
    gameData = data;
    this.messengers = messengers;
    this.clientNetworkBridge = clientNetworkBridge;
    vault = new Vault(messengers);
    final Map<String, INode> allPlayers = new HashMap<>(remotePlayerMapping);
    for (final Player player : gamePlayers) {
      // this is necessary for Server games, but not needed for client games.
      allPlayers.put(player.getName(), messengers.getLocalNode());
    }
    playerManager = new PlayerManager(allPlayers);
    setupLocalPlayers(gamePlayers);
  }

  private void setupLocalPlayers(final Set<Player> localPlayers) {
    final PlayerList playerList = gameData.getPlayerList();
    for (final Player gp : localPlayers) {
      final GamePlayer player = playerList.getPlayerId(gp.getName());
      gamePlayers.put(player, gp);
      final IPlayerBridge bridge = new DefaultPlayerBridge(this);
      gp.initialize(bridge, player);
      final RemoteName descriptor = ServerGame.getRemoteName(gp.getGamePlayer());
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

  public static RemoteName getDisplayChannel() {
    return new RemoteName(DISPLAY_CHANNEL, IDisplay.class);
  }

  @Override
  public void setDisplay(final @Nullable IDisplay display) {
    if (Objects.equals(this.display, display)) {
      return;
    }

    if (this.display != null) {
      messengers.unregisterChannelSubscriber(this.display, getDisplayChannel());
      this.display.shutDown();
    }
    if (display != null) {
      messengers.registerChannelSubscriber(display, getDisplayChannel());
    }
    this.display = display;
  }

  public static RemoteName getSoundChannel() {
    return new RemoteName(SOUND_CHANNEL, ISound.class);
  }

  @Override
  public void setSoundChannel(final @Nullable ISound soundChannel) {
    if (Objects.equals(sound, soundChannel)) {
      return;
    }
    if (sound != null) {
      messengers.unregisterChannelSubscriber(sound, getSoundChannel());
    }
    if (soundChannel != null) {
      messengers.registerChannelSubscriber(soundChannel, getSoundChannel());
    }
    sound = soundChannel;
  }
}
