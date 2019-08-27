package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.client.login.LobbyLoginPreferences;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.moderator.toolbox.ShowToolboxController;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.MacOsIntegration;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.triplea.lobby.common.IUserManager;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/** The lobby client menu bar. */
public final class LobbyMenu extends JMenuBar {
  private static final long serialVersionUID = 4980621864542042057L;

  private final LobbyFrame lobbyFrame;

  public LobbyMenu(final LobbyFrame frame) {
    lobbyFrame = frame;
    // file only has one value, and on mac it is in the apple menu
    if (!SystemProperties.isMac()) {
      createFileMenu(this);
    } else {
      MacOsIntegration.addQuitHandler(lobbyFrame::shutdown);
    }
    createAccountMenu(this);
    if (lobbyFrame.getLobbyClient().isAdmin()) {
      createAdminMenu(this);
    }
    createSettingsMenu(this);
    createHelpMenu(this);
  }

  private void createAccountMenu(final LobbyMenu menuBar) {
    final JMenu account = new JMenu("Account");
    menuBar.add(account);
    addUpdateAccountMenu(account);
  }

  private void createAdminMenu(final LobbyMenu menuBar) {
    final JMenu powerUser = new JMenu("Admin");
    menuBar.add(powerUser);
    createToolBoxWindowMenu(powerUser);
  }

  private void createToolBoxWindowMenu(final JMenu menuBar) {
    final JMenuItem menuItem = new JMenuItem("Open Toolbox");
    menuItem.addActionListener(event -> ShowToolboxController.showToolbox(lobbyFrame));
    menuBar.add(menuItem);
  }

  private void showErrorDialog(final String message, final String title) {
    JOptionPane.showMessageDialog(lobbyFrame, message, title, JOptionPane.ERROR_MESSAGE);
  }

  private void createSettingsMenu(final LobbyMenu menuBar) {
    final JMenu settings = new JMenu("Settings");
    menuBar.add(settings);
    SoundOptions.addGlobalSoundSwitchMenu(settings);
    SoundOptions.addToMenu(settings);
    addChatTimeMenu(settings);
  }

  private static void createHelpMenu(final LobbyMenu menuBar) {
    final JMenu help = new JMenu("Help");
    menuBar.add(help);
    addHelpMenu(help);
  }

  private static void addHelpMenu(final JMenu parentMenu) {
    final JMenuItem hostingLink = new JMenuItem("User Guide");
    hostingLink.addActionListener(
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE));
    parentMenu.add(hostingLink);

    final JMenuItem warClub = new JMenuItem("TripleA Forum");
    warClub.addActionListener(
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_FORUM));
    parentMenu.add(warClub);
  }

  private void addChatTimeMenu(final JMenu parentMenu) {
    final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
    chatTimeBox.addActionListener(e -> lobbyFrame.setShowChatTime(chatTimeBox.isSelected()));
    chatTimeBox.setSelected(true);
    parentMenu.add(chatTimeBox);
  }

  private void addUpdateAccountMenu(final JMenu account) {
    final JMenuItem update = new JMenuItem("Update Account...");
    // only if we are not anonymous login
    update.setEnabled(!lobbyFrame.getLobbyClient().isAnonymousLogin());
    update.addActionListener(e -> updateAccountDetails());
    account.add(update);
  }

  private void updateAccountDetails() {
    final IUserManager manager = lobbyFrame.getLobbyClient().getUserManager();
    final String username = lobbyFrame.getLobbyClient().getMessengers().getLocalNode().getName();
    final String email = manager.getUserEmail(username);
    if (email == null) {
      showErrorDialog("No user info found", "Error");
      return;
    }

    final CreateUpdateAccountPanel panel =
        CreateUpdateAccountPanel.newUpdatePanel(username, email, LobbyLoginPreferences.load());
    final CreateUpdateAccountPanel.ReturnValue returnValue = panel.show(lobbyFrame);
    if (returnValue == CreateUpdateAccountPanel.ReturnValue.CANCEL) {
      return;
    }
    final String error =
        manager.updateUser(
            username, email, RsaAuthenticator.hashPasswordWithSalt(panel.getPassword()));
    if (error != null) {
      showErrorDialog(error, "Error");
      return;
    }

    panel.getLobbyLoginPreferences().save();
  }

  private void createFileMenu(final JMenuBar menuBar) {
    final JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);
    addExitMenu(fileMenu);
  }

  private void addExitMenu(final JMenu parentMenu) {
    final boolean isMac = SystemProperties.isMac();
    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (!isMac) { // On non-Mac operating systems, we need to manually create an Exit menu item
      final JMenuItem menuFileExit =
          new JMenuItem(SwingAction.of("Exit", e -> lobbyFrame.shutdown()));
      parentMenu.add(menuFileExit);
    }
  }
}
