package games.strategy.common.ui;


import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import games.strategy.common.swing.SwingAction;
import games.strategy.common.swing.SwingComponents;
import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.networkMaintenance.BanPlayerAction;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.MutePlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.net.IServerMessenger;
import games.strategy.performance.EnablePerformanceLoggingCheckBox;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.IntTextField;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.LocalizeHTML;
import games.strategy.util.SoftJEditorPane;
import games.strategy.util.Triple;

public class BasicGameMenuBar<CustomGameFrame extends MainGameFrame> extends JMenuBar {
  private static final long serialVersionUID = -1447295944297939539L;
  protected final CustomGameFrame m_frame;
  protected SoftJEditorPane m_gameNotesPane;

  public BasicGameMenuBar(final CustomGameFrame frame) {
    m_frame = frame;
    createFileMenu(this);
    createGameSpecificMenus(this);
    final InGameLobbyWatcherWrapper watcher = createLobbyMenu(this);
    createNetworkMenu(this, watcher);
    createWebHelpMenu(this);
    createDebugMenu(this);
    createHelpMenu(this);
  }

  private void createDebugMenu(final JMenuBar menuBar) {
    final JMenu debugMenu = new JMenu("Debug");
    menuBar.add(debugMenu);
    addChangeProAISettings(debugMenu);
    debugMenu.add(new EnablePerformanceLoggingCheckBox());
    debugMenu.setMnemonic(KeyEvent.VK_D);
    addConsoleMenu(debugMenu);
  }

