package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JTable;

import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

class EventLogTab {

  static Component buildTab(final EventLogTabModel eventLogTabModel) {
    final JTable dataTable = eventLogTable(eventLogTabModel);
    final JButton loadMoreButton = loadMoreButton(dataTable, eventLogTabModel);

    return JPanelBuilder.builder()
        .border(30)
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

  private static JButton loadMoreButton(final JTable table, final EventLogTabModel eventLogTabModel) {
    return JButtonBuilder.builder()
        .title("Load More")
        .actionListener(() -> JTableBuilder.addRows(table, eventLogTabModel.loadMore()))
        .build();
  }
}
