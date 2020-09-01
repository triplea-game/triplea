package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.GameTag;

class DiceSidesReadingTest {
  @Test
  void readInfoTag() {
    final GameTag gameTag = parseMapXml("dice-sides.xml");

    assertThat(gameTag.getDiceSidesTag().getValue(), is("20"));
  }
}
