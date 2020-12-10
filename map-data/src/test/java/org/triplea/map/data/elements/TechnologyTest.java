package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Technology.PlayerTech;

class TechnologyTest {
  @Test
  void technologyParsing() {
    final Technology technology = parseMapXml("technology.xml").getTechnology();
    assertThat(technology, is(notNullValue()));
    assertThat(technology.getTechnologies().getTechNames(), hasSize(3));
    assertThat(technology.getTechnologies().getTechNames().get(0).getName(), is("armour"));
    assertThat(technology.getTechnologies().getTechNames().get(1).getName(), is("tracer_rounds"));
    assertThat(
        technology.getTechnologies().getTechNames().get(2).getName(), is("creeping_barrage"));
    assertThat(
        technology.getTechnologies().getTechNames().get(2).getTech(),
        is("improvedArtillerySupport"));

    assertThat(technology.getPlayerTechs(), hasSize(2));

    final PlayerTech usTech = technology.getPlayerTechs().get(0);

    assertThat(usTech.getPlayer(), is("USA"));
    assertThat(usTech.getCategories(), hasSize(3));

    assertThat(usTech.getCategories().get(0).getName(), is("Land"));
    assertThat(usTech.getCategories().get(0).getTechs(), hasSize(3));
    assertThat(
        usTech.getCategories().get(0).getTechs().get(0).getName(), is("counter_battery_fire"));
    assertThat(usTech.getCategories().get(0).getTechs().get(1).getName(), is("mobile_warfare"));
    assertThat(
        usTech.getCategories().get(0).getTechs().get(2).getName(), is("factory_electrification"));

    assertThat(usTech.getCategories().get(1).getName(), is("Sea"));
    assertThat(usTech.getCategories().get(1).getTechs(), hasSize(1));
    assertThat(
        usTech.getCategories().get(1).getTechs().get(0).getName(), is("antiSubmarine_warfare"));

    assertThat(usTech.getCategories().get(2).getName(), is("Air"));
    assertThat(usTech.getCategories().get(2).getTechs(), hasSize(1));
    assertThat(usTech.getCategories().get(2).getTechs().get(0).getName(), is("radio"));

    final PlayerTech ukTech = technology.getPlayerTechs().get(1);
    assertThat(ukTech.getPlayer(), is("UK"));
    assertThat(ukTech.getCategories(), hasSize(3));

    assertThat(ukTech.getCategories().get(0).getName(), is("Land"));
    assertThat(ukTech.getCategories().get(0).getTechs(), hasSize(2));
    assertThat(ukTech.getCategories().get(0).getTechs().get(0).getName(), is("mobile_warfare"));
    assertThat(
        ukTech.getCategories().get(0).getTechs().get(1).getName(), is("factory_electrification"));

    assertThat(ukTech.getCategories().get(1).getName(), is("Sea"));
    assertThat(ukTech.getCategories().get(1).getTechs(), hasSize(2));
    assertThat(
        ukTech.getCategories().get(1).getTechs().get(0).getName(), is("antiSubmarine_warfare"));
    assertThat(ukTech.getCategories().get(1).getTechs().get(1).getName(), is("aircraftCarrier"));

    assertThat(ukTech.getCategories().get(2).getName(), is("Air"));
    assertThat(ukTech.getCategories().get(2).getTechs(), hasSize(3));
    assertThat(ukTech.getCategories().get(2).getTechs().get(0).getName(), is("airTraffic_control"));
    assertThat(ukTech.getCategories().get(2).getTechs().get(1).getName(), is("strategic_bombing"));
    assertThat(ukTech.getCategories().get(2).getTechs().get(2).getName(), is("radio"));
  }
}
