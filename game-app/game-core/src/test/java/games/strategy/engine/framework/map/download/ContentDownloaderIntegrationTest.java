package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.triplea.io.CloseableDownloader;
import org.triplea.io.ContentDownloader;

class ContentDownloaderIntegrationTest extends AbstractClientSettingTestCase {
  @Test
  void downloadFile() throws Exception {
    try (CloseableDownloader contentDownloader =
        new ContentDownloader(URI.create(UrlConstants.DOWNLOAD_WEBSITE))) {

      final List<String> content =
          IOUtils.readLines(contentDownloader.getStream(), StandardCharsets.UTF_8);

      assertThat(content).isNotEmpty();
    }
  }
}
