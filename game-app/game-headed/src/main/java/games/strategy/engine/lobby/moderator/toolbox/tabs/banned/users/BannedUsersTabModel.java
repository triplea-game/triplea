package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.java.DateTimeFormatterUtil;

@AllArgsConstructor
class BannedUsersTabModel {
  @VisibleForTesting static final String REMOVE_BUTTON_TEXT = "Remove";

  private final ToolboxUserBanClient toolboxUserBanClient;

  static List<String> fetchTableHeaders() {
    return List.of("Ban ID", "Username", "Date Banned", "IP", "Hashed Mac", "Ban Expiry", "");
  }

  List<List<String>> fetchTableData() {
    return toolboxUserBanClient.getUserBans().stream()
        .map(
            userBan ->
                List.of(
                    userBan.getBanId(),
                    userBan.getUsername(),
                    DateTimeFormatterUtil.formatEpochMilli(
                        userBan.getBanDate(), DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE),
                    userBan.getIp(),
                    userBan.getHashedMac(),
                    DateTimeFormatterUtil.formatEpochMilli(
                        userBan.getBanExpiry(),
                        DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE),
                    REMOVE_BUTTON_TEXT))
        .collect(Collectors.toList());
  }

  void removeBan(final String banId) {
    toolboxUserBanClient.removeUserBan(banId);
  }
}
