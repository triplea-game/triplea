package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Show a scrollable list of 'banned users'. Bans are recorded by IP address and Hashed Mac.
 *
 * <pre>
 * +--------------------------------------------------------------------------------+
 * | +--------------------------------------------------------------------------+ |^|
 * | | Name  | Access Date | IP  | Mac   | Registered |            |            | | |
 * | +--------------------------------------------------------------------------+ | |
 * | | name1 | Date        | IP1 |  mac1 |    T/F     | {Ban Name} | {Ban User} | | |
 * | +--------------------------------------------------------------------------+ |v|
 * +--------------------------------------------------------------------------------+
 * </pre>
 */
public final class AccessLogTab implements Supplier<Component> {

  private final AccessLogTabActions accessLogTabActions;

  public AccessLogTab(
      final JFrame parentFrame,
      final ToolboxAccessLogClient toolboxAccessLogClient,
      final ToolboxUserBanClient toolboxUserBanClient,
      final ToolboxUsernameBanClient toolboxUsernameBanClient) {
    accessLogTabActions =
        new AccessLogTabActions(
            parentFrame,
            new AccessLogTabModel(
                toolboxAccessLogClient, toolboxUserBanClient, toolboxUsernameBanClient));
  }

  @Override
  public Component get() {
    final JButton loadMoreButton = new JButtonBuilder().title("Load More").build();

    final JTable table = buildTable(loadMoreButton);

    loadMoreButton.addActionListener(e -> accessLogTabActions.loadMore(table, loadMoreButton));

    return new JPanelBuilder()
        .border(10)
        .borderLayout()
        .addNorth(
            new JButtonBuilder()
                .title("Refresh")
                .actionListener(() -> accessLogTabActions.reload(table, loadMoreButton))
                .build())
        .addCenter(SwingComponents.newJScrollPane(table))
        .addSouth(loadMoreButton)
        .build();
  }

  private JTable buildTable(final JButton loadMoreButton) {
    final JTable table =
        JTableBuilder.builder().columnNames(AccessLogTabModel.fetchTableHeaders()).build();

    accessLogTabActions.reload(table, loadMoreButton);

    ButtonColumn.attachButtonColumn(table, 5, accessLogTabActions.banUserName());
    ButtonColumn.attachButtonColumn(table, 6, accessLogTabActions.banUser());
    return table;
  }
}
