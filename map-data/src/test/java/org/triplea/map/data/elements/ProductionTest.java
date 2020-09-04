package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class ProductionTest {

  @Test
  void productionParsingTest() {
    final Production production = parseMapXml("player-list.xml").getProduction();
    assertThat(production, is(notNullValue()));
    assertThat(production.getProductionRules(), hasSize(2));

    assertThat(production.getProductionRules().get(0), is(notNullValue()));
    assertThat(production.getProductionRules().get(0).getName(), is("buyInfantry"));
    assertThat(production.getProductionRules().get(0).getCost(), is(notNullValue()));
    assertThat(production.getProductionRules().get(0).getCost().getResource(), is("PUs"));
    assertThat(production.getProductionRules().get(0).getCost().getQuantity(), is("2"));
    assertThat(
        production.getProductionRules().get(0).getResult().getResourceOrUnit(), is("Infantry"));
    assertThat(production.getProductionRules().get(0).getResult().getQuantity(), is("1"));

    assertThat(production.getProductionRules().get(1).getName(), is("buyTank"));
    assertThat(production.getProductionRules().get(1).getCost(), is(notNullValue()));
    assertThat(production.getProductionRules().get(1).getCost().getResource(), is("PUs"));
    assertThat(production.getProductionRules().get(1).getCost().getQuantity(), is("5"));
    assertThat(production.getProductionRules().get(1).getResult().getResourceOrUnit(), is("Tank"));
    assertThat(production.getProductionRules().get(1).getResult().getQuantity(), is("1"));

    assertThat(production.getRepairRule(), hasSize(1));
    assertThat(
        production.getRepairRule().get(0).getName(), is("repairFactoryIndustrialTechnology"));
    assertThat(production.getRepairRule().get(0).getCost().getResource(), is("PUs"));
    assertThat(production.getRepairRule().get(0).getCost().getQuantity(), is("1"));
    assertThat(production.getRepairRule().get(0).getResult().getResourceOrUnit(), is("factory"));
    assertThat(production.getRepairRule().get(0).getResult().getQuantity(), is("2"));

    assertThat(production.getRepairFrontier(), hasSize(1));
    assertThat(production.getRepairFrontier().get(0).getName(), is("repair"));
    assertThat(production.getRepairFrontier().get(0).getRepairRules(), hasSize(1));
    assertThat(
        production.getRepairFrontier().get(0).getRepairRules().get(0).getName(),
        is("repairFactory"));

    assertThat(production.getProductionFrontier(), hasSize(1));
    assertThat(production.getProductionFrontier().get(0).getName(), is("NeutralFrontier"));
    assertThat(production.getProductionFrontier().get(0).getFrontierRules(), hasSize(2));
    assertThat(
        production.getProductionFrontier().get(0).getFrontierRules().get(0).getName(),
        is("buyInfantry"));
    assertThat(
        production.getProductionFrontier().get(0).getFrontierRules().get(1).getName(),
        is("buyArtillery"));

    assertThat(production.getPlayerProduction(), hasSize(2));
    assertThat(production.getPlayerProduction().get(0).getPlayer(), is("Russians"));
    assertThat(production.getPlayerProduction().get(0).getFrontier(), is("RussiansFrontier"));
    assertThat(production.getPlayerProduction().get(1).getPlayer(), is("Italians"));
    assertThat(production.getPlayerProduction().get(1).getFrontier(), is("ItaliansFrontier"));

    assertThat(production.getPlayerRepair(), hasSize(2));
    assertThat(production.getPlayerRepair().get(0), is("France"));
    assertThat(production.getPlayerRepair().get(0), is("repair"));
    assertThat(production.getPlayerRepair().get(1), is("Russia"));
    assertThat(production.getPlayerRepair().get(1), is("repair"));
  }
}