  protected void addReportBugsMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of( "Send Bug Report", e -> {
      SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES);
    } )).setMnemonic(KeyEvent.VK_B);
  }

  protected void addConsoleMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Show Console...", e ->  {
        ErrorConsole.getConsole().setVisible(true);
        ErrorConsole.getConsole().append(DebugUtils.getMemory());
    })).setMnemonic(KeyEvent.VK_C);
  }

  private void addChangeProAISettings(final JMenu parentMenu) {
    boolean areThereProAIs = false;
    final Set<IGamePlayer> players = (m_frame).getLocalPlayers().getLocalPlayers();
    for (final IGamePlayer player : players) {
      if (player instanceof ProAI) {
        areThereProAIs = true;
      }
    }
    if (areThereProAIs) {
      ProAI.initialize((TripleAFrame) m_frame);
      parentMenu.addSeparator();
      parentMenu.add(SwingAction.of("Show Hard AI Logs", e -> ProAI.showSettingsWindow())).setMnemonic(KeyEvent.VK_X);
      parentMenu.addSeparator();
    }
  }

  protected void createGameSpecificMenus(final JMenuBar menuBar) {}

  public void dispose() {
    if (m_gameNotesPane != null) {
      m_gameNotesPane.dispose();
    }
  }

  public SoftJEditorPane getGameNotesJEditorPane() {
    return m_gameNotesPane;
  }

  protected void addGameNotesMenu(final JMenu parentMenu) {
    // allow the game developer to write notes that appear in the game
    // displays whatever is in the notes field in html
    final String notesProperty = getData().getProperties().get("notes", "");
    if (notesProperty != null && notesProperty.trim().length() != 0) {
      final String notes = LocalizeHTML.localizeImgLinksInHTML(notesProperty.trim());
      m_gameNotesPane = new SoftJEditorPane(notes);
      parentMenu.add(SwingAction.of("Game Notes...", e -> {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            final JEditorPane pane = m_gameNotesPane.getComponent();
            final JScrollPane scroll = new JScrollPane(pane);
            scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
            final JDialog dialog = new JDialog(m_frame);
            dialog.setModal(false);
            // needs java 1.6 at least...
            // dialog.setModalityType(ModalityType.MODELESS);
            dialog.setAlwaysOnTop(true);
            dialog.add(scroll, BorderLayout.CENTER);
            final JPanel buttons = new JPanel();
            final JButton button = new JButton(SwingAction.of("OK", event -> {
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();

            }));
            buttons.add(button);
            dialog.getRootPane().setDefaultButton(button);
            dialog.add(buttons, BorderLayout.SOUTH);
            dialog.pack();
            if (dialog.getWidth() < 400) {
              dialog.setSize(400, dialog.getHeight());
            }
            if (dialog.getHeight() < 300) {
              dialog.setSize(dialog.getWidth(), 300);
            }
            if (dialog.getWidth() > 800) {
              dialog.setSize(800, dialog.getHeight());
            }
            if (dialog.getHeight() > 600) {
              dialog.setSize(dialog.getWidth(), 600);
            }
            dialog.setLocationRelativeTo(m_frame);
            dialog.addWindowListener(new WindowAdapter() {
              @Override
              public void windowOpened(final WindowEvent e) {
                scroll.getVerticalScrollBar().getModel().setValue(0);
                scroll.getHorizontalScrollBar().getModel().setValue(0);
                button.requestFocus();
              }
            });
            dialog.setVisible(true);
          }
        });
      })).setMnemonic(KeyEvent.VK_N);
    }
  }

  protected InGameLobbyWatcherWrapper createLobbyMenu(final JMenuBar menuBar) {
    if (!(m_frame.getGame() instanceof ServerGame)) {
      return null;
    }
    final ServerGame serverGame = (ServerGame) m_frame.getGame();
    final InGameLobbyWatcherWrapper watcher = serverGame.getInGameLobbyWatcher();
    if (watcher == null || !watcher.isActive()) {
      return watcher;
    }
    final JMenu lobby = new JMenu("Lobby");
    lobby.setMnemonic(KeyEvent.VK_L);
    menuBar.add(lobby);
    lobby.add(new EditGameCommentAction(watcher, m_frame));
    lobby.add(new RemoveGameFromLobbyAction(watcher));
    return watcher;
  }

  protected void createNetworkMenu(final JMenuBar menuBar, final InGameLobbyWatcherWrapper watcher) {
    // revisit
    // if we are not a client or server game
    // then this will not create the network menu
    if (getGame().getMessenger() instanceof DummyMessenger) {
      return;
    }
    final JMenu menuNetwork = new JMenu("Network");
    menuNetwork.setMnemonic(KeyEvent.VK_N);
    addBootPlayer(menuNetwork);
    addBanPlayer(menuNetwork);
    addMutePlayer(menuNetwork);
    addSetGamePassword(menuNetwork, watcher);
    addShowPlayers(menuNetwork);
    menuBar.add(menuNetwork);
  }

  protected void addBootPlayer(final JMenu parentMenu) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    final Action boot = new BootPlayerAction(this, messenger);
    parentMenu.add(boot);
    return;
  }

  protected void addBanPlayer(final JMenu parentMenu) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    final Action ban = new BanPlayerAction(this, messenger);
    parentMenu.add(ban);
    return;
  }

  protected void addMutePlayer(final JMenu parentMenu) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    final Action mute = new MutePlayerAction(this, messenger);
    parentMenu.add(mute);
    return;
  }

  protected void addSetGamePassword(final JMenu parentMenu, final InGameLobbyWatcherWrapper watcher) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    parentMenu.add(new SetPasswordAction(this, watcher, (ClientLoginValidator) messenger.getLoginValidator()));
  }

  protected void addShowPlayers(final JMenu menuGame) {
    if (!getGame().getData().getProperties().getEditableProperties().isEmpty()) {
      final AbstractAction optionsAction =
          SwingAction.of("Show Who is Who...", e -> PlayersPanel.showPlayers(getGame(), m_frame));
      menuGame.add(optionsAction);
    }
  }

  protected void createHelpMenu(final JMenuBar menuBar) {
    final JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    menuBar.add(helpMenu);
    addGameSpecificHelpMenus(helpMenu);
    addGameNotesMenu(helpMenu);
    addAboutMenu(helpMenu);
    addReportBugsMenu(helpMenu);
  }

  private static void createWebHelpMenu(final JMenuBar menuBar) {
    final JMenu web = new JMenu("Web");
    web.setMnemonic(KeyEvent.VK_W);
    menuBar.add(web);
    addWebMenu(web);
  }

  private static void addWebMenu(final JMenu parentMenu) {
    final JMenuItem hostingLink = new JMenuItem("How to Host...");
    hostingLink.setMnemonic(KeyEvent.VK_H);
    final JMenuItem mapLink = new JMenuItem("Install Maps...");
    mapLink.setMnemonic(KeyEvent.VK_I);
    final JMenuItem bugReport = new JMenuItem("Bug Report...");
    bugReport.setMnemonic(KeyEvent.VK_B);
    final JMenuItem lobbyRules = new JMenuItem("Lobby Rules...");
    lobbyRules.setMnemonic(KeyEvent.VK_L);
    final JMenuItem warClub = new JMenuItem("War Club & Ladder...");
    warClub.setMnemonic(KeyEvent.VK_W);
    final JMenuItem devForum = new JMenuItem("Developer Forum...");
    devForum.setMnemonic(KeyEvent.VK_E);
    final JMenuItem donateLink = new JMenuItem("Donate...");
    donateLink.setMnemonic(KeyEvent.VK_O);
    final JMenuItem guidesLink = new JMenuItem("Guides...");
    guidesLink.setMnemonic(KeyEvent.VK_G);

    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_HOSTING_MAPS));
    mapLink.addActionListener(e  -> SwingComponents.newOpenUrlConfirmationDialog( UrlConstants.SF_HOSTING_MAPS));
    bugReport.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_TICKET_LIST));
    lobbyRules.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB_LOBBY_RULES ));
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB));
    devForum.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_FORUM));
    donateLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    guidesLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_WIKI_GUIDES));

    parentMenu.add(hostingLink);
    parentMenu.add(mapLink);
    parentMenu.add(bugReport);
    parentMenu.add(lobbyRules);
    parentMenu.add(warClub);
    parentMenu.add(devForum);
    parentMenu.add(donateLink);
    parentMenu.add(guidesLink);
  }

  protected void addGameSpecificHelpMenus(final JMenu helpMenu) {}



  protected void addAboutMenu(final JMenu parentMenu) {
    final String text = "<h2>" + getData().getGameName() + "</h2>" + "<p><b>Engine Version:</b> "
        + ClientContext.engineVersion() + "<br><b>Game:</b> " + getData().getGameName()
        + "<br><b>Game Version:</b>" + getData().getGameVersion() + "</p>"
        + "<p>For more information please visit,<br><br>"
        + "<b><a hlink='" + UrlConstants.SOURCE_FORGE + "'>" + UrlConstants.SOURCE_FORGE + "</a></b><br><br>";
    final JEditorPane editorPane = new JEditorPane();
    editorPane.setBorder(null);
    editorPane.setBackground(getBackground());
    editorPane.setEditable(false);
    editorPane.setContentType("text/html");
    editorPane.setText(text);
    final JScrollPane scroll = new JScrollPane(editorPane);
    scroll.setBorder(null);
    if (System.getProperty("mrj.version") == null) {
      parentMenu.addSeparator();
      parentMenu.add(SwingAction.of("About...", e -> {
        JOptionPane.showMessageDialog(m_frame, editorPane, "About " + m_frame.getGame().getData().getGameName(),
            JOptionPane.PLAIN_MESSAGE);
      })).setMnemonic(KeyEvent.VK_A);
    } else
    // On Mac OS X, put the About menu where Mac users expect it to be
    {
      Application.getApplication().addApplicationListener(new ApplicationAdapter() {
        @Override
        public void handleAbout(final ApplicationEvent event) {
          // otherwise the default About menu will still show appear
          event.setHandled(true);
          JOptionPane.showMessageDialog(m_frame, editorPane, "About " + m_frame.getGame().getData().getGameName(),
              JOptionPane.PLAIN_MESSAGE);
        }
      });
    }
  }


  protected void createFileMenu(final JMenuBar menuBar) {
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    menuBar.add(fileMenu);
    addSaveMenu(fileMenu);
    addPostPBEM(fileMenu);
    fileMenu.addSeparator();
    addExitMenu(fileMenu);
  }

  public static File getSaveGameLocationDialog(final Frame frame) {
    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (GameRunner.isMac()) {
      final FileDialog fileDialog = new FileDialog(frame);
      fileDialog.setMode(FileDialog.SAVE);
      SaveGameFileChooser.ensureDefaultDirExists();
      fileDialog.setDirectory(SaveGameFileChooser.DEFAULT_DIRECTORY.getPath());
      fileDialog.setFilenameFilter(new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) { // the extension should be .tsvg, but find svg
                                                                   // extensions as well
          return name.endsWith(".tsvg") || name.endsWith(".svg");
        }
      });
      fileDialog.setVisible(true);
      String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      if (fileName == null) {
        return null;
      } else {
        if (!fileName.endsWith(".tsvg")) {
          fileName += ".tsvg";
        }
        // If the user selects a filename that already exists,
        // the AWT Dialog on Mac OS X will ask the user for confirmation
        final File f = new File(dirName, fileName);
        return f;
      }
    }
    // Non-Mac platforms should use the normal Swing JFileChooser
    else {
      final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
      final int rVal = fileChooser.showSaveDialog(frame);
      if (rVal != JFileChooser.APPROVE_OPTION) {
        return null;
      }
      File f = fileChooser.getSelectedFile();
      // disallow sub directories to be entered (in the form directory/name) for Windows boxes
      if (GameRunner.isWindows()) {
        final int slashIndex = Math.min(f.getPath().lastIndexOf("\\"), f.getPath().length());
        final String filePath = f.getPath().substring(0, slashIndex);
        if (!fileChooser.getCurrentDirectory().toString().equals(filePath)) {
          JOptionPane.showConfirmDialog(frame, "Sub directories are not allowed in the file name.  Please rename it.",
              "Cancel?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
          return null;
        }
      }
      if (!f.getName().toLowerCase().endsWith(".tsvg")) {
        f = new File(f.getParent(), f.getName() + ".tsvg");
      }
      // A small warning so users will not over-write a file
      if (f.exists()) {
        final int choice =
            JOptionPane.showConfirmDialog(frame, "A file by that name already exists. Do you wish to over write it?",
                "Over-write?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
          return null;
        }
      }
      return f;
    }
  }

  protected void addSaveMenu(final JMenu parent) {
    final JMenuItem menuFileSave = new JMenuItem(SwingAction.of("Save...", e -> {
      final File f = getSaveGameLocationDialog(m_frame);
      if (f != null) {
        getGame().saveGame(f);
        JOptionPane.showMessageDialog(m_frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
      }
    }));
    menuFileSave.setMnemonic(KeyEvent.VK_S);
    menuFileSave.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    parent.add(menuFileSave);
  }

  protected void addPostPBEM(final JMenu parent) {
    if (!PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(getGame().getData())) {
      return;
    }
    final JMenuItem menuPBEM = new JMenuItem(SwingAction.of("Post PBEM/PBF Gamesave...", e -> {
      final GameData data = getGame().getData();
      if (data == null || !PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(data)) {
        return;
      }
      final String title = "Manual Gamesave Post";
      try {
        data.acquireReadLock();
        final GameStep step = data.getSequence().getStep();
        final PlayerID currentPlayer = (step == null ? PlayerID.NULL_PLAYERID
            : (step.getPlayerID() == null ? PlayerID.NULL_PLAYERID : step.getPlayerID()));
        final int round = data.getSequence().getRound();
        final HistoryLog historyLog = new HistoryLog();
        historyLog.printFullTurn(data, false, GameStepPropertiesHelper.getTurnSummaryPlayers(data));
        final PBEMMessagePoster poster = new PBEMMessagePoster(getData(), currentPlayer, round, title);
        PBEMMessagePoster.postTurn(title, historyLog, true, poster, null, m_frame, null);
      } finally {
        data.releaseReadLock();
      }
    }));
    menuPBEM.setMnemonic(KeyEvent.VK_P);
    menuPBEM.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_P, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    parent.add(menuPBEM);
  }

  protected void addExitMenu(final JMenu parentMenu) {
    final boolean isMac = GameRunner.isMac();
    final JMenuItem leaveGameMenuExit = new JMenuItem(SwingAction.of("Leave Game", e -> m_frame.leaveGame()));
    leaveGameMenuExit.setMnemonic(KeyEvent.VK_L);
    if (isMac) { // On Mac OS X, the command-Q is reserved for the Quit action,
                 // so set the command-L key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    } else { // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    parentMenu.add(leaveGameMenuExit);
    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacWrapper.registerMacShutdownHandler(m_frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      final JMenuItem menuFileExit = new JMenuItem(SwingAction.of("Exit", e -> m_frame.shutdown()));
      menuFileExit.setMnemonic(KeyEvent.VK_E);
      parentMenu.add(menuFileExit);
    }
  }

  protected static boolean isJavaGreatThan5() {
    final String version = System.getProperties().getProperty("java.version");
    return version.indexOf("1.5") == -1;
  }

  protected static boolean isJavaGreatThan6() {
    final String version = System.getProperties().getProperty("java.version");
    return version.indexOf("1.5") == -1 && version.indexOf("1.6") == -1;
  }

  public static List<String> getLookAndFeelAvailableList() {
    final List<String> substanceLooks = new ArrayList<String>();
    for (final LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
      substanceLooks.add(look.getClassName());
    }
    if (!isJavaGreatThan6()) {
      substanceLooks.remove("javax.swing.plaf.nimbus.NimbusLookAndFeel");
    }
    if (isJavaGreatThan5()) {
      substanceLooks.addAll(new ArrayList<String>(
          Arrays.asList(new String[] {"org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceCeruleanLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel",
              "org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel"})));
    }
    return substanceLooks;
  }

  /**
   * First is our JList, second is our LookAndFeels string -> class map, third is our 'current' look and feel.
   */
  public static Triple<JList<String>, Map<String, String>, String> getLookAndFeelList() {
    final Map<String, String> lookAndFeels = new LinkedHashMap<String, String>();
    try {
      final List<String> substanceLooks = getLookAndFeelAvailableList();
      for (final String s : substanceLooks) {
        @SuppressWarnings("rawtypes")
        final Class c = Class.forName(s);
        final LookAndFeel lf = (LookAndFeel) c.newInstance();
        lookAndFeels.put(lf.getName(), s);
      }
    } catch (final Exception t) {
      t.printStackTrace();
      // we know all machines have these 3, so use them
      lookAndFeels.clear();
      lookAndFeels.put("Original", UIManager.getSystemLookAndFeelClassName());
      lookAndFeels.put("Metal", MetalLookAndFeel.class.getName());
      lookAndFeels.put("Platform Independent", UIManager.getCrossPlatformLookAndFeelClassName());
    }
    final JList<String> list = new JList<>(new Vector<String>(lookAndFeels.keySet()));
    String currentKey = null;
    for (final String s : lookAndFeels.keySet()) {
      final String currentName = UIManager.getLookAndFeel().getClass().getName();
      if (lookAndFeels.get(s).equals(currentName)) {
        currentKey = s;
        break;
      }
    }
    list.setSelectedValue(currentKey, false);
    return Triple.of(list, lookAndFeels, currentKey);
  }

  protected void addSetLookAndFeel(final JMenu menuView) {
    menuView.add(SwingAction.of("Set Look and Feel...", e -> {
      final Triple<JList<String>, Map<String, String>, String> lookAndFeel = getLookAndFeelList();
      final JList<String> list = lookAndFeel.getFirst();
      final String currentKey = lookAndFeel.getThird();
      final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
      if (JOptionPane.showConfirmDialog(m_frame, list) == JOptionPane.OK_OPTION) {
        final String selectedValue = (String) list.getSelectedValue();
        if (selectedValue == null) {
          return;
        }
        if (selectedValue.equals(currentKey)) {
          return;
        }
        GameRunner2.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
        EventThreadJOptionPane.showMessageDialog(m_frame, "The look and feel will update when you restart TripleA",
            new CountDownLatchHandler(true));
      }
    })).setMnemonic(KeyEvent.VK_F);
  }

  protected void addShowGameUuid(final JMenu menuView) {
    menuView.add(SwingAction.of("Game UUID...", e -> {
      final String id = (String) getData().getProperties().get(GameData.GAME_UUID);
      final JTextField text = new JTextField();
      text.setText(id);
      final JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.add(new JLabel("Game UUID:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0));
      JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(BasicGameMenuBar.this), panel, "Game UUID",
          JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"}, "OK");
    })).setMnemonic(KeyEvent.VK_U);
  }

  protected void addChatTimeMenu(final JMenu parentMenu) {
    final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
    chatTimeBox.setMnemonic(KeyEvent.VK_T);
    chatTimeBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_frame.setShowChatTime(chatTimeBox.isSelected());
      }
    });
    chatTimeBox.setSelected(false);
    parentMenu.add(chatTimeBox);
    chatTimeBox.setEnabled(MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null);
  }

  protected void addAISleepDuration(final JMenu parentMenu) {
    final JMenuItem AISleepDurationBox = new JMenuItem("AI Pause Duration...");
    AISleepDurationBox.setMnemonic(KeyEvent.VK_A);
    AISleepDurationBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final IntTextField text = new IntTextField(50, 10000);
        text.setText(String.valueOf(AbstractUIContext.getAIPauseDuration()));
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.add(new JLabel("AI Pause Duration (ms):"), new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(BasicGameMenuBar.this), panel,
            "Set AI Pause Duration", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"},
            "OK");
        try {
          AbstractUIContext.setAIPauseDuration(Integer.parseInt(text.getText()));
        } catch (final Exception ex) {
        }
      }
    });
    parentMenu.add(AISleepDurationBox);
  }

  protected void addGameOptionsMenu(final JMenu menuGame) {
    if (!getGame().getData().getProperties().getEditableProperties().isEmpty()) {
      final AbstractAction optionsAction = SwingAction.of("View Game Options...", e -> {
        final PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties().getEditableProperties(), false);
        JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
      });
      menuGame.add(optionsAction).setMnemonic(KeyEvent.VK_O);
    }
  }

  // TODO: create a second menu option for parsing current attachments
  protected void addExportXML(final JMenu parentMenu) {
    final Action exportXML = SwingAction.of("Export game.xml file (Beta)...", e -> exportXMLFile());
    parentMenu.add(exportXML).setMnemonic(KeyEvent.VK_X);
  }

  private void exportXMLFile() {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(System.getProperties().getProperty("user.dir"));
    final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
    int round = 0;
    try {
      getData().acquireReadLock();
      round = getData().getSequence().getRound();
    } finally {
      getData().releaseReadLock();
    }
    String defaultFileName =
        "xml_" + formatDate.format(new Date()) + "_" + getData().getGameName() + "_round_" + round;
    defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
    defaultFileName = defaultFileName + ".xml";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final GameData data = getData();
    final String xmlFile;
    try {
      data.acquireReadLock();
      final GameDataExporter exporter = new games.strategy.engine.data.export.GameDataExporter(data, false);
      xmlFile = exporter.getXML();
    } finally {
      data.releaseReadLock();
    }
    try {
      try (final FileWriter writer = new FileWriter(chooser.getSelectedFile());) {
        writer.write(xmlFile);
      }
    } catch (final IOException e1) {
      ClientLogger.logQuietly(e1);
    }
  }

  public IGame getGame() {
    return m_frame.getGame();
  }

  public GameData getData() {
    return m_frame.getGame().getData();
  }
}
