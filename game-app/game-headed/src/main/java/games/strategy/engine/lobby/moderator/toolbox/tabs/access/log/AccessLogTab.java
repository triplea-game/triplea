package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogSearchRequest;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Show a scrollable list of 'banned users'. Bans are recorded by IP address and Hashed Mac.
 *
 * <pre>
 * +-----------------------------------------------------------------------------------+
 * |                                                                                    |
 * | +-----------------------------------------------------------------------------+ |^|
 * | | Name  | Access Date | IP  | SystemId | Registered |            |            | | |
 * | +-----------------------------------------------------------------------------+ | |
 * | | name1 | Date        | IP1 | systemId |    T/F     | {Ban Name} | {Ban User} | | |
 * | +-----------------------------------------------------------------------------+ |v|
 * +-----------------------------------------------------------------------------------+
 * </pre>
 */
public final class AccessLogTab implements Supplier<Component> {

  private final AccessLogTabActions accessLogTabActions;
  private final JFrame parentFrame;

  public AccessLogTab(
      final JFrame parentFrame,
      final ToolboxAccessLogClient toolboxAccessLogClient,
      final ToolboxUserBanClient toolboxUserBanClient,
      final ToolboxUsernameBanClient toolboxUsernameBanClient) {
    this.parentFrame = parentFrame;
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
        .addSouth(
            new JPanelBuilder()
                .borderLayout()
                .addNorth(loadMoreButton)
                .addSouth(searchPanel(table, loadMoreButton))
                .build())
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

  private JComponent searchPanel(final JTable table, final JButton loadMoreButton) {
    final JTextField username = new JTextFieldBuilder().columns(8).build();
    final JTextField ip = new JTextFieldBuilder().columns(6).build();
    final JTextField systemId = new JTextFieldBuilder().columns(10).build();
    final Runnable searchAction =
        () ->
            accessLogTabActions.search(
                AccessLogSearchRequest.builder()
                    .username(username.getText())
                    .ip(ip.getText())
                    .systemId(systemId.getText())
                    .build(),
                table,
                loadMoreButton);

    final JButton searchButton = new JButtonBuilder("Search").actionListener(searchAction).build();
    username.addActionListener(e -> searchAction.run());
    ip.addActionListener(e -> searchAction.run());
    systemId.addActionListener(e -> searchAction.run());

    final JButton searchTips =
        new JButtonBuilder("(SearchTips)")
            .actionListener(
                () ->
                    SwingComponents.showDialog(
                        parentFrame,
                        "Search Tips",
                        "By default all searches are exact matches.\n"
                            + "\n"
                            + "Using '%' is called wildcard matching and matches anything,\n"
                            + "all fields support it.\n\n"
                            + "  Examples:\n\n"
                            + "  user"
                            + "      Finds all usernames exactly matching 'user'\n\n"
                            + "  user%\n"
                            + "      Finds all usernames starting with 'user'\n\n"
                            + "  %user%\n"
                            + "      Finds all usernames containing 'user'\n\n"
                            + "  %user\n"
                            + "      Finds all usernames ending with 'user'\n\n"
                            + "  user%hat\n"
                            + "      Find all usernames starting with 'user'\n"
                            + "      and ending with 'hat'\n"
                            + "\n"
                            + "Entire access log table is searched, no time limit.\n"
                            + "Exact searches and starts-with searches are the least "
                            + "taxing on the database.\n"))
            .build();

    return new JPanelBuilder()
        .flowLayout()
        .addLabel("Username")
        .add(username)
        .addLabel("IP")
        .add(ip)
        .addLabel("System ID")
        .add(systemId)
        .add(searchButton)
        .addLabel("")
        .add(searchTips)
        .build();
  }
}
