package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;

import com.google.common.annotations.VisibleForTesting;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class BannedUsersTabModel {
  @VisibleForTesting
  static final String REMOVE_BUTTON_TEXT = "Remove";

  private final ToolboxUserBanClient toolboxUserBanClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList(
        "Ban ID",
        "Username",
        "Date Banned",
        "IP",
        "Hashed Mac",
        "Ban Expiry",
        "");
  }

  List<List<String>> fetchTableData() {
    return toolboxUserBanClient.getUserBans()
        .stream()
        .map(userBan -> Arrays.asList(
            userBan.getBanId(),
            userBan.getUsername(),
            String.valueOf(userBan.getBanDate()),
            userBan.getIp(),
            userBan.getHashedMac(),
            userBan.getBanExpiry().toString(),
            REMOVE_BUTTON_TEXT))
        .collect(Collectors.toList());
  }

  void removeBan(final String banId) {
    toolboxUserBanClient.removeUserBan(banId);
  }
}
