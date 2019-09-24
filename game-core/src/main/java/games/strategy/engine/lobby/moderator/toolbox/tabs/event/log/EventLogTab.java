package games.strategy.engine.lobby.moderator.toolbox.tabs.event.log;

import com.google.common.base.Preconditions;
import games.strategy.engine.lobby.moderator.toolbox.tabs.Pager;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JTable;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Simple tab that is a table containing entries from the moderator audit history table. Contains a
 * 'refresh' button to reload the table data and a 'load more' button to fetch additional (paged)
 * results. Eg:
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
public final class EventLogTab implements Supplier<Component> {
  private static final int PAGE_SIZE = 30;

  private final Pager pager;

  public EventLogTab(final ToolboxEventLogClient toolboxEventLogClient) {
    final EventLogTabModel eventLogTabModel = new EventLogTabModel(toolboxEventLogClient);
    Preconditions.checkNotNull(eventLogTabModel);
    pager =
        Pager.builder().pageSize(PAGE_SIZE).dataFetcher(eventLogTabModel::fetchTableData).build();
  }

  @Override
  public Component get() {
    final JTable dataTable = eventLogTable();
    final JButton loadMoreButton = loadMoreButton(dataTable);
    final JButton refreshButton = refreshButton(dataTable, loadMoreButton);

    return new JPanelBuilder()
        .border(30)
        .borderLayout()
        .addNorth(refreshButton)
        .addCenter(SwingComponents.newJScrollPane(dataTable))
        .addSouth(loadMoreButton)
        .build();
  }

  private JTable eventLogTable() {
    return JTableBuilder.builder()
        .columnNames(EventLogTabModel.fetchTableHeaders())
        .tableData(pager.getTableData())
        .build();
  }

  private JButton refreshButton(final JTable table, final JButton loadMoreButton) {
    return new JButtonBuilder()
        .title("Refresh")
        .actionListener(
            () -> {
              loadMoreButton.setEnabled(true);
              JTableBuilder.setData(table, pager.getTableData());
            })
        .build();
  }

  private JButton loadMoreButton(final JTable table) {
    return new JButtonBuilder()
        .title("Load More")
        .actionListener(
            button -> {
              final List<List<String>> newData = pager.loadMoreData();
              if (newData.isEmpty()) {
                button.setEnabled(false);
              } else {
                JTableBuilder.addRows(table, newData);
              }
            })
        .build();
  }
}
