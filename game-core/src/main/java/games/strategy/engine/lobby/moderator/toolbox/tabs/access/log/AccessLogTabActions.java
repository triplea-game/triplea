package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import games.strategy.engine.lobby.client.ui.action.ActionDuration;
import games.strategy.engine.lobby.client.ui.action.ActionDurationDialog;
import games.strategy.engine.lobby.client.ui.action.ActionDurationDialog.ActionName;
import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import games.strategy.engine.lobby.moderator.toolbox.tabs.Pager;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogSearchRequest;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

@Log
class AccessLogTabActions {
  private static final int PAGE_SIZE = 50;

  private final JFrame parentFrame;

  private final AccessLogTabModel accessLogTabModel;
  private Pager pager;

  AccessLogTabActions(final JFrame parentFrame, final AccessLogTabModel accessLogTabModel) {
    this.parentFrame = parentFrame;
    this.accessLogTabModel = accessLogTabModel;

    pager =
        Pager.builder().dataFetcher(accessLogTabModel::fetchTableData).pageSize(PAGE_SIZE).build();
  }

  void reload(final JTable table, final JButton loadMoreButton) {
    final List<List<String>> tableData = pager.getTableData();
    disableButtonIfEndOfData(tableData, loadMoreButton);
    JTableBuilder.setData(table, tableData);
  }

  void loadMore(final JTable table, final JButton loadMoreButton) {
    final List<List<String>> moreData = pager.loadMoreData();
    disableButtonIfEndOfData(moreData, loadMoreButton);
    JTableBuilder.addRows(table, moreData);
  }

  void search(
      final AccessLogSearchRequest accessLogSearchRequest,
      final JTable table,
      final JButton loadMoreButton) {
    AsyncRunner.runAsync(() -> runSearch(accessLogSearchRequest, table, loadMoreButton))
        .exceptionally(
            e ->
                log.log(
                    Level.SEVERE,
                    "Error running search with request: " + accessLogSearchRequest,
                    e));
  }

  private void runSearch(
      final AccessLogSearchRequest accessLogSearchRequest,
      final JTable table,
      final JButton loadMoreButton) {
    pager =
        Pager.builder()
            .pageSize(PAGE_SIZE)
            .dataFetcher(
                pagingParams ->
                    accessLogTabModel.fetchSearchData(accessLogSearchRequest, pagingParams))
            .build();

    final List<List<String>> tableData = pager.getTableData();
    SwingUtilities.invokeLater(
        () -> {
          disableButtonIfEndOfData(tableData, loadMoreButton);
          JTableBuilder.setData(table, tableData);
        });
  }

  private void disableButtonIfEndOfData(
      final List<List<String>> tableData, final JButton loadMoreButton) {
    loadMoreButton.setEnabled(tableData.size() >= PAGE_SIZE);
  }

  BiConsumer<Integer, DefaultTableModel> banUserName() {
    return (rowNumber, tableModel) -> {
      final String username = String.valueOf(tableModel.getValueAt(rowNumber, 1));

      SwingComponents.promptUser(
          "Ban Username?",
          "Ban username: " + username + "?",
          () -> accessLogTabModel.banUserName(username));
    };
  }

  BiConsumer<Integer, DefaultTableModel> banUser() {
    return (rowNumber, tableModel) ->
        ActionDurationDialog.builder()
            .parent(parentFrame)
            .actionName(ActionName.BAN)
            .build()
            .prompt()
            .map(
                duration ->
                    BanData.builder()
                        .actionDuration(duration)
                        .username(String.valueOf(tableModel.getValueAt(rowNumber, 1)))
                        .ip(String.valueOf(tableModel.getValueAt(rowNumber, 2)))
                        .hashedMac(String.valueOf(tableModel.getValueAt(rowNumber, 3)))
                        .build())
            .ifPresent(this::confirmAndExecuteBan);
  }

  @Builder
  private static final class BanData {
    @Nonnull private final String username;
    @Nonnull private final String ip;
    @Nonnull private final String hashedMac;
    @Nonnull private final ActionDuration actionDuration;
  }

  private void confirmAndExecuteBan(final BanData banData) {
    SwingComponents.promptUser(
        "Confirm Ban",
        "Are you sure you want to ban: " + banData.username + " for " + banData.actionDuration,
        () -> {
          accessLogTabModel.banUser(
              UserBanParams.builder()
                  .username(banData.username)
                  .ip(banData.ip)
                  .systemId(banData.hashedMac)
                  .minutesToBan(banData.actionDuration.toMinutes())
                  .build());

          MessagePopup.showMessage(
              parentFrame, banData.username + " banned for " + banData.actionDuration);
        });
  }
}
