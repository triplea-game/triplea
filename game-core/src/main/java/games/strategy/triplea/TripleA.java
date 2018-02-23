package games.strategy.triplea;

import java.awt.Frame;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.ai.fastAI.FastAi;
import games.strategy.triplea.ai.proAI.ProAi;
import games.strategy.triplea.ai.weakAI.DoesNothingAi;
import games.strategy.triplea.ai.weakAI.WeakAi;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.HeadlessUiContext;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.ui.display.TripleADisplay;
import games.strategy.ui.SwingAction;

@MapSupport
public class TripleA implements IGameLoader {
  private static final long serialVersionUID = -8374315848374732436L;
  public static final String HUMAN_PLAYER_TYPE = "Human";
  public static final String WEAK_COMPUTER_PLAYER_TYPE = "Easy (AI)";
  public static final String FAST_COMPUTER_PLAYER_TYPE = "Fast (AI)";
  public static final String PRO_COMPUTER_PLAYER_TYPE = "Hard (AI)";
  public static final String DOESNOTHINGAI_COMPUTER_PLAYER_TYPE = "Does Nothing (AI)";
  protected transient ITripleADisplay display;

  protected transient ISound soundChannel;
  protected transient IGame game;

  @Override
  public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames) {
    final Set<IGamePlayer> players = new HashSet<>();
    for (final String name : playerNames.keySet()) {
      final String type = playerNames.get(name);
      if (type.equals(WEAK_COMPUTER_PLAYER_TYPE)) {
        players.add(new WeakAi(name, type));
      } else if (type.equals(FAST_COMPUTER_PLAYER_TYPE)) {
        players.add(new FastAi(name, type));
      } else if (type.equals(PRO_COMPUTER_PLAYER_TYPE)) {
        players.add(new ProAi(name, type));
      } else if (type.equals(DOESNOTHINGAI_COMPUTER_PLAYER_TYPE)) {
        players.add(new DoesNothingAi(name, type));
      } else if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE)) {
        final TripleAPlayer player = new TripleAPlayer(name, type);
        players.add(player);
      } else {
        throw new IllegalStateException("Player type not recognized:" + type);
      }
    }
    return players;
  }

  @Override
  public void shutDown() {
    if ((game != null) && (soundChannel != null)) {
      game.removeSoundChannel(soundChannel);
      // set sound channel to null to handle the case of shutdown being called multiple times.
      // If/when shutdown is called exactly once, then the null assignment should be unnecessary.
      soundChannel = null;
    }

    if (display != null) {
      game.removeDisplay(display);
      display.shutDown();
      display = null;
    }
  }

  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless)
      throws InterruptedException {
    this.game = game;
    if (game.getData().getDelegateList().getDelegate("edit") == null) {
      // An evil hack: instead of modifying the XML, force an EditDelegate by adding one here
      final EditDelegate delegate = new EditDelegate();
      delegate.initialize("edit", "edit");
      game.getData().getDelegateList().addDelegate(delegate);
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
      SwingAction.invokeAndWait(() -> {
        final TripleAFrame frame = new TripleAFrame(game, localPlayers);
        display = new TripleADisplay(frame);
        game.addDisplay(display);
        soundChannel = new DefaultSoundChannel(localPlayers);
        game.addSoundChannel(soundChannel);
        frame.setSize(700, 400);
        frame.setVisible(true);
        ClipPlayer.play(SoundPath.CLIP_GAME_START);
        connectPlayers(players, frame);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.toFront();
      });
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
  public String[] getServerPlayerTypes() {
    return new String[] {HUMAN_PLAYER_TYPE, WEAK_COMPUTER_PLAYER_TYPE, FAST_COMPUTER_PLAYER_TYPE,
        PRO_COMPUTER_PLAYER_TYPE, DOESNOTHINGAI_COMPUTER_PLAYER_TYPE};
  }

  @Override
  public Class<? extends IChannelSubscribor> getDisplayType() {
    return ITripleADisplay.class;
  }

  @Override
  public Class<? extends IChannelSubscribor> getSoundType() {
    return ISound.class;
  }

  @Override
  public Class<? extends IRemote> getRemotePlayerType() {
    return ITripleAPlayer.class;
  }

  @Override
  public IUnitFactory getUnitFactory() {
    return new IUnitFactory() {
      private static final long serialVersionUID = 5684926837825766505L;

      @Override
      public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data) {
        return new TripleAUnit(type, owner, data);
      }
    };
  }

  @Override
  public String toString() {
    return "TripleA[]";
  }
}
