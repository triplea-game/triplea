package games.strategy.engine.framework;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.player.Player;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.vault.Vault;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.ResourceLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.triplea.sound.ISound;

/**
 * This abstract class keeps common variables and methods from a game (ClientGame or ServerGame).
 */
public abstract class AbstractGame implements IGame {
  @NonNls
  private static final String DISPLAY_CHANNEL =
      "games.strategy.engine.framework.AbstractGame.DISPLAY_CHANNEL";

  @NonNls
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

  private final ClientNetworkBridge clientNetworkBridge;
  @Nullable private IDisplay display;
  @Nullable private ISound sound;

  @Nullable private ResourceLoader resourceLoader;

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

  @Override
  public void setResourceLoader(final ResourceLoader resourceLoader) {
    this.resourceLoader =
        Preconditions.checkNotNull(resourceLoader, "ResourceLoader needs to be non-null");
  }

  @Override
  public ResourceLoader getResourceLoader() {
    return Preconditions.checkNotNull(
        resourceLoader, "ResourceLoader has been accessed before setting it");
  }

  private void setupLocalPlayers(final Set<Player> localPlayers) {
    final PlayerList playerList = gameData.getPlayerList();
    for (final Player gp : localPlayers) {
      final GamePlayer player = playerList.getPlayerId(gp.getName());
      gamePlayers.put(player, gp);
      gp.initialize(new PlayerBridge(this), player);
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

      clientNetworkBridge.addListener(
          IDisplay.BombingResultsMessage.TYPE, message -> message.accept(display));
      clientNetworkBridge.addListener(
          IDisplay.NotifyRetreatMessage.TYPE,
          message -> message.accept(display, gameData.getPlayerList()));
      clientNetworkBridge.addListener(
          IDisplay.NotifyUnitsRetreatingMessage.TYPE,
          message -> message.accept(display, gameData.getUnits()));
      clientNetworkBridge.addListener(
          IDisplay.NotifyDiceMessage.TYPE, message -> message.accept(display));
      clientNetworkBridge.addListener(
          IDisplay.DisplayShutdownMessage.TYPE, message -> message.accept(display));
      clientNetworkBridge.addListener(
          IDisplay.GoToBattleStepMessage.TYPE, message -> message.accept(display));
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
