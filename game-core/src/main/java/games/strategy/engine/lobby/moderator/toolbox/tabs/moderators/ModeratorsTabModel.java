package games.strategy.engine.lobby.moderator.toolbox.tabs.moderators;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.java.DateTimeFormatterUtil;

class ModeratorsTabModel {
  @VisibleForTesting static final List<String> HEADERS = List.of("Name", "Last Login");

  @VisibleForTesting
  static final List<String> SUPER_MOD_HEADERS = List.of("Name", "Last Login", "", "");

  @VisibleForTesting static final String REMOVE_MOD_BUTTON_TEXT = "Remove Mod";
  @VisibleForTesting static final String MAKE_ADMIN_BUTTON_TEXT = "Make Admin";

  private static final Function<Long, String> dateTimeFormatter =
      epochMillis ->
          DateTimeFormatterUtil.formatEpochMilli(
              epochMillis, DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE);

  private final ToolboxModeratorManagementClient toolboxModeratorManagementClient;

  @Getter private final boolean isAdmin;

  ModeratorsTabModel(final ToolboxModeratorManagementClient toolboxModeratorManagementClient) {
    this.toolboxModeratorManagementClient = toolboxModeratorManagementClient;
    isAdmin = toolboxModeratorManagementClient.isCurrentUserAdmin();
  }

  List<String> fetchTableHeaders() {
    return isAdmin() ? SUPER_MOD_HEADERS : HEADERS;
  }

  List<List<String>> fetchTableData() {
    return toolboxModeratorManagementClient.fetchModeratorList().stream()
        .map(
            modInfo ->
                isAdmin
                    ? List.of(
                        modInfo.getName(),
                        Optional.ofNullable(modInfo.getLastLoginEpochMillis())
                            .map(dateTimeFormatter)
                            .orElse(""),
                        REMOVE_MOD_BUTTON_TEXT,
                        MAKE_ADMIN_BUTTON_TEXT)
                    : List.of(
                        modInfo.getName(),
                        Optional.ofNullable(modInfo.getLastLoginEpochMillis())
                            .map(dateTimeFormatter)
                            .orElse("")))
        .collect(Collectors.toList());
  }

  void removeMod(final String user) {
    toolboxModeratorManagementClient.removeMod(user);
  }

  void addAdmin(final String user) {
    toolboxModeratorManagementClient.addAdmin(user);
  }

  boolean checkUserExists(final String user) {
    return toolboxModeratorManagementClient.checkUserExists(user);
  }

  void addModerator(final String user) {
    toolboxModeratorManagementClient.addModerator(user);
  }
}
