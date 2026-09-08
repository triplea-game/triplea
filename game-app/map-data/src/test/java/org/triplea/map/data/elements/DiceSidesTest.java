package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class DiceSidesTest {
  @Test
  void readInfoTag() {
    final DiceSides diceSides = parseMapXml("dice-sides.xml").getDiceSides();

    assertThat(diceSides.getValue()).isEqualTo(20);
  }
}
