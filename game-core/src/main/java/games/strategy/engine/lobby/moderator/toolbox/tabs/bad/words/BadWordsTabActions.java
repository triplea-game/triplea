package games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import java.util.function.BiConsumer;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lombok.AllArgsConstructor;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JTableBuilder;

/**
 * A model-like object that defines actions to be taken from the UI. This object is still aware of
 * UI components and interacts with the model to get and update backend data.
 */
@AllArgsConstructor
class BadWordsTabActions {

  static final String REMOVE_BUTTON_TEXT = "Remove";
  private final JFrame parentFrame;
  private final BadWordsTabModel badWordsTabModel;

  /**
   * Defines the action to be taken when moderator clicks the 'remove' button. Sends a 'remove'
   * request to the backend to remove the 'bad-word' value found on the corresponding table row..
   */
  BiConsumer<Integer, DefaultTableModel> removeButtonListener() {
    return (rowNumber, tableModel) -> {
      final String valueToRemove = (String) tableModel.getValueAt(rowNumber, 0);
      badWordsTabModel.removeBadWord(valueToRemove);
      MessagePopup.showMessage(parentFrame, "Removed: " + valueToRemove);
      tableModel.removeRow(rowNumber);
    };
  }

  /**
   * Adds a new bad word value, sends the value to the backend and updates the local UI table with
   * the new bad word.
   */
  void addBadWord(final JTable table, final String newBadWord) {
    badWordsTabModel.addBadWord(newBadWord);
    MessagePopup.showMessage(parentFrame, "Added: " + newBadWord);
    ((DefaultTableModel) table.getModel()).addRow(new String[] {newBadWord, REMOVE_BUTTON_TEXT});
    ButtonColumn.attachButtonColumn(table, 1, removeButtonListener());
  }

  void refresh(final JTable table) {
    JTableBuilder.setData(table, badWordsTabModel.fetchTableData());
  }
}
