package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogData;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogSearchRequest;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.java.DateTimeFormatterUtil;

@RequiredArgsConstructor
class AccessLogTabModel {
  private final ToolboxAccessLogClient toolboxAccessLogClient;
  private final ToolboxUserBanClient toolboxUserBanClient;
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;

  static List<String> fetchTableHeaders() {
    return List.of("Access Date", "Username", "IP", "System Id", "Registered", "", "");
  }

  List<List<String>> fetchTableData(final PagingParams pagingParams) {
    return toolboxAccessLogClient.getAccessLog(pagingParams).stream()
        .map(AccessLogTabModel::mapAccessLogDataToTable)
        .collect(Collectors.toList());
  }

  private static List<String> mapAccessLogDataToTable(final AccessLogData accessLogData) {
    return List.of(
        DateTimeFormatterUtil.formatEpochMilli(
            accessLogData.getAccessDate(), DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE),
        accessLogData.getUsername(),
        accessLogData.getIp(),
        accessLogData.getSystemId(),
        accessLogData.isRegistered() ? "Y" : "",
        "Ban Name",
        "Ban User");
  }

  List<List<String>> fetchSearchData(
      final AccessLogSearchRequest accessLogSearchParams, final PagingParams pagingParams) {
    return toolboxAccessLogClient.getAccessLog(accessLogSearchParams, pagingParams).stream()
        .map(AccessLogTabModel::mapAccessLogDataToTable)
        .collect(Collectors.toList());
  }

  void banUserName(final String username) {
    toolboxUsernameBanClient.addUsernameBan(username);
  }

  void banUser(final UserBanParams banUserParams) {
    toolboxUserBanClient.banUser(banUserParams);
  }
}
