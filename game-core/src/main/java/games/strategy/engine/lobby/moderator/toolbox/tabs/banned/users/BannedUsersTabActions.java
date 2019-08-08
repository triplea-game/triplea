package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import java.util.function.BiConsumer;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lombok.Builder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

@Builder
class BannedUsersTabActions {
  private final JFrame parentFrame;
  private final BannedUsersTabModel bannedUsersTabModel;

  BiConsumer<Integer, DefaultTableModel> removeButtonListener() {
    return (rowNum, tableModel) -> {
      final String bannedUserName = String.valueOf(tableModel.getValueAt(rowNum, 1));

      SwingComponents.promptUser(
          "Unban " + bannedUserName,
          "Are you sure you want to unban " + bannedUserName + "?",
          () -> {
            final String banId = String.valueOf(tableModel.getValueAt(rowNum, 0));
            bannedUsersTabModel.removeBan(banId);

            tableModel.removeRow(rowNum);
            MessagePopup.showMessage(parentFrame, "Unbanned: " + bannedUserName);
          });
    };
  }

  void refreshTableData(final JTable table) {
    JTableBuilder.setData(table, bannedUsersTabModel.fetchTableData());
  }
}
