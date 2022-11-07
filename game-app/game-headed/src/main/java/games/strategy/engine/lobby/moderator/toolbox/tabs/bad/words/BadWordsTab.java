package games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Show a scrollable list of 'bad words'. This are words which may not be contained in user names.
 * This tab has a table with the bad words (DB table contents), a remove button for each and a text
 * field at bottom with submit button to add new values. Eg:
 *
 * <pre>
 * +-----------------------------------------+
 * |   WINDOW LABEL HEADER                   |
 * +-----------------------------------------+
 * |    +-----------------------------+     ^|
 * |    | Header    |                 |    | |
 * |    +-----------------------------+    | |
 * |    | word1     | {Remove Button} |    | |
 * |    | word2     | {Remove Button} |    | |
 * |    +-----------------------------+     v|
 * +-----------------------------------------+
 * | ADD LABEL |  TEXT FIELD | SUBMIT BUTTON |
 * +-----------------------------------------+
 * </pre>
 */
public final class BadWordsTab implements Supplier<Component> {
  private static final int MIN_LENGTH = 4;

  private final BadWordsTabActions badWordsTabActions;
  private final BadWordsTabModel badWordsTabModel;

  public BadWordsTab(final JFrame parentFrame, final ToolboxBadWordsClient toolboxBadWordsClient) {
    badWordsTabModel = new BadWordsTabModel(toolboxBadWordsClient);
    badWordsTabActions = new BadWordsTabActions(parentFrame, badWordsTabModel);
  }

  @Override
  public Component get() {
    final JTable table = buildTable();

    return new JPanelBuilder()
        .border(10)
        .borderLayout()
        .addNorth(
            new JPanelBuilder()
                .borderLayout()
                .addNorth(new JLabel("Bad words are not allowed to be contained in any user name"))
                .addCenter(
                    new JButtonBuilder()
                        .title("Refresh")
                        .actionListener(() -> badWordsTabActions.refresh(table))
                        .build())
                .build())
        .addCenter(SwingComponents.newJScrollPane(table))
        .addSouth(buildAddBadWordsPanel(table))
        .build();
  }

  /**
   * Table is a simple two column table, first is the bad word entry, second is a 'remove' button to
   * remove that entry.
   */
  private JTable buildTable() {
    final JTable table =
        JTableBuilder.builder()
            .columnNames(BadWordsTabModel.fetchTableHeaders())
            .tableData(badWordsTabModel.fetchTableData())
            .build();

    ButtonColumn.attachButtonColumn(table, 1, badWordsTabActions.removeButtonListener());

    return table;
  }

  private JPanel buildAddBadWordsPanel(final JTable table) {
    final JTextField addField =
        JTextFieldBuilder.builder()
            .columns(10)
            .maxLength(20)
            .toolTip("Bad word to add, must be at least " + MIN_LENGTH + " characters long")
            .build();

    final JButton addButton =
        new JButtonBuilder()
            .enabled(false)
            .title("Add Bad Word")
            .actionListener(
                button -> {
                  final String newBadWord = addField.getText();
                  badWordsTabActions.addBadWord(table, newBadWord);
                  addField.setText("");
                  button.setEnabled(false);
                })
            .toolTip(
                "Adds a new bad word to the table, must be at least "
                    + MIN_LENGTH
                    + " characters long")
            .build();

    new DocumentListenerBuilder(
            () -> addButton.setEnabled(addField.getText().trim().length() >= MIN_LENGTH))
        .attachTo(addField);

    return new JPanelBuilder()
        .add(
            new JPanelBuilder()
                .add(addField)
                .add(Box.createHorizontalStrut(10))
                .add(addButton)
                .build())
        .build();
  }
}
