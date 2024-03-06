package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatPanel;
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
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.TripleADisplay;
import java.awt.Component;
import java.awt.Frame;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.client.HeadedGameRunner;
import org.triplea.java.Interruptibles;
import org.triplea.sound.DefaultSoundChannel;
import org.triplea.sound.SoundPath;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.util.LocalizeHtml;

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
  public Collection<PlayerTypes.Type> getPlayerTypes() {
    return HeadedPlayerTypes.getPlayerTypes();
  }

  @Override
  public void startGame(
      final LocalPlayers localPlayers,
      final IGame game,
      final Set<Player> players,
      final Chat chat) {
    final TripleAFrame frame =
        TripleAFrame.create(game, localPlayers, chat, HeadedGameRunner::clientLeftGame);

    SwingUtilities.invokeLater(
        () -> {
          LookAndFeelSwingFrameListener.register(frame);
          frame.setSize(700, 400);
          frame.setExtendedState(Frame.MAXIMIZED_BOTH);
          frame.setVisible(true);
          frame.toFront();
        });

    frame.getUiContext().getClipPlayer().play(SoundPath.CLIP_GAME_START);
    for (final Player player : players) {
      if (player instanceof TripleAPlayer) {
        ((TripleAPlayer) player).setFrame(frame);
      }
    }
    game.setDisplay(new TripleADisplay(frame));
    game.setSoundChannel(
        new DefaultSoundChannel(localPlayers, frame.getUiContext().getClipPlayer()));
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
  public ChatModel createChatModel(
      String chatName, Messengers messengers, ClientNetworkBridge clientNetworkBridge) {
    return ChatPanel.newChatPanel(
        messengers, chatName, ChatMessagePanel.ChatSoundProfile.GAME, clientNetworkBridge);
  }

  @Override
  public boolean shouldMinimizeExpensiveAiUse() {
    return false;
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

  @Override
  public IServerStartupRemote getStartupRemote(
      IServerStartupRemote.ServerModelView serverModelView) {
    return new HeadedServerStartupRemote(serverModelView);
  }

  @Override
  public boolean promptGameStop(String status, String title, @Nullable Path mapLocation) {
    // now tell the HOST, and see if they want to continue the game.
    String displayMessage =
        mapLocation == null ? status : LocalizeHtml.localizeImgLinksInHtml(status, mapLocation);
    if (displayMessage.endsWith("</body>")) {
      displayMessage =
          displayMessage.substring(0, displayMessage.length() - "</body>".length())
              + "</br><p>Do you want to continue?</p></body>";
    } else {
      displayMessage = displayMessage + "</br><p>Do you want to continue?</p>";
    }
    return !EventThreadJOptionPane.showConfirmDialog(
        null,
        "<html>" + displayMessage + "</html>",
        "Continue Game?  (" + title + ")",
        EventThreadJOptionPane.ConfirmDialogType.YES_NO);
  }

  @Override
  public PlayerTypes.Type getDefaultLocalPlayerType() {
    return HeadedPlayerTypes.HUMAN_PLAYER;
  }
}
