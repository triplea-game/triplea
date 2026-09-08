package games.strategy.engine.framework.map.file.system.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ZippedMapsExtractorTest {

  @Test
  void testExtractionFolderNaming() {
    assertThat(ZippedMapsExtractor.computeExtractionFolderName("zip")).isEqualTo("zip");
    assertThat(ZippedMapsExtractor.computeExtractionFolderName("zip-master")).isEqualTo("zip");
    assertThat(ZippedMapsExtractor.computeExtractionFolderName("zip-master.zip")).isEqualTo("zip");
    assertThat(ZippedMapsExtractor.computeExtractionFolderName("zip.zip")).isEqualTo("zip");
  }
}
