package games.strategy.engine.lobby.moderator.toolbox.tabs.api.keys;

import static games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil.verifyTableDimensions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.api.key.ApiKeyData;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;


@ExtendWith(MockitoExtension.class)
class ApiKeyTabModelTest {
  private static final String KEY_VALUE = "When the mate laughs trade scurvy";
  private static final ApiKeyData API_KEY_DATA =
      ApiKeyData.builder()
          .publicId("Well, coal-black passion!")
          .lastUsedIp("Gar, belay.")
          .lastUsed(Instant.now())
          .build();

  private static final ApiKeyData API_KEY_DATA_WITH_NULLS =
      ApiKeyData.builder()
          .publicId("Jolly roger, yer not enduring me without a courage!")
          .build();

  @Mock
  private ToolboxApiKeyClient toolboxApiKeyClient;

  @InjectMocks
  private ApiKeyTabModel apiKeyTabModel;

  @Test
  void fetchData() {
    when(toolboxApiKeyClient.getApiKeys())
        .thenReturn(Arrays.asList(API_KEY_DATA, API_KEY_DATA_WITH_NULLS));

    final List<List<String>> tableData = apiKeyTabModel.fetchTableData();

    assertThat(tableData, hasSize(2));

    verifyTableDimensions(tableData, ApiKeyTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(tableData, 0,
        API_KEY_DATA.getPublicId(),
        API_KEY_DATA.getLastUsed().toString(),
        API_KEY_DATA.getLastUsedIp(),
        ApiKeyTabModel.DELETE_BUTTON_TEXT);

    ToolboxTabModelTestUtil.verifyTableDataAtRow(tableData, 1,
        API_KEY_DATA_WITH_NULLS.getPublicId(),
        "",
        "",
        ApiKeyTabModel.DELETE_BUTTON_TEXT);
  }

  @Test
  void createSingleUseKey() {
    when(toolboxApiKeyClient.generateSingleUseKey())
        .thenReturn(new NewApiKey(KEY_VALUE));

    assertThat(apiKeyTabModel.createSingleUseKey(), is(KEY_VALUE));
  }

  @Test
  void deleteKey() {
    apiKeyTabModel.deleteKey(KEY_VALUE);
    verify(toolboxApiKeyClient).deleteApiKey(KEY_VALUE);
  }
}
