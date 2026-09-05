package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class TerritoryEffectListTest {

  @Test
  void territoryEffectParsing() {
    final TerritoryEffectList territoryEffectList =
        parseMapXml("territory-effect-list.xml").getTerritoryEffectList();
    assertThat(territoryEffectList.getTerritoryEffects()).hasSize(3);
    assertThat(territoryEffectList.getTerritoryEffects().get(0).getName()).isEqualTo("city");
    assertThat(territoryEffectList.getTerritoryEffects().get(1).getName()).isEqualTo("mountain");
    assertThat(territoryEffectList.getTerritoryEffects().get(2).getName()).isEqualTo("sea");
  }
}
