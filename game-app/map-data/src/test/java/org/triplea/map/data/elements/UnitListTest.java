package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class UnitListTest {

  @Test
  void territoryEffectParsing() {
    final UnitList unitList = parseMapXml("unit-list.xml").getUnitList();
    assertThat(unitList.getUnits()).hasSize(3);
    assertThat(unitList.getUnits().get(0).getName()).isEqualTo("Infantry");
    assertThat(unitList.getUnits().get(1).getName()).isEqualTo("Militia");
    assertThat(unitList.getUnits().get(2).getName()).isEqualTo("Helicopter");
  }
}
