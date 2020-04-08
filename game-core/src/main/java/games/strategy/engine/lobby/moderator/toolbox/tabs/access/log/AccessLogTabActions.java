package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import games.strategy.engine.lobby.client.ui.action.BanDuration;
import games.strategy.engine.lobby.client.ui.action.BanDurationDialog;
import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import games.strategy.engine.lobby.moderator.toolbox.tabs.Pager;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

class AccessLogTabActions {
  private static final int PAGE_SIZE = 50;

  private final JFrame parentFrame;

  private final AccessLogTabModel accessLogTabModel;
  private final Pager pager;

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
        BanDurationDialog.prompt(
            parentFrame,
            duration -> {
              confirmBan(
                  duration,
                  BanData.builder()
                      .username(String.valueOf(tableModel.getValueAt(rowNumber, 1)))
                      .ip(String.valueOf(tableModel.getValueAt(rowNumber, 2)))
                      .hashedMac(String.valueOf(tableModel.getValueAt(rowNumber, 3)))
                      .build());
            });
  }

  @Builder
  private static final class BanData {
    @Nonnull private final String username;
    @Nonnull private final String ip;
    @Nonnull private final String hashedMac;
  }

  private void confirmBan(final BanDuration banDuration, final BanData banData) {
    SwingComponents.promptUser(
        "Confirm Ban",
        "Are you sure you want to ban: " + banData.username + " for " + banDuration,
        () -> {
          accessLogTabModel.banUser(
              UserBanParams.builder()
                  .username(banData.username)
                  .ip(banData.ip)
                  .systemId(banData.hashedMac)
                  .minutesToBan(banDuration.toMinutes())
                  .build());

          MessagePopup.showMessage(parentFrame, banData.username + " banned for " + banDuration);
        });
  }
}
