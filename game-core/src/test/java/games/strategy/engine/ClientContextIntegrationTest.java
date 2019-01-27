package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;

@Integration
class ClientContextIntegrationTest extends AbstractClientSettingTestCase {

  @Test
  void verifyClientContext() {
    assertThat(ClientContext.downloadCoordinator(), notNullValue());
    assertThat(ClientContext.engineVersion(), notNullValue());

    final List<DownloadFileDescription> list = ClientContext.getMapDownloadList();

    assertThat(list, notNullValue());
    assertThat(list, not(empty()));
  }
}
