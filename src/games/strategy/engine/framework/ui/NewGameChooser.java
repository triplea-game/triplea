package games.strategy.engine.framework.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.util.LocalizeHTML;

public class NewGameChooser extends JDialog {
  private static final long serialVersionUID = -3223711652118741132L;

  // Use synchronization when accessing s_cachedGameModel, it is accessed by both
  // the Swing AWT event thread and also background threads, which parses available
  // maps in the background when a game is not playing
  private static NewGameChooserModel cachedGameModel = null;
  private static ClearGameChooserCacheMessenger cacheClearedMessenger;

  private JButton okButton;
  private JButton cancelButton;
  private JList<NewGameChooserEntry> gameList;
  private JPanel infoPanel;
  private JEditorPane notesPanel;
  private NewGameChooserModel gameListModel;
  private NewGameChooserEntry choosen;
  private boolean finishedRefreshing = false;

  private NewGameChooser(final Frame owner) {
    super(owner, "Select a Game", true);
    createComponents();
    layoutCoponents();
    setupListeners();
    setWidgetActivation();
    updateInfoPanel();
    Timer timer = new Timer();
    timer.schedule(new TimerTask(){
      @Override
      public void run() {
        if(!finishedRefreshing){
          JOptionPane.showMessageDialog(owner, "Loading the Maps took more than 5 seconds... \n Thanks for your patience.");
        }
      }}, 5000);
    refreshGameList();
    finishedRefreshing = true;
  }

  private void createComponents() {
    okButton = new JButton("OK");
    cancelButton = new JButton("Cancel");
    gameListModel = getNewGameChooserModel();
    gameList = new JList<>(gameListModel);
    infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    notesPanel = new JEditorPane();
    notesPanel.setEditable(false);
    notesPanel.setContentType("text/html");
    notesPanel.setBackground(new JLabel().getBackground());
  }

