package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import java.awt.Component;
import java.time.Instant;
import java.util.function.BiConsumer;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import lombok.AllArgsConstructor;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JTableBuilder;

@AllArgsConstructor
final class BannedUsernamesTabActions {

  private final JFrame parentFrame;
  private final BannedUsernamesTabModel bannedUsernamesTabModel;

  BiConsumer<Integer, DefaultTableModel> removeButtonListener() {
    return (rowNum, tableModel) -> {
      final String bannedUserName = String.valueOf(tableModel.getValueAt(rowNum, 0));
      bannedUsernamesTabModel.removeUsernameBan(bannedUserName);
      tableModel.removeRow(rowNum);
      MessagePopup.showMessage(parentFrame, "Removed banned username: " + bannedUserName);
    };
  }

  void addUserNameBan(final JTextField addField, final Component button, final JTable table) {
    final String newBannedUserName = addField.getText().trim();
    bannedUsernamesTabModel.addUsernameBan(newBannedUserName);

    ((DefaultTableModel) table.getModel())
        .addRow(new String[] {newBannedUserName, Instant.now().toString(), "Remove"});
    ButtonColumn.attachButtonColumn(table, 2, removeButtonListener());

    addField.setText("");
    button.setEnabled(false);
  }

  void refreshTableData(final JTable table) {
    JTableBuilder.setData(table, bannedUsernamesTabModel.fetchTableData());
  }
}
