package games.strategy.engine.framework.map.file.system.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class ZippedMapsExtractorTest {

  @Test
  void testExtractionFolderNaming() {
    assertThat(ZippedMapsExtractor.createExtractionFolderName("zip"), is("zip"));
    assertThat(ZippedMapsExtractor.createExtractionFolderName("zip-master"), is("zip"));
    assertThat(ZippedMapsExtractor.createExtractionFolderName("zip-master.zip"), is("zip"));
    assertThat(ZippedMapsExtractor.createExtractionFolderName("zip.zip"), is("zip"));
  }
}
