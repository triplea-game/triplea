package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Game;

public class TripleaTest {
  @Test
  void readInfoTag() {
    final Game parsedMap = parseMapXml("triplea-tag.xml");

    assertThat(parsedMap.getTriplea().getMinimumVersion(), is("min-version"));
  }
}
