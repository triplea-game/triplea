package games.strategy.triplea.ui.menubar;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.metal.MetalLookAndFeel;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.triplea.ui.MacQuitMenuWrapper;
import games.strategy.triplea.ui.PlayersPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.networkMaintenance.BanPlayerAction;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.MutePlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
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
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.IntTextField;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.Triple;

public class TripleAMenuBar extends JMenuBar {
  private static final long serialVersionUID = -1447295944297939539L;
  protected final TripleAFrame frame;
  protected JEditorPane gameNotesPane;

  public TripleAMenuBar(final TripleAFrame frame) {
    this.frame = frame;
    add(createFileMenu());
    new ViewMenu(this, frame, getData());
    new GameMenu(this, frame, getData());
    new ExportMenu(this, frame, getData());

    final InGameLobbyWatcherWrapper watcher = createLobbyMenu(this);
    createNetworkMenu(this, watcher);
    createWebHelpMenu(this);
    new DebugMenu(this, frame);
    new HelpMenu(this, frame.getUIContext(), getData(), getBackground());
  }




  public JEditorPane getGameNotesJEditorPane() {
    return gameNotesPane;
  }

  protected InGameLobbyWatcherWrapper createLobbyMenu(final JMenuBar menuBar) {
    if (!(frame.getGame() instanceof ServerGame)) {
      return null;
    }
    final ServerGame serverGame = (ServerGame) frame.getGame();
    final InGameLobbyWatcherWrapper watcher = serverGame.getInGameLobbyWatcher();
    if (watcher == null || !watcher.isActive()) {
      return watcher;
    }
    final JMenu lobby = new JMenu("Lobby");
    lobby.setMnemonic(KeyEvent.VK_L);
    menuBar.add(lobby);
    lobby.add(new EditGameCommentAction(watcher, frame));
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
  }

  protected void addBanPlayer(final JMenu parentMenu) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    final Action ban = new BanPlayerAction(this, messenger);
    parentMenu.add(ban);
  }

  protected void addMutePlayer(final JMenu parentMenu) {
    if (!getGame().getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
    final Action mute = new MutePlayerAction(this, messenger);
    parentMenu.add(mute);
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
          SwingAction.of("Show Who is Who...", e -> PlayersPanel.showPlayers(getGame(), frame));
      menuGame.add(optionsAction);
    }
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
    final JMenuItem helpLink = new JMenuItem("Help...");
    helpLink.setMnemonic(KeyEvent.VK_G);
    final JMenuItem ruleBookLink = new JMenuItem("Rule Book...");
    ruleBookLink.setMnemonic(KeyEvent.VK_K);

    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_HOSTING_MAPS));
    mapLink.addActionListener(e  -> SwingComponents.newOpenUrlConfirmationDialog( UrlConstants.SF_HOSTING_MAPS));
    bugReport.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_TICKET_LIST));
    lobbyRules.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB_LOBBY_RULES ));
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB));
    devForum.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_FORUM));
    donateLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    helpLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.WEBSITE_HELP));
    ruleBookLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK));

    parentMenu.add(hostingLink);
    parentMenu.add(mapLink);
    parentMenu.add(bugReport);
    parentMenu.add(lobbyRules);
    parentMenu.add(warClub);
    parentMenu.add(devForum);
    parentMenu.add(donateLink);
    parentMenu.add(helpLink);
    parentMenu.add(ruleBookLink);
  }



  protected JMenu createFileMenu() {
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    fileMenu.add(createSaveMenu());

    if (PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(getGame().getData())) {
      fileMenu.add(addPostPBEM());
    }

    fileMenu.addSeparator();
    addExitMenu(fileMenu);
    return fileMenu;
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

  private JMenuItem createSaveMenu() {
    final JMenuItem menuFileSave = new JMenuItem(SwingAction.of("Save...", e -> {
      final File f = getSaveGameLocationDialog(frame);
      if (f != null) {
        getGame().saveGame(f);
        JOptionPane.showMessageDialog(frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
      }
    }));
    menuFileSave.setMnemonic(KeyEvent.VK_S);
    menuFileSave.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    return menuFileSave;
  }

  protected JMenuItem addPostPBEM() {
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
        PBEMMessagePoster.postTurn(title, historyLog, true, poster, null, frame, null);
      } finally {
        data.releaseReadLock();
      }
    }));
    menuPBEM.setMnemonic(KeyEvent.VK_P);
    menuPBEM.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_P, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    return menuPBEM;
  }

  protected void addExitMenu(final JMenu parentMenu) {
    final boolean isMac = GameRunner.isMac();
    final JMenuItem leaveGameMenuExit = new JMenuItem(SwingAction.of("Leave Game", e -> frame.leaveGame()));
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
      MacQuitMenuWrapper.registerMacShutdownHandler(frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      final JMenuItem menuFileExit = new JMenuItem(SwingAction.of("Exit", e -> frame.shutdown()));
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
    final List<String> substanceLooks = new ArrayList<>();
    for (final LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
      substanceLooks.add(look.getClassName());
    }
    if (!isJavaGreatThan6()) {
      substanceLooks.remove("javax.swing.plaf.nimbus.NimbusLookAndFeel");
    }
    if (isJavaGreatThan5()) {
      substanceLooks.addAll(new ArrayList<>(
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
    final Map<String, String> lookAndFeels = new LinkedHashMap<>();
    try {
      final List<String> substanceLooks = getLookAndFeelAvailableList();
      for (final String s : substanceLooks) {
        final Class<?> c = Class.forName(s);
        final LookAndFeel lf = (LookAndFeel) c.newInstance();
        lookAndFeels.put(lf.getName(), s);
      }
    } catch (final Exception e) {
      ClientLogger.logError("An Error occured while getting LookAndFeels", e);
      // we know all machines have these 3, so use them
      lookAndFeels.clear();
      lookAndFeels.put("Original", UIManager.getSystemLookAndFeelClassName());
      lookAndFeels.put("Metal", MetalLookAndFeel.class.getName());
      lookAndFeels.put("Platform Independent", UIManager.getCrossPlatformLookAndFeelClassName());
    }
    final JList<String> list = new JList<>(new Vector<>(lookAndFeels.keySet()));
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





  public IGame getGame() {
    return frame.getGame();
  }

  public GameData getData() {
    return frame.getGame().getData();
  }
}