  private void layoutCoponents() {
    setLayout(new BorderLayout());
    final JSplitPane mainSplit = new JSplitPane();
    add(mainSplit, BorderLayout.CENTER);
    final JScrollPane listScroll = new JScrollPane();
    listScroll.setBorder(null);
    listScroll.getViewport().setBorder(null);
    listScroll.setViewportView(gameList);
    final JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new GridBagLayout());
    final JLabel gamesLabel = new JLabel("Games");
    gamesLabel.setFont(gamesLabel.getFont().deriveFont(Font.BOLD, gamesLabel.getFont().getSize() + 2));
    leftPanel.add(gamesLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    leftPanel.add(listScroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.EAST,
        GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
    mainSplit.setLeftComponent(leftPanel);
    mainSplit.setRightComponent(infoPanel);
    mainSplit.setBorder(null);
    listScroll.setMinimumSize(new Dimension(200, 0));
    final JPanel buttonsPanel = new JPanel();
    add(buttonsPanel, BorderLayout.SOUTH);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    buttonsPanel.add(Box.createHorizontalStrut(30));
    buttonsPanel.add(Box.createGlue());
    buttonsPanel.add(okButton);
    buttonsPanel.add(cancelButton);
    buttonsPanel.add(Box.createGlue());
    final JScrollPane notesScroll = new JScrollPane();
    notesScroll.setViewportView(notesPanel);
    notesScroll.setBorder(null);
    notesScroll.getViewport().setBorder(null);
    infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    infoPanel.add(notesScroll, BorderLayout.CENTER);
  }

  public static NewGameChooserEntry chooseGame(final Frame parent, final String defaultGameName) {
    NewGameChooser chooser = new NewGameChooser(parent);
    chooser.setSize(800, 600);
    chooser.setLocationRelativeTo(parent);
    chooser.selectGame(defaultGameName);
    chooser.setVisible(true);
    // chooser is now visible and waits for user action
    final NewGameChooserEntry choosen = chooser.choosen;
    // remove all system resources (we have been having a problem with a memory leak related to this somehow)
    chooser.setVisible(false);
    chooser.removeAll();
    chooser.notesPanel.setText("");
    chooser.notesPanel.removeAll();
    chooser.notesPanel = null;
    chooser.dispose();
    chooser = null;
    return choosen;
  }

  private void selectGame(final String gameName) {
    if (gameName == null || gameName.equals("-")) {
      gameList.setSelectedIndex(0);
      return;
    }
    final NewGameChooserEntry entry = gameListModel.findByName(gameName);
    if (entry != null) {
      gameList.setSelectedValue(entry, true);
    }
  }

  private void updateInfoPanel() {
    if (getSelected() != null) {
      final GameData data = getSelected().getGameData();
      final StringBuilder notes = new StringBuilder();
      notes.append("<h1>").append(data.getGameName()).append("</h1>");
      final String mapNameDir = data.getProperties().get("mapName", "");
      appendListItem("Map Name", mapNameDir, notes);
      appendListItem("Number Of Players", data.getPlayerList().size() + "", notes);
      appendListItem("Location", getSelected().getLocation() + "", notes);
      appendListItem("Version", data.getGameVersion() + "", notes);
      notes.append("<p></p>");
      final String notesProperty = data.getProperties().get("notes", "");
      if (notesProperty != null && notesProperty.trim().length() != 0) {
        // UIContext resource loader should be null (or potentially is still the last game we played's loader),
        // so we send the map dir name so that our localizing of image links can get a new resource loader if needed
        notes.append(LocalizeHTML.localizeImgLinksInHTML(notesProperty.trim(), null, mapNameDir));
      }
      notesPanel.setText(notes.toString());
    } else {
      if (notesPanel != null) {
        notesPanel.setText("");
      }
    }
    // scroll to the top of the notes screen
    SwingUtilities.invokeLater(() ->{
      if (notesPanel != null) {
        notesPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
      }
    });
  }

  private static void appendListItem(final String title, final String value, final StringBuilder builder) {
    builder.append("<b>").append(title).append("</b>").append(": ").append(value).append("<br>");
  }

  private NewGameChooserEntry getSelected() {
    final int selected = gameList.getSelectedIndex();
    if (selected == -1) {
      return null;
    }
    return gameListModel.get(selected);
  }

  private void setWidgetActivation() {}

  private void setupListeners() {
    okButton.addActionListener(e -> selectAndReturn());
    cancelButton.addActionListener(e -> cancelAndReturn());
    gameList.addListSelectionListener(e -> updateInfoPanel());
    gameList.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent event) {
        if (event.getClickCount() == 2) {
          selectAndReturn();
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {}

      @Override
      public void mouseReleased(final MouseEvent e) {}

      @Override
      public void mouseEntered(final MouseEvent e) {}

      @Override
      public void mouseExited(final MouseEvent e) {}
    });
  }

  /** Populates the NewGameChooserModel cache if empty, then returns the cached instance */
  public synchronized static NewGameChooserModel getNewGameChooserModel() {
    if (cachedGameModel == null) {
      refreshNewGameChooserModel();
    }
    return cachedGameModel;
  }

  public synchronized static void refreshNewGameChooserModel() {
    clearNewGameChooserModel();
    cacheClearedMessenger = new ClearGameChooserCacheMessenger();
    cachedGameModel = new NewGameChooserModel(cacheClearedMessenger);
  }

  public static void clearNewGameChooserModel() {
    if (cacheClearedMessenger != null) {
      cacheClearedMessenger.sendCancel();
      cacheClearedMessenger = null;
    }
    synchronizedClear();
  }

  private synchronized static void synchronizedClear() {
    if (cachedGameModel != null) {
      cachedGameModel.clear();
      cachedGameModel = null;
    }
  }

  /**
   * Refreshes the game list (from disk) then caches the new list
   */
  private void refreshGameList() {
    gameList.setEnabled(false);
    final NewGameChooserEntry selected = getSelected();
        try {
          refreshNewGameChooserModel();
          gameListModel = getNewGameChooserModel();
          gameList.setModel(gameListModel);
          if(selected != null){
            selectGame(selected.getGameData().getGameName());
          }
        } finally {
          gameList.setEnabled(true);
        }
  }

  private void selectAndReturn() {
    choosen = getSelected();
    setVisible(false);
  }

  private void cancelAndReturn() {
    choosen = null;
    setVisible(false);
  }
}
