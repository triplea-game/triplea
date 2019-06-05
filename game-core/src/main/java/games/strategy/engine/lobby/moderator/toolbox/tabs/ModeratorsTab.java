package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JTable;

import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;


/**
 * Tab with a table showing moderators with a button to add moderator.
 */
class ModeratorsTab {

  private static final String REMOVE_BUTTON_TEXT = "Remove";

  static Component buildTab(final JFrame parent, final ModeratorsTabModel moderatorsTabModel) {

    final JTable table = buildTable(parent, moderatorsTabModel);

    return JPanelBuilder.builder()
        .border(10)
        .addCenter(SwingComponents.newJScrollPane(table))
        .build();
  }



  private static JTable buildTable(final JFrame parent, final ModeratorsTabModel moderatorsTabModel) {
    final List<String> headers = new ArrayList<>(Arrays.asList("Moderator", "Mod Since"));
    if (moderatorsTabModel.isModeratorAdmin()) {
      headers.add(" "); // remove button column
    }

    final JTable table = JTableBuilder.builder()
        .columnNames(headers)
        .tableData(
            // add a 'remove' column to the data.
            moderatorsTabModel.getModeratorList()
                .stream()
                .map(word -> Arrays.asList(word, REMOVE_BUTTON_TEXT))
                .collect(Collectors.toList()))
        .build();

    return table;
  }

}
