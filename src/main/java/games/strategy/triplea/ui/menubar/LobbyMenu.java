package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.client.ui.MacLobbyWrapper;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.IUserManager;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.lobby.server.userDB.UserDao;
import games.strategy.net.INode;
import games.strategy.net.Node;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.MD5Crypt;

public class LobbyMenu extends JMenuBar {
  private static final long serialVersionUID = 4980621864542042057L;
  private final LobbyFrame m_frame;

  public LobbyMenu(final LobbyFrame frame) {
    m_frame = frame;
    // file only has one value
    // and on mac it is in the apple menu
    if (!SystemProperties.isMac()) {
      createFileMenu(this);
    } else {
      MacLobbyWrapper.registerMacShutdownHandler(m_frame);
    }
    createAccountMenu(this);
    if (m_frame.getLobbyClient().isAdmin()) {
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
      final Runnable runner = () -> {
        final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers()
            .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
        final StringBuilder builder = new StringBuilder();
        builder.append("Online Players:\r\n\r\n");
        for (final INode player : m_frame.getChatMessagePanel().getChat().getOnlinePlayers()) {
          builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
        }
        builder.append("Players That Have Left (Last 10):\r\n\r\n");
        for (final INode player : m_frame.getChatMessagePanel().getChat().getPlayersThatLeft_Last10()) {
          builder.append(controller.getInformationOn(player)).append("\r\n\r\n");
        }
        final Runnable componentCreation = () -> {
          final JDialog dialog = new JDialog(m_frame, "Players Information");
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
          final JButton button = new JButton(SwingAction.of(e -> dialog.dispose()));
          button.setText("Close");
          button.setMinimumSize(new Dimension(100, 30));
          dialog.add(button, BorderLayout.SOUTH);
          dialog.setMinimumSize(new Dimension(500, 300));
          dialog.setSize(new Dimension(800, 600));
          dialog.setResizable(true);
          dialog.setLocationRelativeTo(m_frame);
          dialog.setDefaultCloseOperation(2);
          dialog.setVisible(true);
        };
        SwingUtilities.invokeLater(componentCreation);
      };
      final Thread thread = new Thread(runner);
      thread.start();
    });
    parentMenu.add(revive);
  }

  private void addBanUsernameMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Ban Username");
    item.addActionListener(e -> {
      final String name1 = JOptionPane.showInputDialog(null,
          "Enter the username that you want to ban from the lobby.\r\n\r\n"
              + "Note that this ban is effective on any username, registered or anonymous, online or offline.",
          "");
      if (name1 == null || name1.length() < 1) {
        return;
      }
      if (UserDao.validateUserName(name1) != null) {
        if (JOptionPane.showConfirmDialog(m_frame,
            "The username you entered is invalid. Do you want to ban it anyhow?", "Invalid Username",
            JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }
      final long ticks = requestTimespanSupplication();
      final long expire = System.currentTimeMillis() + ticks;
      final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      try {
        controller.banUsername(new Node(name1, InetAddress.getByName("0.0.0.0"), 0), new Date(expire));
      } catch (final UnknownHostException ex) {
        ClientLogger.logQuietly(ex);
      }
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private void addBanMacAddressMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Ban Hashed Mac Address");
    item.addActionListener(e -> {
      final String mac = JOptionPane.showInputDialog(null,
          "Enter the hashed Mac Address that you want to ban from the lobby.\r\n\r\n"
              + "Hashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3",
          "");
      if (mac == null || mac.length() < 1) {
        return;
      }
      final String prefix = MD5Crypt.MAGIC + "MH$";
      final String error;
      if (mac.length() != 28) {
        error = "Must be 28 characters long";
      } else if (!mac.startsWith(prefix)) {
        error = "Must start with: " + prefix;
      } else if (!mac.matches("[0-9a-zA-Z$./]+")) {
        error = "Must use only these characters: 0-9a-zA-Z$./";
      } else {
        error = null;
      }
      if (error != null) {
        if (JOptionPane.showConfirmDialog(m_frame,
            "The hashed Mac Address you entered is invalid (" + error + "). Do you want to ban it anyhow?",
            "Invalid Hashed Mac", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }
      final long ticks = requestTimespanSupplication();
      final long expire = System.currentTimeMillis() + ticks;
      final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      try {
        controller.banMac(new Node("None (Admin menu originated ban)", InetAddress.getByName("0.0.0.0"), 0), mac,
            new Date(expire));
      } catch (final UnknownHostException ex) {
        ClientLogger.logQuietly(ex);
      }
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private void addUnbanUsernameMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Unban Username");
    item.addActionListener(e -> {
      final String name1 =
          JOptionPane.showInputDialog(null, "Enter the username that you want to unban from the lobby.", "");
      if (name1 == null || name1.length() < 1) {
        return;
      }
      if (UserDao.validateUserName(name1) != null) {
        if (JOptionPane.showConfirmDialog(m_frame,
            "The username you entered is invalid. Do you want to ban it anyhow?", "Invalid Username",
            JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }
      final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      try {
        controller.banUsername(new Node(name1, InetAddress.getByName("0.0.0.0"), 0), new Date(0));
      } catch (final UnknownHostException ex) {
        ClientLogger.logQuietly(ex);
      }
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private void addUnbanMacAddressMenu(final JMenu parentMenu) {
    final JMenuItem item = new JMenuItem("Unban Hashed Mac Address");
    item.addActionListener(e -> {
      final String mac = JOptionPane.showInputDialog(null,
          "Enter the hashed Mac Address that you want to unban from the lobby.\r\n\r\n"
              + "Hashed Mac Addresses should be entered in this format: $1$MH$345ntXD4G3AKpAeHZdaGe3",
          "");
      if (mac == null || mac.length() < 1) {
        return;
      }
      final String prefix = MD5Crypt.MAGIC + "MH$";
      final String error;
      if (mac.length() != 28) {
        error = "Must be 28 characters long";
      } else if (!mac.startsWith(prefix)) {
        error = "Must start with: " + prefix;
      } else if (!mac.matches("[0-9a-zA-Z$./]+")) {
        error = "Must use only these characters: 0-9a-zA-Z$./";
      } else {
        error = null;
      }
      if (error != null) {
        if (JOptionPane.showConfirmDialog(m_frame,
            "The hashed Mac Address you entered is invalid (" + error + "). Do you want to ban it anyhow?",
            "Invalid Hashed Mac", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }
      final IModeratorController controller = (IModeratorController) m_frame.getLobbyClient().getMessengers()
          .getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
      try {
        controller.banMac(new Node("None (Admin menu originated unban)", InetAddress.getByName("0.0.0.0"), 0), mac,
            new Date(0));
      } catch (final UnknownHostException ex) {
        ClientLogger.logQuietly(ex);
      }
    });
    item.setEnabled(true);
    parentMenu.add(item);
  }

  private long requestTimespanSupplication() {
    final List<String> timeUnits = new ArrayList<>();
    timeUnits.add("Minute");
    timeUnits.add("Hour");
    timeUnits.add("Day");
    timeUnits.add("Week");
    timeUnits.add("Month");
    timeUnits.add("Year");
    timeUnits.add("Forever");
    final int result = JOptionPane.showOptionDialog(m_frame, "Select the unit of measurement: ", "Select Timespan Unit",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, timeUnits.toArray(), timeUnits.toArray()[3]);
    if (result < 0) {
      return -1;
    }
    final String selectedTimeUnit = (String) timeUnits.toArray()[result];
    if (selectedTimeUnit.equals("Forever")) {
      return Long.MAX_VALUE;
    }
    final String stringr =
        JOptionPane.showInputDialog(m_frame, "Now please enter the length of time: (In " + selectedTimeUnit + "s) ", 1);
    if (stringr == null) {
      return -1;
    }
    final long result2 = Long.parseLong(stringr);
    if (result2 < 0) {
      return -1;
    }
    long ticks = 0;
    if (selectedTimeUnit.equals("Minute")) {
      ticks = result2 * 1000 * 60;
    } else if (selectedTimeUnit.equals("Hour")) {
      ticks = result2 * 1000 * 60 * 60;
    } else if (selectedTimeUnit.equals("Day")) {
      ticks = result2 * 1000 * 60 * 60 * 24;
    } else if (selectedTimeUnit.equals("Week")) {
      ticks = result2 * 1000 * 60 * 60 * 24 * 7;
    } else if (selectedTimeUnit.equals("Month")) {
      ticks = result2 * 1000 * 60 * 60 * 24 * 30;
    } else if (selectedTimeUnit.equals("Year")) {
      ticks = result2 * 1000 * 60 * 60 * 24 * 365;
    }
    return ticks;
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
    chatTimeBox.addActionListener(e -> m_frame.setShowChatTime(chatTimeBox.isSelected()));
    chatTimeBox.setSelected(true);
    parentMenu.add(chatTimeBox);
  }

  private void addUpdateAccountMenu(final JMenu account) {
    final JMenuItem update = new JMenuItem("Update Account...");
    // only if we are not anonymous login
    update.setEnabled(!m_frame.getLobbyClient().isAnonymousLogin());
    update.addActionListener(e -> updateAccountDetails());
    account.add(update);
  }

  private void updateAccountDetails() {
    final IUserManager manager =
        (IUserManager) m_frame.getLobbyClient().getRemoteMessenger().getRemote(IUserManager.USER_MANAGER);
    final DBUser user = manager.getUserInfo(m_frame.getLobbyClient().getMessenger().getLocalNode().getName());
    if (user == null) {
      JOptionPane.showMessageDialog(this, "No user info found", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final CreateUpdateAccountPanel panel = CreateUpdateAccountPanel.newUpdatePanel(user);
    final CreateUpdateAccountPanel.ReturnValue rVal = panel.show(m_frame);
    if (rVal == CreateUpdateAccountPanel.ReturnValue.CANCEL) {
      return;
    }
    final String error = manager.updateUser(panel.getUserName(), panel.getEmail(), MD5Crypt.crypt(panel.getPassword()));
    if (error != null) {
      JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
    }
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
      final JMenuItem menuFileExit = new JMenuItem(SwingAction.of("Exit", e -> m_frame.shutdown()));
      parentMenu.add(menuFileExit);
    }
  }
}
