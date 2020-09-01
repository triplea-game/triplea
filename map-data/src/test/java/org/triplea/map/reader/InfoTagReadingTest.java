package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.ParsedMap;

class InfoTagReadingTest {
  @Test
  void readInfoTag() {
    final ParsedMap parsedMap = parseMapXml("resources/info-tag.xml");

    assertThat(parsedMap.getInfoTag().getName(), is("info-tag-test"));
    assertThat(parsedMap.getInfoTag().getVersion(), is("123.xyz"));
  }
}
