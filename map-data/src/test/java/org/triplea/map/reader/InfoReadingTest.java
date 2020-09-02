package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Game;

class InfoReadingTest {
  @Test
  void readInfoTag() {
    final Game parsedMap = parseMapXml("info-tag.xml");

    assertThat(parsedMap.getInfo().getName(), is("info-tag-test"));
    assertThat(parsedMap.getInfo().getVersion(), is("123.xyz"));
  }
}
