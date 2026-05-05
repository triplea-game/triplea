package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.ChatMessagePanel;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPlayerPanel;
import games.strategy.triplea.EngineImageLoader;
import games.strategy.triplea.ui.QuitHandler;
import games.strategy.triplea.ui.menubar.LobbyMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import lombok.Getter;
import org.triplea.game.client.HeadedGameRunner;
import org.triplea.http.client.web.socket.WebSocket;
import org.triplea.sound.ClipPlayer;
import org.triplea.swing.SwingComponents;

/** The top-level frame window for the lobby client UI. */
public class LobbyFrame extends JFrame implements QuitHandler {
  private static final long serialVersionUID = -388371674076362572L;

  @Getter private final LobbyModel lobbyModel;

  public LobbyFrame(final LobbyModel lobbyModel) {
    super("TripleA Lobby");
    this.lobbyModel = lobbyModel;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    SwingComponents.addWindowClosedListener(this, HeadedGameRunner::exitGameIfNoWindowsVisible);
    setIconImage(EngineImageLoader.loadFrameIcon());

    final var loginResult = lobbyModel.getLoginResult();
    final var connection = lobbyModel.getConnection();

    setJMenuBar(new LobbyMenu(this, loginResult, connection));
    connection.addConnectionClosedListener(this::shutdown);
    final var gameListingModel = lobbyModel.getGameListingModel();
    final var reconnectOverlay = new ReconnectOverlay(this, this::shutdown);
    connection.addReconnectionListener(
        new WebSocket.ReconnectionHandler() {
          @Override
          public void onReconnecting(final int attempt) {
            reconnectOverlay.show(attempt);
          }

          @Override
          public void onReconnected() {
            gameListingModel.refresh();
            reconnectOverlay.dismiss();
          }
        });

    final ChatMessagePanel chatMessagePanel =
        new ChatMessagePanel(lobbyModel.getChat(), ChatSoundProfile.LOBBY, new ClipPlayer());
    Optional.ofNullable(loginResult.getLoginMessage())
        .ifPresent(chatMessagePanel::addServerMessage);

    final ChatPlayerPanel chatPlayers = new ChatPlayerPanel(lobbyModel.getChat());
    chatPlayers.setPreferredSize(new Dimension(200, 600));
    chatPlayers.addActionFactory(
        clickedChatter ->
            LobbyPlayerActions.buildFor(this, loginResult, clickedChatter, connection));

    final var tableModel = new LobbyGameTableModel(loginResult.isModerator(), gameListingModel);
    final LobbyGamePanel gamePanel =
        new LobbyGamePanel(this, loginResult, tableModel, gameListingModel);

    final JSplitPane leftSplit = new JSplitPane();
    leftSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    leftSplit.setTopComponent(gamePanel);
    leftSplit.setBottomComponent(chatMessagePanel);
    leftSplit.setResizeWeight(0.5);
    gamePanel.setPreferredSize(new Dimension(700, 200));
    chatMessagePanel.setPreferredSize(new Dimension(700, 400));
    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(leftSplit);
    mainSplit.setRightComponent(chatPlayers);
    mainSplit.setResizeWeight(1);
    add(mainSplit, BorderLayout.CENTER);
    pack();
    chatMessagePanel.requestFocusInWindow();
    setLocationRelativeTo(null);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            shutdown();
          }
        });
  }

  @Override
  public boolean shutdown() {
    setVisible(false);
    dispose();
    lobbyModel.shutdown();
    return true;
  }
}
