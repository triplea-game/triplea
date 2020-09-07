package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class MapTest {
  @Test
  void mapTagParsing() {
    final Map map = parseMapXml("map.xml").getMap();
    assertThat(map, is(notNullValue()));
    assertThat(map.getTerritories(), is(notNullValue()));
    assertThat(map.getTerritories(), hasSize(2));

    assertThat(map.getTerritories().get(0), is(notNullValue()));
    assertThat(map.getTerritories().get(0).getName(), is("Belgium"));
    assertThat(map.getTerritories().get(0).isWater(), is(false));

    assertThat(map.getTerritories().get(1), is(notNullValue()));
    assertThat(map.getTerritories().get(1).getName(), is("Sea"));
    assertThat(map.getTerritories().get(1).isWater(), is(true));

    assertThat(map.getConnections(), is(notNullValue()));
    assertThat(map.getTerritories(), hasSize(2));

    assertThat(map.getConnections().get(0), is(notNullValue()));
    assertThat(map.getConnections().get(0).getT1(), is("start1"));
    assertThat(map.getConnections().get(0).getT2(), is("end1"));

    assertThat(map.getConnections().get(1), is(notNullValue()));
    assertThat(map.getConnections().get(1).getT1(), is("start2"));
    assertThat(map.getConnections().get(1).getT2(), is("end2"));
  }
}
