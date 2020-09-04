package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class DiceSidesTest {
  @Test
  void readInfoTag() {
    final DiceSides diceSides = parseMapXml("dice-sides.xml").getDiceSides();

    assertThat(diceSides.getValue(), is("20"));
  }
}
