package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.util.LocalizeHtml;

/**
 * A modal dialog that prompts the user to select a game (map) from the list of installed games
 * (maps).
 */
public class GameChooser extends JDialog {
  private static final long serialVersionUID = -3223711652118741132L;

  private GameChooserEntry chosen;

  private GameChooser(
      final Frame owner, final GameChooserModel gameChooserModel, final String gameName) {
    super(owner, "Select a Game", true);
    final JList<GameChooserEntry> gameList = new JList<>(gameChooserModel);
    if (gameName == null || gameName.equals("-")) {
      gameList.setSelectedIndex(0);
      return;
    }
    gameChooserModel
        .findByName(gameName)
        .ifPresent(entry -> gameList.setSelectedValue(entry, true));
    setLayout(new BorderLayout());

    final JSplitPane mainSplit = new JSplitPane();
    add(mainSplit, BorderLayout.CENTER);
    final JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new GridBagLayout());
    leftPanel.add(
        new JLabelBuilder("Games").adjustFontSize(2).bold().build(),
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

    final JScrollPane listScroll = SwingComponents.newJScrollPane(gameList);
    listScroll.setMinimumSize(new Dimension(200, 0));
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

    final JEditorPane notesPanel = new JEditorPane();
    notesPanel.setEditable(false);
    notesPanel.setContentType("text/html");
    notesPanel.setForeground(Color.BLACK);
    notesPanel.setText(buildGameNotesText(gameList.getSelectedValue()));

    final JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    infoPanel.add(SwingComponents.newJScrollPane(notesPanel), BorderLayout.CENTER);

    mainSplit.setRightComponent(infoPanel);
    mainSplit.setBorder(null);

    final Runnable selectAndReturn =
        () -> {
          chosen = gameList.getSelectedValue();
          setVisible(false);
        };

    final JPanel buttonsPanel =
        new JPanelBuilder()
            .boxLayoutHorizontal()
            .addHorizontalStrut(30)
            .add(Box.createGlue())
            .add(new JButtonBuilder("OK").actionListener(selectAndReturn).build())
            .add(new JButtonBuilder("Cancel").actionListener(this::dispose).build())
            .add(Box.createGlue())
            .build();
    add(buttonsPanel, BorderLayout.SOUTH);

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
              selectAndReturn.run();
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
  public static GameChooserEntry chooseGame(
      final Frame parent, final GameChooserModel gameChooserModel, final String defaultGameName) {
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
