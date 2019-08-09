package games.strategy.engine.lobby.moderator.toolbox.tabs.api.keys;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JTable;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

/**
 * API key tab allows moderators to view their API keys, remove keys, and issue new temporary keys
 * that can be used to log in from additional computers.
 *
 * <pre>
 * +---------------------------------------------------------------------+
 * |   WINDOW LABEL HEADER                                               |
 * +---------------------------------------------------------------------+
 * |    +-----------------------------------------------------------+  |^|
 * |    | key_id  |  last_used_date  | last_used_ip | delete button |  | |
 * |    +-----------------------------------------------------------+  |v|
 * +---------------------------------------------------------------------+
 * |           Add Single-Use-Key Button                                 |
 * +---------------------------------------------------------------------+
 * </pre>
 */
public final class ApiKeysTab implements Supplier<Component> {

  private static final String WINDOW_HEADER_LABEL =
      "<html>Use this window to remove keys that are no "
          + "longer needed and to issue new single-use-keys. <br />"
          + "Single use keys can be used to register from additional computers.";

  private final JFrame parentFrame;
  private final ApiKeyTabActions apiKeyTabActions;
  private final ApiKeyTabModel apiKeyTabModel;

  public ApiKeysTab(final JFrame parentFrame, final ToolboxApiKeyClient toolboxApiKeyClient) {
    this.parentFrame = parentFrame;
    apiKeyTabModel = new ApiKeyTabModel(toolboxApiKeyClient);
    apiKeyTabActions = new ApiKeyTabActions(apiKeyTabModel);
  }

  @Override
  public Component get() {
    return JPanelBuilder.builder()
        .border(10)
        .addNorth(JLabelBuilder.builder().text(WINDOW_HEADER_LABEL).border(15).build())
        .addCenter(
            SwingComponents.newJScrollPane(
                buildTable(parentFrame, apiKeyTabActions, apiKeyTabModel)))
        .addSouth(
            JButtonBuilder.builder()
                .title("Create New Single-Use Key")
                .actionListener(apiKeyTabActions.createSingleUseKeyAction(parentFrame))
                .build())
        .build();
  }

  /**
   * Table is a simple two column table, first is the bad word entry, second is a 'remove' button to
   * remove that entry.
   */
  private JTable buildTable(
      final JFrame parentFrame,
      final ApiKeyTabActions apiKeyTabActions,
      final ApiKeyTabModel apiKeyTabModel) {
    final JTable table =
        JTableBuilder.builder()
            .columnNames(ApiKeyTabModel.fetchTableHeaders())
            .tableData(apiKeyTabModel.fetchTableData())
            .build();
    ButtonColumn.attachButtonColumn(table, 3, apiKeyTabActions.deleteKeyAction(parentFrame));
    return table;
  }
}
