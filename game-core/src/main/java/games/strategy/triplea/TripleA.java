package games.strategy.triplea;

import java.awt.Frame;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.HeadlessUiContext;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.ui.display.TripleADisplay;

/**
 * Default implementation of {@link IGameLoader}.
 *
 * <p>
 * TODO: As there are no longer different game loader specializations, this class should be renamed to
 * {@code DefaultGameLoader} and moved to the {@code g.s.engine.data} package.
 * </p>
 */
public class TripleA implements IGameLoader {
  private static final long serialVersionUID = -8374315848374732436L;

  protected transient ITripleADisplay display;

  protected transient ISound soundChannel;
  protected transient IGame game;

  @Override
  public Set<IGamePlayer> newPlayers(final Map<String, PlayerType> playerNames) {
    return playerNames.entrySet()
        .stream()
        .map(TripleA::toGamePlayer)
        .collect(Collectors.toSet());
  }

  private static IGamePlayer toGamePlayer(final Map.Entry<String, PlayerType> namePlayerType) {
    return namePlayerType.getValue()
        .newPlayerWithName(namePlayerType.getKey());
  }

  @Override
  public void shutDown() {
    if (game != null && soundChannel != null) {
      game.removeSoundChannel(soundChannel);
      // set sound channel to null to handle the case of shutdown being called multiple times.
      // If/when shutdown is called exactly once, then the null assignment should be unnecessary.
      soundChannel = null;
    }

    if (display != null) {
      if (game != null) {
        game.removeDisplay(display);
      }
      display.shutDown();
      display = null;
    }
  }

  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players,
      final boolean headless, @Nullable final Chat chat) {
    this.game = game;
    if (game.getData().getDelegate("edit") == null) {
      // An evil hack: instead of modifying the XML, force an EditDelegate by adding one here
      final EditDelegate delegate = new EditDelegate();
      delegate.initialize("edit", "edit");
      game.getData().addDelegate(delegate);
      if (game instanceof ServerGame) {
        ((ServerGame) game).addDelegateMessenger(delegate);
      }
    }
    final LocalPlayers localPlayers = new LocalPlayers(players);
    if (headless) {
      final UiContext uiContext = new HeadlessUiContext();
      uiContext.setDefaultMapDir(game.getData());
      uiContext.setLocalPlayers(localPlayers);
      display = new HeadlessDisplay();
      soundChannel = new HeadlessSoundChannel();
      game.addDisplay(display);
      game.addSoundChannel(soundChannel);

      // technically not needed because we won't have any "local human players" in a headless game.
      connectPlayers(players, null);
    } else {
      final TripleAFrame frame = TripleAFrame.create(game, localPlayers, chat);

      SwingUtilities.invokeLater(() -> {
        LookAndFeelSwingFrameListener.register(frame);
        frame.setSize(700, 400);
        frame.setVisible(true);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.toFront();
      });

      display = new TripleADisplay(frame);
      game.addDisplay(display);
      soundChannel = new DefaultSoundChannel(localPlayers);
      game.addSoundChannel(soundChannel);
      ClipPlayer.play(SoundPath.CLIP_GAME_START);
      connectPlayers(players, frame);

    }
  }

  private static void connectPlayers(final Set<IGamePlayer> players, final TripleAFrame frame) {
    for (final IGamePlayer player : players) {
      if (player instanceof TripleAPlayer) {
        ((TripleAPlayer) player).setFrame(frame);
      }
    }
  }

  @Override
  public Class<? extends IChannelSubscriber> getDisplayType() {
    return ITripleADisplay.class;
  }

  @Override
  public Class<? extends IChannelSubscriber> getSoundType() {
    return ISound.class;
  }

  @Override
  public Class<? extends IRemote> getRemotePlayerType() {
    return ITripleAPlayer.class;
  }

  @Override
  public Unit newUnit(final UnitType type, final PlayerId owner, final GameData data) {
    return new TripleAUnit(type, owner, data);
  }
}
