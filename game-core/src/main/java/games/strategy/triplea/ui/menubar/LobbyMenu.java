package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.google.common.base.Strings;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.client.login.LobbyLoginPreferences;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.client.ui.MacLobbyWrapper;
import games.strategy.engine.lobby.client.ui.TimespanDialog;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.IUserManager;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.engine.lobby.server.login.RsaAuthenticator;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.Node;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;

/**
 * The lobby client menu bar.
 */
public final class LobbyMenu extends JMenuBar {
  private static final long serialVersionUID = 4980621864542042057L;

  private final LobbyFrame lobbyFrame;

  public LobbyMenu(final LobbyFrame frame) {
    lobbyFrame = frame;
    // file only has one value
    // and on mac it is in the apple menu
    if (!SystemProperties.isMac()) {
      createFileMenu(this);
    } else {
      MacLobbyWrapper.registerMacShutdownHandler(lobbyFrame);
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
    createDiagnosticsMenu(powerUser);
    createToolboxMenu(powerUser);
  }

  private void createDiagnosticsMenu(final JMenu menuBar) {
    final JMenu diagnostics = new JMenu("Diagnostics");
    menuBar.add(diagnostics);
    addDisplayPlayersInformationMenu(diagnostics);
  }

  private void createToolboxMenu(final JMenu menuBar) {
    final JMenu toolbox = new JMenu("Toolbox");
    menuBar.add(toolbox);
    addBanUsernameMenu(toolbox);
    addBanMacAddressMenu(toolbox);
    addUnbanUsernameMenu(toolbox);
    addUnbanMacAddressMenu(toolbox);
  }

  private void addDisplayPlayersInformationMenu(final JMenu parentMenu) {
    final JMenuItem revive = new JMenuItem("Display Players Information");
    revive.setEnabled(true);
    revive.addActionListener(event -> {
      new Thread(() -> {
        final IModeratorController controller = (IModeratorController) lobbyFrame.getLobbyClient().getMessengers()
            .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
        final StringBuilder builder = new StringBuilder();
        builder.append("Online Players:\r\n\r\n");
        for (final INode player : lobbyFrame.getChatMessagePanel().getChat().getOnlinePlayers()) {
          builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
        }
        builder.append("Players That Have Left (Last 10):\r\n\r\n");
        for (final INode player : lobbyFrame.getChatMessagePanel().getChat().getPlayersThatLeft_Last10()) {
          builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
        }
        SwingUtilities.invokeLater(() -> {
          final JDialog dialog = new JDialog(lobbyFrame, "Players Information");
          final JTextArea label = new JTextArea(builder.toString());
          label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
          label.setEditable(false);
          label.setAutoscrolls(true);
          label.setLineWrap(false);
          label.setFocusable(true);
          label.setWrapStyleWord(true);
          label.setLocation(0, 0);
          dialog.setBackground(label.getBackground());
          dialog.setLayout(new BorderLayout());
          final JScrollPane pane = new JScrollPane();
          pane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          pane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          pane.setViewportView(label);
          dialog.add(pane, BorderLayout.CENTER);
          final JButton button = new JButton("Close");
          button.addActionListener(e -> dialog.dispose());
          button.setMinimumSize(new Dimension(100, 30));
          dialog.add(button, BorderLayout.SOUTH);
          dialog.setMinimumSize(new Dimension(500, 300));
          dialog.setSize(new Dimension(800, 600));
          dialog.setResizable(true);
          dialog.setLocationRelativeTo(lobbyFrame);
          dialog.setDefaultCloseOperation(2);
          dialog.setVisible(true);
        });
      }).start();
    });
    parentMenu.add(revive);
  }

  private void addBanUsernameMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Ban Username");
    item.addActionListener(e -> {
      final String name = JOptionPane.showInputDialog(null,
          "Enter the username that you want to ban from the lobby.\r\n\r\n"
              + "Note that this ban is effective on any username, registered or anonymous, online or offline.",
          "");
      if ((name == null) || (name.length() < 1)) {
        return;
      }
      if (!DBUser.isValidUserName(name)) {
        JOptionPane.showMessageDialog(lobbyFrame, "The username you entered is invalid.", "Invalid Username",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      TimespanDialog.prompt(lobbyFrame, "Select Timespan",
          "Please consult other admins before banning longer than 1 day.", date -> {
            final IModeratorController controller = (IModeratorController) lobbyFrame.getLobbyClient().getMessengers()
                .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
            controller.banUsername(newDummyNode(name), date);
          });
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private static INode newDummyNode(final String name) {
    try {
      return new Node(name, InetAddress.getByAddress(new byte[] {0, 0, 0, 0}), 0);
    } catch (final UnknownHostException e) {
      throw new AssertionError("should never happen");
    }
  }

  private void addBanMacAddressMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Ban Hashed Mac Address");
    item.addActionListener(e -> {
      final String mac =
          JOptionPane.showInputDialog(null, "Enter the hashed Mac Address that you want to ban from the lobby.\r\n\r\n"
              + "Hashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3", "");
      if ((mac == null) || (mac.length() < 1)) {
        return;
      }
      if (!MacFinder.isValidHashedMacAddress(mac)) {
        JOptionPane.showMessageDialog(lobbyFrame, "The hashed Mac Address you entered is invalid.",
            "Invalid Hashed Mac", JOptionPane.ERROR_MESSAGE);
        return;
      }
      TimespanDialog.prompt(lobbyFrame, "Select Timespan",
          "Please consult other admins before banning longer than 1 day.", date -> {
            final IModeratorController controller = (IModeratorController) lobbyFrame.getLobbyClient().getMessengers()
                .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
            controller.banMac(newDummyNode("__unknown__"), mac, date);
          });
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private void addUnbanUsernameMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Unban Username");
    item.addActionListener(e -> {
      final String name =
          JOptionPane.showInputDialog(null, "Enter the username that you want to unban from the lobby.", "");
      if ((name == null) || (name.length() < 1)) {
        return;
      }
      if (!DBUser.isValidUserName(name)) {
        JOptionPane.showMessageDialog(lobbyFrame, "The username you entered is invalid.", "Invalid Username",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      final IModeratorController controller = (IModeratorController) lobbyFrame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      controller.banUsername(newDummyNode(name), Date.from(Instant.EPOCH));
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private void addUnbanMacAddressMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Unban Hashed Mac Address");
    item.addActionListener(e -> {
      final String mac =
          JOptionPane.showInputDialog(null, "Enter the hashed Mac Address that you want to unban from the lobby.\n\n"
              + "Hashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3", "");
      if ((mac == null) || (mac.length() < 1)) {
        return;
      }
      if (!MacFinder.isValidHashedMacAddress(mac)) {
        JOptionPane.showMessageDialog(lobbyFrame, "The hashed Mac Address you entered is invalid.",
            "Invalid Hashed Mac", JOptionPane.ERROR_MESSAGE);
        return;
      }
      final IModeratorController controller = (IModeratorController) lobbyFrame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      controller.banMac(newDummyNode("__unknown__"), mac, Date.from(Instant.EPOCH));
    });
    item.setEnabled(true);
    parentMenu.add(item);
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
    final JMenuItem hostingLink = new JMenuItem("How to host");
    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.HOSTING_GUIDE));
    parentMenu.add(hostingLink);

    final JMenuItem helpPageLink = new JMenuItem("Help Page");
    helpPageLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP));
    parentMenu.add(helpPageLink);

    final JMenuItem lobbyRules = new JMenuItem("Lobby Rules");
    lobbyRules.addActionListener(
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_LOBBY_RULES));
    parentMenu.add(lobbyRules);

    final JMenuItem warClub = new JMenuItem("TripleA Forum");
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_FORUM));
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
    final IUserManager manager =
        (IUserManager) lobbyFrame.getLobbyClient().getRemoteMessenger().getRemote(IUserManager.USER_MANAGER);
    final DBUser user = manager.getUserInfo(lobbyFrame.getLobbyClient().getMessenger().getLocalNode().getName());
    if (user == null) {
      JOptionPane.showMessageDialog(this, "No user info found", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    final CreateUpdateAccountPanel panel = CreateUpdateAccountPanel.newUpdatePanel(user, LobbyLoginPreferences.load());
    final CreateUpdateAccountPanel.ReturnValue returnValue = panel.show(lobbyFrame);
    if (returnValue == CreateUpdateAccountPanel.ReturnValue.CANCEL) {
      return;
    }
    final String error = Strings.emptyToNull(""
        + Strings.nullToEmpty(manager.updateUser(
            panel.getUserName(),
            panel.getEmail(),
            games.strategy.util.Md5Crypt.crypt(panel.getPassword())))
        + Strings.nullToEmpty(manager.updateUser(
            panel.getUserName(),
            panel.getEmail(),
            RsaAuthenticator.hashPasswordWithSalt(panel.getPassword()))));
    if (error != null) {
      JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
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
      final JMenuItem menuFileExit = new JMenuItem(SwingAction.of("Exit", e -> lobbyFrame.shutdown()));
      parentMenu.add(menuFileExit);
    }
  }
}
