package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.GameTag;

public class TripleaTagTest {
  @Test
  void readInfoTag() {
    final GameTag parsedMap = parseMapXml("resources/triplea-tag.xml");

    assertThat(parsedMap.getTripleaTag().getMinimumVersion(), is("min-version"));
  }
}
