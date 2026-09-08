package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class InfoTest {
  @Test
  void readInfoTag() {
    final Info info = parseMapXml("info.xml").getInfo();

    assertThat(info.getName()).isEqualTo("info-tag-test");
  }
}
