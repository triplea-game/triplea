package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import games.strategy.engine.lobby.moderator.toolbox.tabs.Pager;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

class AccessLogTabActions {
  private static final int PAGE_SIZE = 50;

  private static final String ONE_HOUR = "1 Hour";
  private static final String ONE_DAY = "1 Day";
  private static final String ONE_WEEK = "1 Week";
  private static final String THREE_WEEKS = "3 Weeks";

  private static final ImmutableMap<String, Integer> TIME_DURATION_MAP =
      ImmutableMap.of(
          ONE_HOUR, 1,
          ONE_DAY, (int) TimeUnit.DAYS.toHours(1),
          ONE_WEEK, (int) TimeUnit.DAYS.toHours(7),
          THREE_WEEKS, (int) TimeUnit.DAYS.toHours(21));

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
        promptForBanDuration(
            BanData.builder()
                .username(String.valueOf(tableModel.getValueAt(rowNumber, 1)))
                .ip(String.valueOf(tableModel.getValueAt(rowNumber, 2)))
                .hashedMac(String.valueOf(tableModel.getValueAt(rowNumber, 3)))
                .build());
  }

  @Builder
  private static final class BanData {
    @Nonnull private final String username;
    @Nonnull private final String ip;
    @Nonnull private final String hashedMac;
  }

  private void promptForBanDuration(final BanData banData) {
    final JDialog dialog =
        new JDialog(JOptionPane.getFrameForComponent(parentFrame), "Ban " + banData.username, true);
    dialog
        .getContentPane()
        .add(
            new JPanelBuilder()
                .borderLayout()
                .addNorth(new JLabel("How long to ban?"))
                .addCenter(banDurationPanel(dialog, banData))
                .build());
    SwingComponents.addEscapeKeyListener(dialog, dialog::dispose);
    dialog.pack();
    dialog.setLocationRelativeTo(parentFrame);
    dialog.setVisible(true);
    dialog.dispose();
  }

  private Component banDurationPanel(final JDialog dialog, final BanData banData) {
    final JRadioButton oneHour = new JRadioButton("1 Hour");
    oneHour.setSelected(true);
    final JRadioButton oneDay = new JRadioButton("1 Day");
    final JRadioButton oneWeek = new JRadioButton("1 Week");
    final JRadioButton threeWeeks = new JRadioButton("3 Weeks");

    final ButtonGroup group = new ButtonGroup();
    group.add(oneHour);
    group.add(oneDay);
    group.add(oneWeek);
    group.add(threeWeeks);

    final JPanel radioPanel =
        new JPanelBuilder()
            .boxLayoutVertical()
            .add(oneHour)
            .add(oneDay)
            .add(oneWeek)
            .add(threeWeeks)
            .build();

    return new JPanelBuilder()
        .borderLayout()
        .addCenter(radioPanel)
        .addSouth(
            new JPanelBuilder()
                .flowLayout()
                .add(Box.createHorizontalStrut(10))
                .add(new JButtonBuilder().title("Cancel").actionListener(dialog::dispose).build())
                .add(Box.createHorizontalStrut(25))
                .add(
                    new JButtonBuilder()
                        .title("Submit")
                        .actionListener(
                            () -> {
                              dialog.dispose();
                              final String selectedDuration =
                                  getSelection(List.of(oneHour, oneDay, oneWeek, threeWeeks));
                              confirmBan(selectedDuration, banData);
                            })
                        .build())
                .build())
        .build();
  }

  private String getSelection(final List<JRadioButton> radioButtons) {
    return radioButtons.stream()
        .filter(AbstractButton::isSelected)
        .findFirst()
        .map(AbstractButton::getText)
        // .map(TIME_DURATION_MAP::get)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Expected to find a selected button, programming error."));
  }

  private void confirmBan(final String banDurationText, final BanData banData) {

    SwingComponents.promptUser(
        "Confirm Ban",
        "Are you sure you want to ban: " + banData.username + " for " + banDurationText,
        () -> {
          accessLogTabModel.banUser(
              UserBanParams.builder()
                  .username(banData.username)
                  .ip(banData.ip)
                  .systemId(banData.hashedMac)
                  .minutesToBan(TIME_DURATION_MAP.get(banDurationText))
                  .build());

          MessagePopup.showMessage(
              parentFrame, banData.username + " banned for " + banDurationText);
        });
  }
}
