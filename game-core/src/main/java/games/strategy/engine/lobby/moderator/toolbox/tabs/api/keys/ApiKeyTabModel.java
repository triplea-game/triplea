package games.strategy.engine.lobby.moderator.toolbox.tabs.api.keys;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;

import com.google.common.annotations.VisibleForTesting;

import lombok.AllArgsConstructor;


@AllArgsConstructor
class ApiKeyTabModel {
  @VisibleForTesting
  static final String DELETE_BUTTON_TEXT = "Delete";
  private final ToolboxApiKeyClient toolboxApiKeyClient;

  static List<String> fetchTableHeaders() {
    return Arrays.asList("Key ID", "Last Used", "Last Used IP", "");
  }


  List<List<String>> fetchTableData() {
    return toolboxApiKeyClient.getApiKeys()
        .stream()
        .map(keyData -> Arrays.asList(
            keyData.getPublicId(),
            Optional.ofNullable(keyData.getLastUsed())
                .map(Instant::toString)
                .orElse(""),
            Optional.ofNullable(keyData.getLastUsedIp())
                .orElse(""),
            DELETE_BUTTON_TEXT))
        .collect(Collectors.toList());
  }

  String createSingleUseKey() {
    return toolboxApiKeyClient.generateSingleUseKey().getApiKey();
  }

  void deleteKey(final String keyId) {
    toolboxApiKeyClient.deleteApiKey(keyId);
  }
}
