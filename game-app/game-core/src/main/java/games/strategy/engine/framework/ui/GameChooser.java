package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.DownloadedMapsListing;
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
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * Use to display a modal dialog that prompts the user to select a game (map) from the list of
 * installed games (maps).
 */
@UtilityClass
public class GameChooser {

  /**
   * Displays the Game Chooser dialog and invokes the gameChosenCallback if a game is selected.
   * Blocking, blocks until the dialog is closed.
   *
   * @param gameChosenCallback The callback invoked when a game is chosen.
   */
  public static void chooseGame(
      final Frame owner,
      final DownloadedMapsListing downloadedMapsListing,
      final String gameName,
      final Consumer<Path> gameChosenCallback) {

    final JDialog dialog = new JDialog(owner, "Select a Game", true);
    dialog.setLayout(new BorderLayout());

    final DefaultListModel<DefaultGameChooserEntry> gameChooserModel = new DefaultListModel<>();
    downloadedMapsListing //
        .createGameChooserEntries()
        .forEach(gameChooserModel::addElement);

    final JList<DefaultGameChooserEntry> gameList = new JList<>(gameChooserModel);
    if (gameName == null || gameName.equals("-")) {
      gameList.setSelectedIndex(0);
    } else {
      IntStream.range(0, gameChooserModel.size())
          .mapToObj(gameChooserModel::get)
          .filter(entry -> entry.getGameName().equals(gameName))
          .findAny()
          .ifPresent(entry -> gameList.setSelectedValue(entry, true));
    }

    final JSplitPane mainSplit = new JSplitPane();
    dialog.add(mainSplit, BorderLayout.CENTER);
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

    Optional.ofNullable(gameList.getSelectedValue())
        .map(DefaultGameChooserEntry::readGameNotes)
        .ifPresent(notesPanel::setText);

    final JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
    infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    infoPanel.add(SwingComponents.newJScrollPane(notesPanel), BorderLayout.CENTER);

    mainSplit.setRightComponent(infoPanel);
    mainSplit.setBorder(null);

    final Runnable selectAndReturn = () -> dialog.setVisible(false);

    final JButton cancelButton =
        new JButtonBuilder("Cancel").actionListener(dialog::dispose).build();
    SwingKeyBinding.addKeyBinding(cancelButton, KeyCode.ESCAPE, dialog::dispose);

    final JPanel buttonsPanel =
        new JPanelBuilder()
            .boxLayoutHorizontal()
            .addHorizontalStrut(30)
            .add(Box.createGlue())
            .add(new JButtonBuilder("OK").actionListener(selectAndReturn).build())
            .add(cancelButton)
            .add(Box.createGlue())
            .build();
    dialog.add(buttonsPanel, BorderLayout.SOUTH);

    gameList.addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            Optional.ofNullable(gameList.getSelectedValue())
                .map(DefaultGameChooserEntry::readGameNotes)
                .ifPresent(notesPanel::setText);
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

    dialog.setSize(800, 600);
    dialog.setLocationRelativeTo(owner);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setVisible(true); // Blocking and waits for user action

    Optional.ofNullable(gameList.getSelectedValue())
        .flatMap(DefaultGameChooserEntry::getGameXmlFilePath)
        .ifPresent(gameChosenCallback);
  }
}
