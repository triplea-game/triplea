package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class UnitListTest {

  @Test
  void territoryEffectParsing() {
    final UnitList unitList = parseMapXml("unit-list.xml").getUnitList();
    assertThat(unitList.getUnits(), hasSize(3));
    assertThat(unitList.getUnits().get(0).getName(), is("Infantry"));
    assertThat(unitList.getUnits().get(1).getName(), is("Militia"));
    assertThat(unitList.getUnits().get(2).getName(), is("Helicopter"));
  }
}
