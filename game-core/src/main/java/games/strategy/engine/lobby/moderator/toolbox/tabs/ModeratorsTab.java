package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;

import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Tab with a table showing moderators with a button to add moderator.
 * TODO: WORK IN PROGRESS!
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ModeratorsTab {

  private static final String REMOVE_BUTTON_TEXT = "Remove";

  static Component buildTab(final ModeratorsTabModel moderatorsTabModel) {

    final JTable table = buildTable(moderatorsTabModel);

    return JPanelBuilder.builder()
        .border(10)
        .addCenter(SwingComponents.newJScrollPane(table))
        .build();
  }

  private static JTable buildTable(final ModeratorsTabModel moderatorsTabModel) {
    final List<String> headers = new ArrayList<>(Arrays.asList("Moderator", "Mod Since"));
    if (moderatorsTabModel.isModeratorAdmin()) {
      headers.add(" "); // remove button column
    }

    return JTableBuilder.builder()
        .columnNames(headers)
        .tableData(
            // add a 'remove' column to the data.
            ModeratorsTabModel.getModeratorList()
                .stream()
                .map(word -> Arrays.asList(word, REMOVE_BUTTON_TEXT))
                .collect(Collectors.toList()))
        .build();
  }

}
