package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class MapTest {
  @Test
  void mapTagParsing() {
    final Map map = parseMapXml("map.xml").getMap();
    assertThat(map).isNotNull();
    assertThat(map.getTerritories()).isNotNull();
    assertThat(map.getTerritories()).hasSize(2);

    assertThat(map.getTerritories().get(0)).isNotNull();
    assertThat(map.getTerritories().get(0).getName()).isEqualTo("Belgium");
    assertThat(map.getTerritories().get(0).getWater()).isNull();

    assertThat(map.getTerritories().get(1)).isNotNull();
    assertThat(map.getTerritories().get(1).getName()).isEqualTo("Sea");
    assertThat(map.getTerritories().get(1).getWater()).isTrue();

    assertThat(map.getConnections()).isNotNull();
    assertThat(map.getTerritories()).hasSize(2);

    assertThat(map.getConnections().get(0)).isNotNull();
    assertThat(map.getConnections().get(0).getT1()).isEqualTo("start1");
    assertThat(map.getConnections().get(0).getT2()).isEqualTo("end1");

    assertThat(map.getConnections().get(1)).isNotNull();
    assertThat(map.getConnections().get(1).getT1()).isEqualTo("start2");
    assertThat(map.getConnections().get(1).getT2()).isEqualTo("end2");
  }
}
