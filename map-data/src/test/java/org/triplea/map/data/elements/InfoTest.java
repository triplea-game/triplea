package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class InfoTest {
  @Test
  void readInfoTag() {
    final Info info = parseMapXml("info.xml").getInfo();

    assertThat(info.getName(), is("info-tag-test"));
    assertThat(info.getVersion(), is("123.xyz"));
  }
}
