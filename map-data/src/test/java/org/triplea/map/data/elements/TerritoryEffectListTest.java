package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class TerritoryEffectListTest {

  @Test
  void territoryEffectParsing() {
    final TerritoryEffectList territoryEffectList =
        parseMapXml("territory-effect-list.xml").getTerritoryEffectList();
    assertThat(territoryEffectList.getTerritoryEffects(), hasSize(3));
    assertThat(territoryEffectList.getTerritoryEffects().get(0).getName(), is("city"));
    assertThat(territoryEffectList.getTerritoryEffects().get(1).getName(), is("mountain"));
    assertThat(territoryEffectList.getTerritoryEffects().get(2).getName(), is("sea"));
  }
}
