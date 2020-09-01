package org.triplea.map.reader;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import org.triplea.map.data.elements.GameTag;

@UtilityClass
public class XmlReaderTestUtils {
  public static InputStream openFile(final String fileName) {
    return InfoTagReadingTest.class.getClassLoader().getResourceAsStream(fileName);
  }

  static GameTag parseMapXml(final String xmlFileName) {
    try (InputStream stream = openFile(xmlFileName)) {
      final MapReadResult readResult = MapElementReader.readXml(xmlFileName, stream);

      assertThat(readResult.getErrorMessages(), is(empty()));
      assertThat(readResult.getGameTag(), isPresent());
      return readResult.getGameTag().orElseThrow();
    } catch (final IOException e) {
      throw new RuntimeException("Failed to parse (expecting a valid XML file): " + xmlFileName, e);
    }
  }
}
