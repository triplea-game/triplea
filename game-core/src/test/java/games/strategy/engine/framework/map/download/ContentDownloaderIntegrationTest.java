package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

@Integration
class ContentDownloaderIntegrationTest extends AbstractClientSettingTestCase {
  @Test
  void downloadFile() throws Exception {
    try (CloseableDownloader contentDownloader =
        new ContentDownloader(URI.create(UrlConstants.DOWNLOAD_WEBSITE))) {

      final List<String> content =
          IOUtils.readLines(contentDownloader.getStream(), StandardCharsets.UTF_8);

      assertThat(content, not(empty()));
    }
  }
}
