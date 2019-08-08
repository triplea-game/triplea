package games.strategy.engine.lobby.moderator.toolbox.tabs.moderators;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;

class ModeratorsTabModel {
  @VisibleForTesting static final List<String> HEADERS = Arrays.asList("Name", "Last Login");

  @VisibleForTesting
  static final List<String> SUPER_MOD_HEADERS =
      Arrays.asList("Name", "Last Login", "Generate API-Key", "Remove Mod", "Add Super-Mod");

  @VisibleForTesting static final String GENERATE_API_KEY_BUTTON_TEXT = "Generate API-Key";
  @VisibleForTesting static final String REMOVE_MOD_BUTTON_TEXT = "Remove Mod";
  @VisibleForTesting static final String ADD_SUPER_MOD_BUTTON = "Add Super-Mod";

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
                    ? Arrays.asList(
                        modInfo.getName(),
                        Optional.ofNullable(modInfo.getLastLogin())
                            .map(Instant::toString)
                            .orElse(""),
                        GENERATE_API_KEY_BUTTON_TEXT,
                        REMOVE_MOD_BUTTON_TEXT,
                        ADD_SUPER_MOD_BUTTON)
                    : Arrays.asList(
                        modInfo.getName(),
                        Optional.ofNullable(modInfo.getLastLogin())
                            .map(Instant::toString)
                            .orElse("")))
        .collect(Collectors.toList());
  }

  String generateApiKey(final String user) {
    return toolboxModeratorManagementClient.generateSingleUseKey(user);
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
