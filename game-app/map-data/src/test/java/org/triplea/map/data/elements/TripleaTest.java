package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class TripleaTest {
  @Test
  void readInfoTag() {
    final Triplea triplea = parseMapXml("triplea.xml").getTriplea();

    assertThat(triplea.getMinimumVersion(), is("min-version"));
  }
}
