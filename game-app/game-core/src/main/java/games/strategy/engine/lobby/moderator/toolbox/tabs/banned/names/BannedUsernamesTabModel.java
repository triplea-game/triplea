package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.java.DateTimeFormatterUtil;

/** Model object interacts with backend, does not hold state and is not aware of UI components. */
@Builder
class BannedUsernamesTabModel {

  @VisibleForTesting static final String REMOVE_BUTTON_TEXT = "Remove";

  @Nonnull private final ToolboxUsernameBanClient toolboxUsernameBanClient;

  static List<String> fetchTableHeaders() {
    return List.of("Username", "Date Banned", "");
  }

  List<List<String>> fetchTableData() {
    return toolboxUsernameBanClient.getUsernameBans().stream()
        .map(
            banData ->
                List.of(
                    banData.getBannedName(),
                    DateTimeFormatterUtil.formatEpochMilli(
                        banData.getBanDate(), DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE),
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
