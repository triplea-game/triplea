package games.strategy.engine.lobby.client.ui.action.player.info;

import java.awt.Component;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.Alias;
import org.triplea.java.DateTimeFormatterUtil;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

/**
 * Tab that has a table showing player aliases, these are alternative names chosen by a player with
 * a matching system-id or IP. We show the network identifiers, their name and their most recent
 * access date.
 */
@AllArgsConstructor
class PlayerAliasesTab {
  private final PlayerSummaryForModerator playerSummaryForModerator;

  String getTabTitle() {
    return "Aliases (" + playerSummaryForModerator.getAliases().size() + ")";
  }

  Component getTabContents() {
    return SwingComponents.newJScrollPane(
        new JTableBuilder<Alias>()
            .columnNames("Name", "Last Date Used", "IP", "System ID")
            .rowData(
                playerSummaryForModerator.getAliases().stream()
                    .sorted(Comparator.comparing(Alias::getEpochMilliDate))
                    .collect(Collectors.toList()))
            .rowMapper(
                alias ->
                    List.of(
                        alias.getName(),
                        DateTimeFormatterUtil.formatEpochMilli(alias.getEpochMilliDate()),
                        alias.getIp(),
                        alias.getSystemId()))
            .build());
  }
}
