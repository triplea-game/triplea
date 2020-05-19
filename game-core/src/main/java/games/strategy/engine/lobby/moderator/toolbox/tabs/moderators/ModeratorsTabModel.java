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
  @VisibleForTesting static final String ADD_SUPER_MOD_BUTTON = "Add Super-Mod";

  private static final Function<Long, String> dateTimeFormatter =
      epochMillis ->
          DateTimeFormatterUtil.formatEpochMilli(
              epochMillis, DateTimeFormatterUtil.FormatOption.WITHOUT_TIMEZONE);

  private final ToolboxModeratorManagementClient toolboxModeratorManagementClient;

  @Getter private final boolean isSuperMod;

  ModeratorsTabModel(final ToolboxModeratorManagementClient toolboxModeratorManagementClient) {
    this.toolboxModeratorManagementClient = toolboxModeratorManagementClient;
    isSuperMod = toolboxModeratorManagementClient.isCurrentUserSuperMod();
  }

  List<String> fetchTableHeaders() {
    return isSuperMod() ? SUPER_MOD_HEADERS : HEADERS;
  }

  List<List<String>> fetchTableData() {
    return toolboxModeratorManagementClient.fetchModeratorList().stream()
        .map(
            modInfo ->
                isSuperMod
                    ? List.of(
                        modInfo.getName(),
                        Optional.ofNullable(modInfo.getLastLoginEpochMillis())
                            .map(dateTimeFormatter)
                            .orElse(""),
                        REMOVE_MOD_BUTTON_TEXT,
                        ADD_SUPER_MOD_BUTTON)
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

  void addSuperMod(final String user) {
    toolboxModeratorManagementClient.addSuperMod(user);
  }

  boolean checkUserExists(final String user) {
    return toolboxModeratorManagementClient.checkUserExists(user);
  }

  void addModerator(final String user) {
    toolboxModeratorManagementClient.addModerator(user);
  }
}
