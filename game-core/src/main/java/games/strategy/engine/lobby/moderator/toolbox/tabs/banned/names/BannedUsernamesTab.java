package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Show a scrollable list of 'banned usernames'. These are exact match usernames not allowed in the
 * lobby. This tab shows a table with the banned usernames and a button with text field for adding a
 * custom username.
 *
 * <pre>
 * +----------------------------------------------------+
 * |    +----------------------------------------+    |^|
 * |    | Name   | Date Banned |                 |    | |
 * |    +----------------------------------------+    | |
 * |    | name1  | date        | {Remove Button} |    | |
 * |    | name2  | date        | {Remove Button} |    | |
 * |    +----------------------------------------+    |v|
 * +----------------------------------------------------+
 * | ADD LABEL   |      TEXT FIELD |      SUBMIT BUTTON |
 * +----------------------------------------------------+
 * </pre>
 */
public final class BannedUsernamesTab implements Supplier<Component> {
  private static final int MIN_LENGTH = 4;
  private final BannedUsernamesTabModel bannedUsernamesTabModel;
  private final BannedUsernamesTabActions bannedUsernamesTabActions;

  public BannedUsernamesTab(
      final JFrame parentFrame, final ToolboxUsernameBanClient toolboxUsernameBanClient) {
    bannedUsernamesTabModel = new BannedUsernamesTabModel(toolboxUsernameBanClient);
    bannedUsernamesTabActions = new BannedUsernamesTabActions(parentFrame, bannedUsernamesTabModel);
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
                .actionListener(() -> bannedUsernamesTabActions.refreshTableData(table))
                .build())
        .addCenter(SwingComponents.newJScrollPane(table))
        .addSouth(buildAddUsernameBanPanel(table))
        .build();
  }

  private JTable buildTable() {
    final JTable table =
        JTableBuilder.builder()
            .columnNames(BannedUsernamesTabModel.fetchTableHeaders())
            .tableData(bannedUsernamesTabModel.fetchTableData())
            .build();

    ButtonColumn.attachButtonColumn(table, 2, bannedUsernamesTabActions.removeButtonListener());
    return table;
  }

  private JPanel buildAddUsernameBanPanel(final JTable table) {
    final JTextField addField =
        JTextFieldBuilder.builder()
            .columns(10)
            .maxLength(20)
            .toolTip("Username to ban, must be at least " + MIN_LENGTH + " characters long")
            .build();

    final JButton addButton =
        new JButtonBuilder()
            .enabled(false)
            .title("Ban Username")
            .actionListener(
                button -> bannedUsernamesTabActions.addUserNameBan(addField, button, table))
            .toolTip("Adds a new banned username, this name will not be allowed to join the lobby.")
            .build();

    new DocumentListenerBuilder(
            () -> addButton.setEnabled(addField.getText().trim().length() >= MIN_LENGTH))
        .attachTo(addField);

    return new JPanelBuilder()
        .add(
            new JPanelBuilder()
                .flowLayout()
                .add(addField)
                .add(Box.createHorizontalStrut(10))
                .add(addButton)
                .build())
        .build();
  }
}
