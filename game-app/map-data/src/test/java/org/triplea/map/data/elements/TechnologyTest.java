package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Technology.PlayerTech;

class TechnologyTest {
  @Test
  void technologyParsing() {
    final Technology technology = parseMapXml("technology.xml").getTechnology();
    assertThat(technology).isNotNull();
    assertThat(technology.getTechnologies().getTechNames()).hasSize(3);
    assertThat(technology.getTechnologies().getTechNames().get(0).getName()).isEqualTo("armour");
    assertThat(technology.getTechnologies().getTechNames().get(1).getName())
        .isEqualTo("tracer_rounds");
    assertThat(technology.getTechnologies().getTechNames().get(2).getName())
        .isEqualTo("creeping_barrage");
    assertThat(technology.getTechnologies().getTechNames().get(2).getTech())
        .isEqualTo("improvedArtillerySupport");

    assertThat(technology.getPlayerTechs()).hasSize(2);

    final PlayerTech usTech = technology.getPlayerTechs().get(0);

    assertThat(usTech.getPlayer()).isEqualTo("USA");
    assertThat(usTech.getCategories()).hasSize(3);

    assertThat(usTech.getCategories().get(0).getName()).isEqualTo("Land");
    assertThat(usTech.getCategories().get(0).getTechs()).hasSize(3);
    assertThat(usTech.getCategories().get(0).getTechs().get(0).getName())
        .isEqualTo("counter_battery_fire");
    assertThat(usTech.getCategories().get(0).getTechs().get(1).getName())
        .isEqualTo("mobile_warfare");
    assertThat(usTech.getCategories().get(0).getTechs().get(2).getName())
        .isEqualTo("factory_electrification");

    assertThat(usTech.getCategories().get(1).getName()).isEqualTo("Sea");
    assertThat(usTech.getCategories().get(1).getTechs()).hasSize(1);
    assertThat(usTech.getCategories().get(1).getTechs().get(0).getName())
        .isEqualTo("antiSubmarine_warfare");

    assertThat(usTech.getCategories().get(2).getName()).isEqualTo("Air");
    assertThat(usTech.getCategories().get(2).getTechs()).hasSize(1);
    assertThat(usTech.getCategories().get(2).getTechs().get(0).getName()).isEqualTo("radio");

    final PlayerTech ukTech = technology.getPlayerTechs().get(1);
    assertThat(ukTech.getPlayer()).isEqualTo("UK");
    assertThat(ukTech.getCategories()).hasSize(3);

    assertThat(ukTech.getCategories().get(0).getName()).isEqualTo("Land");
    assertThat(ukTech.getCategories().get(0).getTechs()).hasSize(2);
    assertThat(ukTech.getCategories().get(0).getTechs().get(0).getName())
        .isEqualTo("mobile_warfare");
    assertThat(ukTech.getCategories().get(0).getTechs().get(1).getName())
        .isEqualTo("factory_electrification");

    assertThat(ukTech.getCategories().get(1).getName()).isEqualTo("Sea");
    assertThat(ukTech.getCategories().get(1).getTechs()).hasSize(2);
    assertThat(ukTech.getCategories().get(1).getTechs().get(0).getName())
        .isEqualTo("antiSubmarine_warfare");
    assertThat(ukTech.getCategories().get(1).getTechs().get(1).getName())
        .isEqualTo("aircraftCarrier");

    assertThat(ukTech.getCategories().get(2).getName()).isEqualTo("Air");
    assertThat(ukTech.getCategories().get(2).getTechs()).hasSize(3);
    assertThat(ukTech.getCategories().get(2).getTechs().get(0).getName())
        .isEqualTo("airTraffic_control");
    assertThat(ukTech.getCategories().get(2).getTechs().get(1).getName())
        .isEqualTo("strategic_bombing");
    assertThat(ukTech.getCategories().get(2).getTechs().get(2).getName()).isEqualTo("radio");
  }
}
