package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTable;

import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Simple tab that is a table containing entries from the moderator audit history table.
 * Contains a 'refresh' button to reload the table data and a 'load more' button to
 * fetch additional (paged) results.
 * Eg:
 *
 * <pre>
 * +-------------------------------------------------+
 * |   REFRESH BUTTON                                |
 * +----------------------------------------------=--+
 * |    +---------------------------------------+   ^|
 * |    | DATE    |  MOD NAME | ACTION | TARGET |  | |
 * |    +---------------------------------------+  | |
 * |    |         |           |        |        |  | |
 * |    +---------------------------------------+   v|
 * +-------------------------------------------------+
 * |   LOAD MORE BUTTON                              |
 * +-------------------------------------------------+
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EventLogTab {

  static Component buildTab(final EventLogTabModel eventLogTabModel) {
    final JTable dataTable = eventLogTable(eventLogTabModel);
    final JButton loadMoreButton = loadMoreButton(dataTable, eventLogTabModel);
    final JButton refreshButton = refreshButton(dataTable, eventLogTabModel, loadMoreButton);

    return JPanelBuilder.builder()
        .border(30)
        .addNorth(refreshButton)
        .addCenter(SwingComponents.newJScrollPane(dataTable))
        .addSouth(loadMoreButton)
        .build();
  }

  private static JTable eventLogTable(final EventLogTabModel eventLogTabModel) {
    return JTableBuilder.builder()
        .columnNames(EventLogTabModel.getEventLogTableHeaders())
        .tableData(eventLogTabModel.getEventLogTableData())
        .build();
  }

  private static JButton refreshButton(
      final JTable table, final EventLogTabModel eventLogTabModel, final JButton loadMoreButton) {
    return JButtonBuilder.builder()
        .title("Refresh")
        .actionListener(() -> {
          loadMoreButton.setEnabled(true);
          JTableBuilder.setData(table, eventLogTabModel.getEventLogTableData());
        })
        .build();
  }

  private static JButton loadMoreButton(final JTable table, final EventLogTabModel eventLogTabModel) {
    return JButtonBuilder.builder()
        .title("Load More")
        .actionListener(button -> {
          final List<List<String>> newData = eventLogTabModel.loadMore();
          if (newData.isEmpty()) {
            button.setEnabled(false);
          } else {
            JTableBuilder.addRows(table, newData);
          }
        })
        .build();
  }
}
