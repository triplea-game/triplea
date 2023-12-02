package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.lobby.client.login.ChangeEmailPanel;
import games.strategy.engine.lobby.client.login.ChangePasswordPanel;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.moderator.toolbox.ToolBoxWindow;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import javax.swing.JMenuBar;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.sound.SoundOptions;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemCheckBoxBuilder;
import org.triplea.swing.SwingComponents;

/** The lobby client menu bar. */
public final class LobbyMenu extends JMenuBar {
  private static final long serialVersionUID = 4980621864542042057L;

  private final LobbyFrame lobbyFrame;

  public LobbyMenu(
      LobbyFrame frame, LoginResult loginResult, PlayerToLobbyConnection playerToLobbyConnection) {
    lobbyFrame = frame;
    // file only has one value, and on mac it is in the apple menu
    if (!SystemProperties.isMac()) {
      add(new JMenuBuilder("File", 'F').addMenuItem("Exit", 'X', lobbyFrame::shutdown).build());
    } else {
      MacOsIntegration.setQuitHandler(lobbyFrame);
    }

    if (!loginResult.isAnonymousLogin()) {
      add(
          new JMenuBuilder("Account", 'A')
              .addMenuItem(
                  "Update Email",
                  'E',
                  () -> ChangeEmailPanel.promptUserForNewEmail(frame, playerToLobbyConnection))
              .addMenuItem(
                  "Update Password",
                  'P',
                  () ->
                      ChangePasswordPanel.doPasswordChange(
                          frame,
                          playerToLobbyConnection,
                          ChangePasswordPanel.AllowCancelMode.SHOW_CANCEL_BUTTON))
              .build());
    }

    if (loginResult.isModerator()) {
      add(
          new JMenuBuilder("Admin", 'M')
              .addMenuItem(
                  "Open Toolbox",
                  'T',
                  () ->
                      ToolBoxWindow.showWindow(
                          lobbyFrame, playerToLobbyConnection.getHttpModeratorToolboxClient()))
              .build());
    }

    add(
        new JMenuBuilder("Settings", 'S')
            .addMenuItem(SoundOptions.buildGlobalSoundSwitchMenuItem())
            .addMenuItem(SoundOptions.buildSoundOptionsMenuItem())
            .addMenuItem(
                new JMenuItemCheckBoxBuilder("Show Chat Times", 'C')
                    .bindSetting(ClientSetting.showChatTimeSettings)
                    .build())
            .build());

    add(
        new JMenuBuilder("Help", 'H')
            .addMenuItem(
                "User Guide",
                'U',
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE))
            .addMenuItem(
                "View Forums",
                'F',
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_FORUM))
            .addMenuItem(
                "Send Bug Report",
                'B',
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES))
            .build());
  }
}
