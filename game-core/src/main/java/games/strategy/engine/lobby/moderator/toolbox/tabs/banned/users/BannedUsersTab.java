package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JTable;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Show a scrollable list of 'banned users'. Bans are recorded by IP address and Hashed Mac.
 *
 * <pre>
 * +--------------------------------------------------------------------+
 * | +-------------------------------------------------------------+  |^|
 * | | Name  | Ban Date | IP  | Hashed Mac | Ban Expiry |          |  | |
 * | +-------------------------------------------------------------+  | |
 * | | name1 | Date     | IP1 |  mac1      |  date      | {Remove} |  | |
 * | +-------------------------------------------------------------+  |v|
 * +--------------------------------------------------------------------+
 * </pre>
 */
public final class BannedUsersTab implements Supplier<Component> {
  private final BannedUsersTabActions bannedUsersTabActions;
  private final BannedUsersTabModel bannedUsersTabModel;

  public BannedUsersTab(final JFrame parentFrame, final ToolboxUserBanClient toolboxUserBanClient) {
    bannedUsersTabModel = new BannedUsersTabModel(toolboxUserBanClient);
    bannedUsersTabActions = new BannedUsersTabActions(parentFrame, bannedUsersTabModel);
  }

  @Override
  public Component get() {
    final JTable table = buildTable();

    return new JPanelBuilder()
        .border(10)
        .borderLayout()
        .addNorth(
            new JButtonBuilder()
                .title("Refresh")
                .actionListener(() -> bannedUsersTabActions.refreshTableData(table))
                .build())
        .addCenter(SwingComponents.newJScrollPane(table))
        .build();
  }

  private JTable buildTable() {
    final JTable table =
        JTableBuilder.builder()
            .columnNames(BannedUsersTabModel.fetchTableHeaders())
            .tableData(bannedUsersTabModel.fetchTableData())
            .build();

    ButtonColumn.attachButtonColumn(table, 6, bannedUsersTabActions.removeButtonListener());
    return table;
  }
}
