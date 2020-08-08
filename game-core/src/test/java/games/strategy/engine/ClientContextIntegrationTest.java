package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

@Integration
class ClientContextIntegrationTest extends AbstractClientSettingTestCase {

  @Test
  void canRetrieveCurrentEngineVersion() {
    assertThat(ClientContext.engineVersion(), notNullValue());
  }

  @Test
  void downloadListOfAvailableMaps() {
    final List<DownloadFileDescription> list = ClientContext.getMapDownloadList();

    assertThat(list, notNullValue());
    assertThat(list, not(empty()));
  }
}
