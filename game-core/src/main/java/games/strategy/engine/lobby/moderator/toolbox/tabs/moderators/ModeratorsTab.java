package games.strategy.engine.lobby.moderator.toolbox.tabs.moderators;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.swing.ButtonColumn;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.KeyTypeValidator;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** Tab with a table showing list of moderators. Super-mods will see additional controls. */
public final class ModeratorsTab implements Supplier<Component> {
  private static final int USERNAME_MIN_LENGTH = 4;
  private final JFrame parentFrame;
  private final ModeratorsTabModel moderatorsTabModel;
  private final ModeratorsTabActions moderatorsTabActions;

  public ModeratorsTab(
      final JFrame parentFrame,
      final ToolboxModeratorManagementClient toolboxModeratorManagementClient) {
    this.parentFrame = parentFrame;
    this.moderatorsTabModel = new ModeratorsTabModel(toolboxModeratorManagementClient);
    this.moderatorsTabActions = new ModeratorsTabActions(parentFrame, moderatorsTabModel);
  }

  @Override
  public Component get() {
    final JTable dataTable = buildTable();

    final JPanel panel =
        new JPanelBuilder()
            .border(10)
            .borderLayout()
            .addNorth(
                new JButtonBuilder()
                    .title("Refresh")
                    .actionListener(() -> moderatorsTabActions.refreshTableData(dataTable))
                    .build())
            .addCenter(SwingComponents.newJScrollPane(dataTable))
            .build();

    if (moderatorsTabModel.isAdmin()) {
      panel.add(buildAddModeratorPanel(), BorderLayout.SOUTH);
    }
    return panel;
  }

  private JTable buildTable() {
    final JTable table =
        JTableBuilder.builder()
            .columnNames(moderatorsTabModel.fetchTableHeaders())
            .tableData(moderatorsTabModel.fetchTableData())
            .build();
    if (moderatorsTabModel.isAdmin()) {
      ButtonColumn.attachButtonColumn(table, 2, moderatorsTabActions.removeModAction(parentFrame));
      ButtonColumn.attachButtonColumn(table, 3, moderatorsTabActions.addAdminAction(parentFrame));
    }
    return table;
  }

  private Component buildAddModeratorPanel() {
    final JTextField addField =
        JTextFieldBuilder.builder()
            .columns(7)
            .maxLength(30)
            .toolTip("Username to add to moderators")
            .build();

    final JButton addButton =
        new JButtonBuilder()
            .enabled(false)
            .title("Add moderator")
            .actionListener(button -> moderatorsTabActions.addModerator(addField, button))
            .toolTip("Promotes a user to the moderators")
            .build();

    // This is a very fancy listener that will validate the entered requested new moderator
    // name exists (as the user types) before we enable the submit button.
    new KeyTypeValidator()
        .attachKeyTypeValidator(
            addField,
            usernameRequested ->
                usernameRequested.length() >= USERNAME_MIN_LENGTH
                    && moderatorsTabModel.checkUserExists(usernameRequested),
            addButton::setEnabled);

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
