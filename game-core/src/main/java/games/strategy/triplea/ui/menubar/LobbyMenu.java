package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.moderator.toolbox.ToolBoxWindow;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import org.triplea.lobby.common.IUserManager;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemCheckBoxBuilder;
import org.triplea.swing.SwingComponents;

/** The lobby client menu bar. */
public final class LobbyMenu extends JMenuBar {
  private static final long serialVersionUID = 4980621864542042057L;

  private final LobbyFrame lobbyFrame;

  public LobbyMenu(final LobbyFrame frame) {
    lobbyFrame = frame;
    // file only has one value, and on mac it is in the apple menu
    if (!SystemProperties.isMac()) {
      add(new JMenuBuilder("File", 'F').addMenuItem("Exit", 'X', lobbyFrame::shutdown).build());
    } else {
      MacOsIntegration.addQuitHandler(lobbyFrame::shutdown);
    }

    if (!lobbyFrame.getLobbyClient().isAnonymousLogin()) {
      add(
          new JMenuBuilder("Account", 'A')
              .addMenuItem("Update Account...", 'U', this::updateAccountDetails)
              .build());
    }

    if (lobbyFrame.getLobbyClient().isAdmin()) {
      add(
          new JMenuBuilder("Admin", 'M')
              .addMenuItem(
                  "Open Toolbox",
                  'T',
                  () ->
                      ToolBoxWindow.showWindow(
                          lobbyFrame,
                          lobbyFrame
                              .getLobbyClient()
                              .getHttpLobbyClient()
                              .getHttpModeratorToolboxClient()))
              .build());
    }

    add(
        new JMenuBuilder("Settings", 'S')
            .addMenuItem(SoundOptions.buildGlobalSoundSwitchMenuItem())
            .addMenuItem(SoundOptions.buildSoundOptionsMenuItem())
            .addMenuItem(
                new JMenuItemCheckBoxBuilder("Show Chat Times", 'C')
                    .bindSetting(ClientSetting.showChatTimeSettings)
                    .actionListener(lobbyFrame::setShowChatTime)
                    .build())
            .build());

    add(
        new JMenuBuilder("Help", 'H')
            .addMenuItem(
                "User Guide",
                'U',
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE))
            .addMenuItem(
                "TripleA Forum",
                'F',
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_FORUM))
            .build());
  }

  private void updateAccountDetails() {
    final IUserManager manager = lobbyFrame.getLobbyClient().getUserManager();
    final String username = lobbyFrame.getLobbyClient().getMessengers().getLocalNode().getName();
    final String email = manager.getUserEmail(username);
    if (email == null) {
      showErrorDialog("No user info found");
      return;
    }

    final CreateUpdateAccountPanel panel = CreateUpdateAccountPanel.newUpdatePanel(username, email);
    final CreateUpdateAccountPanel.ReturnValue returnValue = panel.show(lobbyFrame);
    if (returnValue == CreateUpdateAccountPanel.ReturnValue.CANCEL) {
      return;
    }
    final String error =
        manager.updateUser(
            username, email, RsaAuthenticator.hashPasswordWithSalt(panel.getPassword()));
    if (error != null) {
      showErrorDialog(error);
      return;
    }
  }

  private void showErrorDialog(final String message) {
    JOptionPane.showMessageDialog(lobbyFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
  }
}
