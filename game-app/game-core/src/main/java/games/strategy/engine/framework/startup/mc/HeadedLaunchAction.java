package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.net.Messengers;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.TripleADisplay;
import java.awt.Component;
import java.awt.Frame;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.java.Interruptibles;
import org.triplea.sound.ClipPlayer;
import org.triplea.sound.DefaultSoundChannel;
import org.triplea.sound.ISound;
import org.triplea.sound.SoundPath;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * Headed and default implementation of {@link LaunchAction}. Ideally replaceable with any other
 * graphics framework.
 */
@Slf4j
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
  public Path getAutoSaveFile() {
    return getAutoSaveFileUtils()
        .getLostConnectionAutoSaveFile(LocalDateTime.now(ZoneId.systemDefault()));
  }

  @Override
  public void onLaunch(final ServerGame serverGame) {}

  @Override
  public AutoSaveFileUtils getAutoSaveFileUtils() {
    return new AutoSaveFileUtils();
  }

  @Override
  public ChatModel createChatModel(String chatName, Messengers messengers) {
    return ChatPanel.newChatPanel(messengers, chatName, ChatMessagePanel.ChatSoundProfile.GAME);
  }

  @Override
  public PlayerTypes.Type getDefaultPlayerType() {
    return PlayerTypes.HUMAN_PLAYER;
  }

  @Override
  public WatcherThreadMessaging createThreadMessaging() {
    return new WatcherThreadMessaging.HeadedWatcherThreadMessaging(ui);
  }

  @Override
  public Optional<ServerConnectionProps> getFallbackConnection(Runnable cancelAction) {

    final UserName userName = UserName.of(ClientSetting.playerName.getValueOrThrow());
    final Interruptibles.Result<ServerOptions> optionsResult =
        Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final ServerOptions options =
                          new ServerOptions(ui, userName, GameRunner.PORT, false);
                      options.setLocationRelativeTo(ui);
                      options.setVisible(true);
                      options.dispose();
                      if (!options.getOkPressed()) {
                        return null;
                      }
                      final String name = options.getName();
                      log.debug("Server playing as:" + name);
                      ClientSetting.playerName.setValue(name);
                      ClientSetting.flush();
                      final int port = options.getPort();
                      if (port >= 65536 || port == 0) {
                        if (HeadlessGameServer.headless()) {
                          throw new IllegalStateException("Invalid Port: " + port);
                        }
                        JOptionPane.showMessageDialog(
                            ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                      }
                      return options;
                    }));
    if (!optionsResult.completed) {
      throw new IllegalArgumentException("Error while gathering connection details");
    }
    if (optionsResult.result.isEmpty()) {
      cancelAction.run();
    }
    return optionsResult.result.map(
        options ->
            ServerConnectionProps.builder()
                .name(options.getName())
                .port(options.getPort())
                .password(options.getPassword())
                .build());
  }

  @Override
  public void handleError(String error) {
    SwingComponents.showError(null, "Connection problem", error);
  }
}
