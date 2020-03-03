package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.player.Player;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.TripleADisplay;
import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.triplea.sound.ClipPlayer;
import org.triplea.sound.DefaultSoundChannel;
import org.triplea.sound.ISound;
import org.triplea.sound.SoundPath;

/**
 * Headed and default implementation of {@link LaunchAction}. Ideally replaceable with any other
 * graphics framework.
 */
public class HeadedLaunchAction implements LaunchAction {

  private final Component ui;

  public HeadedLaunchAction(final Component ui) {
    this.ui = ui;
  }

  @Override
  public void handleGameInterruption(
      final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
    gameSelectorModel.onGameEnded();
  }

  @Override
  public void onGameInterrupt() {
    SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(ui).setVisible(true));
  }

  @Override
  public void onEnd(final String message) {
    SwingUtilities.invokeLater(
        () -> JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message));
  }

  @Override
  public IDisplay startGame(
      final LocalPlayers localPlayers,
      final IGame game,
      final Set<Player> players,
      final Chat chat) {
    final TripleAFrame frame = TripleAFrame.create(game, localPlayers, chat);

    SwingUtilities.invokeLater(
        () -> {
          LookAndFeelSwingFrameListener.register(frame);
          frame.setSize(700, 400);
          frame.setVisible(true);
          frame.setExtendedState(Frame.MAXIMIZED_BOTH);
          frame.toFront();
        });

    ClipPlayer.play(SoundPath.CLIP_GAME_START);
    for (final Player player : players) {
      if (player instanceof TripleAPlayer) {
        ((TripleAPlayer) player).setFrame(frame);
      }
    }
    return new TripleADisplay(frame);
  }

  @Override
  public ISound getSoundChannel(final LocalPlayers localPlayers) {
    return new DefaultSoundChannel(localPlayers);
  }

  @Override
  public File getAutoSaveFile() {
    return getAutoSaveFileUtils()
        .getLostConnectionAutoSaveFile(LocalDateTime.now(ZoneId.systemDefault()));
  }

  @Override
  public void onLaunch(final ServerGame serverGame) {}

  @Override
  public AutoSaveFileUtils getAutoSaveFileUtils() {
    return new AutoSaveFileUtils();
  }
}
