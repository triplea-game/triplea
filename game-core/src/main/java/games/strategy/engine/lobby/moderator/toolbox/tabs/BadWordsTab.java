package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.triplea.swing.ButtonColumn;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Show a scrollable list of 'bad words'. This are words which may not be contained
 * in user names. This tab has a table with the bad words (DB table contents), a remove
 * button for each and a text field at bottom with submit button to add new values.
 * Eg:
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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BadWordsTab {
  static final String REMOVE_BUTTON_TEXT = "Remove";
  private static final int MIN_LENGTH = 4;

  static Component buildTab(final BadWordsTabActions badWordsTabActions) {
    final JTable table = buildTable(badWordsTabActions);

    return JPanelBuilder.builder()
        .border(10)
        .addNorth(new JLabel("Bad words are not allowed to be contained in any user name"))
        .addCenter(SwingComponents.newJScrollPane(table))
        .addSouth(buildAddBadWordsPanel(table, badWordsTabActions))
        .build();
  }

  /**
   * Table is a simple two column table, first is the bad word entry, second is a 'remove' button
   * to remove that entry.
   */
  private static JTable buildTable(final BadWordsTabActions badWordsTabActions) {
    final JTable table = JTableBuilder.builder()
        .columnNames("Bad Word", "")
        .tableData(badWordsTabActions.getTableData())
        .build();

    ButtonColumn.attachButtonColumn(table, 1, badWordsTabActions.removeButtonListener());

    return table;
  }


  private static JPanel buildAddBadWordsPanel(final JTable table, final BadWordsTabActions badWordsTabActions) {
    final JTextField addField = JTextFieldBuilder.builder()
        .columns(10)
        .maxLength(20)
        .toolTip("Bad word to add, must be at least " + MIN_LENGTH + " characters long")
        .build();

    final JButton addButton = JButtonBuilder.builder()
        .enabled(false)
        .title("Add Bad Word")
        .actionListener(button -> {
          final String newBadWord = addField.getText();
          if (badWordsTabActions.addBadWord(table, newBadWord)) {
            addField.setText("");
            button.setEnabled(false);
          }
        })
        .toolTip("Adds a new bad word to the table, must be at least " + MIN_LENGTH + " characters long")
        .build();

    DocumentListenerBuilder.attachDocumentListener(
        addField,
        () -> addButton.setEnabled(addField.getText().trim().length() >= MIN_LENGTH));

    return JPanelBuilder.builder()
        .gridLayout(1, 2)
        .add(JPanelBuilder.builder()
            .flowLayout()
            .add(addField)
            .add(Box.createHorizontalStrut(10))
            .add(addButton)
            .build())
        .build();
  }
}
