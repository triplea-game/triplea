package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.LookupModeratorEventsArgs;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.settings.ClientSetting;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
class EventLogTabModel {
  @VisibleForTesting
  static final int PAGE_SIZE = 10;

  private final ModeratorToolboxClient moderatorToolboxClient;
  private int lastRow = 0;

  static List<String> getEventLogTableHeaders() {
    return Arrays.asList("Date", "Moderator", "Action", "Target");
  }

  List<List<String>> getEventLogTableData() {
    final List<ModeratorEvent> moderatorEvents = moderatorToolboxClient.lookupModeratorEvents(
        LookupModeratorEventsArgs.builder()
            .apiKey(ClientSetting.moderatorApiKey.getValueOrThrow())
            .rowCount(PAGE_SIZE)
            .rowStart(0)
            .build());
    lastRow = PAGE_SIZE;
    return toTableData(moderatorEvents);
  }

  private static List<List<String>> toTableData(final List<ModeratorEvent> events) {
    return events.stream()
        .map(event -> Arrays.asList(
            event.getDate().toString(),
            event.getModeratorName(),
            event.getModeratorAction(),
            event.getActionTarget()))
        .collect(Collectors.toList());
  }

  List<List<String>> loadMore() {
    final List<ModeratorEvent> moderatorEvents = moderatorToolboxClient.lookupModeratorEvents(
        LookupModeratorEventsArgs.builder()
            .apiKey(ClientSetting.moderatorApiKey.getValueOrThrow())
            .rowCount(PAGE_SIZE)
            .rowStart(lastRow)
            .build());
    lastRow += PAGE_SIZE;
    return toTableData(moderatorEvents);
  }
}
