package games.strategy.engine.lobby.client.ui.action.player.info;

import java.awt.Component;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.BanInformation;
import org.triplea.java.DateTimeFormatterUtil;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

@AllArgsConstructor
class PlayerBansTab {
  private final PlayerSummaryForModerator playerSummaryForModerator;

  String getTabTitle() {
    return "Bans (" + playerSummaryForModerator.getBans().size() + ")";
  }

  Component getTabContents() {
    return SwingComponents.newJScrollPane(
        new JTableBuilder()
            .columnNames("Name", "Date Banned", "Ban Expiry", "IP", "System ID")
            .tableData(toTableData(playerSummaryForModerator.getBans()))
            .build());
  }

  private List<List<String>> toTableData(final Collection<BanInformation> bans) {
    return bans.stream()
        .map(
            ban ->
                List.of(
                    ban.getName(),
                    DateTimeFormatterUtil.formatEpochMilli(ban.getEpochMilliStartDate()),
                    DateTimeFormatterUtil.formatEpochMilli(ban.getEpochMillEndDate()),
                    ban.getIp(),
                    ban.getSystemId()))
        .collect(Collectors.toList());
  }
}
