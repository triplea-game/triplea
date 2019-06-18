package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;

import com.google.common.annotations.VisibleForTesting;

import lombok.Builder;

/**
 * Model object interacts with backend, does not hold state and is not aware of UI components.
 */
@Builder
class BannedUsernamesTabModel {

  @VisibleForTesting
  static final String REMOVE_BUTTON_TEXT = "Remove";

  @Nonnull
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList("Username", "Date Banned", "");
  }

  List<List<String>> fetchTableData() {
    return toolboxUsernameBanClient.getUsernameBans()
        .stream()
        .map(banData -> Arrays.asList(
            banData.getBannedName(),
            banData.getBanDate().toString(),
            "Remove"))
        .collect(Collectors.toList());
  }

  void removeUsernameBan(final String usernameBanToRemove) {
    toolboxUsernameBanClient.removeUsernameBan(usernameBanToRemove);
  }

  void addUsernameBan(final String usernameToBan) {
    toolboxUsernameBanClient.addUsernameBan(usernameToBan);
  }
}
