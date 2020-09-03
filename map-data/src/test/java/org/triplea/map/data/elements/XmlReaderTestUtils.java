package org.triplea.map.data.elements;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import org.triplea.map.reader.MapElementReader;
import org.triplea.map.reader.MapReadResult;

@UtilityClass
class XmlReaderTestUtils {

  static Game parseMapXml(final String xmlFileName) {
    try (InputStream stream = openFile(xmlFileName)) {
      final MapReadResult readResult = MapElementReader.readXml(xmlFileName, stream);

      assertThat(readResult.getErrorMessages(), is(empty()));
      assertThat(readResult.getGame(), isPresent());
      return readResult.getGame().orElseThrow();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to parse (expecting a valid XML file): " + xmlFileName, e);
    }
  }

  private static InputStream openFile(final String fileName) {
    return InfoTest.class.getClassLoader().getResourceAsStream(fileName);
  }
}
