package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.triplea.util.LocalizeHtml;

/**
 * A modal dialog that prompts the user to select a game (map) from the list of installed games
 * (maps).
 */
public class GameChooser extends JDialog {
  private static final long serialVersionUID = -3223711652118741132L;

  private final JList<GameChooserEntry> gameList;
  private final GameChooserModel gameListModel;
  private GameChooserEntry chosen;

  private GameChooser(
      final Frame owner, final GameChooserModel gameChooserModel, final String gameName) {
    super(owner, "Select a Game", true);
    gameListModel = gameChooserModel;
    gameList = new JList<>(gameListModel);
    if (gameName == null || gameName.equals("-")) {
      gameList.setSelectedIndex(0);
      return;
    }
    gameListModel.findByName(gameName).ifPresent(entry -> gameList.setSelectedValue(entry, true));
    final JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    final JEditorPane notesPanel = new JEditorPane();
    notesPanel.setEditable(false);
    notesPanel.setContentType("text/html");
    notesPanel.setForeground(Color.BLACK);
    notesPanel.setText(buildGameNotesText(gameList.getSelectedValue()));
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
    gamesLabel.setFont(
        gamesLabel.getFont().deriveFont(Font.BOLD, gamesLabel.getFont().getSize() + 2.0F));
    leftPanel.add(
        gamesLabel,
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 10, 10),
            0,
            0));
    leftPanel.add(
        listScroll,
        new GridBagConstraints(
            0,
            1,
            1,
            1,
            1.0,
            1.0,
            GridBagConstraints.EAST,
            GridBagConstraints.BOTH,
            new Insets(0, 10, 0, 0),
            0,
            0));
    mainSplit.setLeftComponent(leftPanel);
    mainSplit.setRightComponent(infoPanel);
    mainSplit.setBorder(null);
    listScroll.setMinimumSize(new Dimension(200, 0));
    final JPanel buttonsPanel = new JPanel();
    add(buttonsPanel, BorderLayout.SOUTH);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    buttonsPanel.add(Box.createHorizontalStrut(30));
    buttonsPanel.add(Box.createGlue());
    final JButton okButton = new JButton("OK");
    buttonsPanel.add(okButton);
    final JButton cancelButton = new JButton("Cancel");
    buttonsPanel.add(cancelButton);
    buttonsPanel.add(Box.createGlue());
    final JScrollPane notesScroll = new JScrollPane();
    notesScroll.setViewportView(notesPanel);
    notesScroll.setBorder(null);
    notesScroll.getViewport().setBorder(null);
    infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    infoPanel.add(notesScroll, BorderLayout.CENTER);

    final ActionListener selectAndReturn =
        e -> {
          chosen = gameList.getSelectedValue();
          setVisible(false);
        };
    okButton.addActionListener(selectAndReturn);
    cancelButton.addActionListener(e -> dispose());
    gameList.addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            notesPanel.setText(buildGameNotesText(gameList.getSelectedValue()));
            // scroll to the top of the notes screen
            SwingUtilities.invokeLater(
                () -> notesPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0)));
          }
        });
    gameList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent event) {
            if (event.getClickCount() == 2) {
              selectAndReturn.actionPerformed(null);
            }
          }
        });
    // scroll to the top of the notes screen
    SwingUtilities.invokeLater(() -> notesPanel.scrollRectToVisible(new Rectangle(0, 0, 0, 0)));
  }

  /**
   * Displays the Game Chooser dialog and returns the game selected by the user or {@code null} if
   * no game was selected.
   */
  public static GameChooserEntry chooseGame(final Frame parent, final String defaultGameName)
      throws InterruptedException {
    final GameChooserModel gameChooserModel =
        new GameChooserModel(
            BackgroundTaskRunner.runInBackgroundAndReturn(
                "Loading all available games...", GameChooserModel::parseMapFiles));
    final GameChooser chooser = new GameChooser(parent, gameChooserModel, defaultGameName);
    chooser.setSize(800, 600);
    chooser.setLocationRelativeTo(parent);
    chooser.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    chooser.setVisible(true); // Blocking and waits for user action
    return chooser.chosen;
  }

  private static String buildGameNotesText(final GameChooserEntry gameChooserEntry) {
    if (gameChooserEntry == null) {
      return "";
    }
    final GameData data = gameChooserEntry.getGameData();
    final StringBuilder notes = new StringBuilder();
    notes.append("<h1>").append(data.getGameName()).append("</h1>");
    final String mapNameDir = data.getProperties().get("mapName", "");
    appendListItem("Map Name", mapNameDir, notes);
    appendListItem("Number Of Players", data.getPlayerList().size() + "", notes);
    appendListItem("Location", gameChooserEntry.getLocation() + "", notes);
    appendListItem("Version", data.getGameVersion() + "", notes);
    notes.append("<p></p>");
    final String trimmedNotes = data.getProperties().get("notes", "").trim();
    if (!trimmedNotes.isEmpty()) {
      // AbstractUiContext resource loader should be null (or potentially is still the last game
      // we played's loader),
      // so we send the map dir name so that our localizing of image links can get a new resource
      // loader if needed
      notes.append(LocalizeHtml.localizeImgLinksInHtml(trimmedNotes, mapNameDir));
    }
    return notes.toString();
  }

  private static void appendListItem(
      final String title, final String value, final StringBuilder builder) {
    builder.append("<b>").append(title).append("</b>").append(": ").append(value).append("<br>");
  }
}
