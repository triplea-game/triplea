package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.SwingComponents;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import lombok.Builder;

/**
 * A model-like object that defines actions to be taken from the UI. This object is still aware of UI components
 * and interacts with the model to get and update backend data.
 */
@Builder
class BadWordsTabActions {

  private final BadWordsTabModel badWordsTabModel;
  private final JFrame parentFrame;

  /**
   * Defines the action to be taken when moderator clicks the 'remove' button. Sends a 'remove'
   * request to the backend to remove the 'bad-word' value found on the corresponding table row..
   */
  BiConsumer<Integer, DefaultTableModel> removeButtonListener() {
    return (rowNumber, tableModel) -> {
      final String valueToRemove = (String) tableModel.getValueAt(rowNumber, 0);

      final String result = badWordsTabModel.removeBadWord(valueToRemove);

      if (result.equalsIgnoreCase(ModeratorToolboxClient.SUCCESS)) {
        MessagePopup.showMessage(parentFrame, "Removed: " + valueToRemove);
        tableModel.removeRow(rowNumber);
      } else {
        SwingComponents.showDialog(
            "Error",
            "Failed to remove bad word:\n" + result);
      }
    };
  }

  /**
   * Returns a "2 x N " list of lists. The first column value is the bad word value from the bad words table,
   * the second column value is the "remove" button.
   */
  List<List<String>> getTableData() {
    try {
      // add a 'remove' column to the data.
      return badWordsTabModel.getBadWords()
          .stream()
          .map(word -> Arrays.asList(word, BadWordsTab.REMOVE_BUTTON_TEXT))
          .collect(Collectors.toList());
    } catch (final RuntimeException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Adds a new bad word value, sends the value to the backend and updates the local UI table with the new bad word.
   *
   * @return True if successful on the backend http server, false otherwise.
   */
  boolean addBadWord(final JTable table, final String newBadWord) {
    if (badWordsTabModel.addBadWord(newBadWord).equalsIgnoreCase(ModeratorToolboxClient.SUCCESS)) {
      MessagePopup.showMessage(parentFrame, "Added: " + newBadWord);
      ((DefaultTableModel) table.getModel()).addRow(new String[] {newBadWord, BadWordsTab.REMOVE_BUTTON_TEXT});
      ButtonColumn.attachButtonColumn(table, 1, removeButtonListener());
      return true;
    }

    SwingComponents.showDialog(
        "Error",
        "Failed to add bad word, if this keeps happening, contact TripleA development.");
    return false;
  }
}
