package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class TripleaTest {
  @Test
  void readInfoTag() {
    final Game parsedMap = parseMapXml("triplea.xml");

    assertThat(parsedMap.getTriplea().getMinimumVersion(), is("min-version"));
  }
}
