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

    assertThat(production.getProductionRules().get(0).getCosts(), hasSize(2));
    assertThat(production.getProductionRules().get(0).getCosts().get(0).getResource(), is("PUs"));
    assertThat(production.getProductionRules().get(0).getCosts().get(0).getQuantity(), is("2"));
    assertThat(production.getProductionRules().get(0).getCosts().get(1).getResource(), is("Oil"));
    assertThat(production.getProductionRules().get(0).getCosts().get(1).getQuantity(), is("3"));

    assertThat(
        production.getProductionRules().get(0).getResults().get(0).getResourceOrUnit(),
        is("Infantry"));
    assertThat(production.getProductionRules().get(0).getResults().get(0).getQuantity(), is("1"));
    assertThat(
        production.getProductionRules().get(0).getResults().get(1).getResourceOrUnit(),
        is("Elite"));
    assertThat(production.getProductionRules().get(0).getResults().get(1).getQuantity(), is("5"));

    assertThat(production.getProductionRules().get(1).getName(), is("buyTank"));
    assertThat(production.getProductionRules().get(1).getCosts(), hasSize(1));
    assertThat(production.getProductionRules().get(1).getCosts().get(0).getResource(), is("PUs"));
    assertThat(production.getProductionRules().get(1).getCosts().get(0).getQuantity(), is("5"));
    assertThat(
        production.getProductionRules().get(1).getResults().get(0).getResourceOrUnit(), is("Tank"));
    assertThat(production.getProductionRules().get(1).getResults().get(0).getQuantity(), is("1"));

    assertThat(production.getRepairRules(), hasSize(1));
    assertThat(
        production.getRepairRules().get(0).getName(), is("repairFactoryIndustrialTechnology"));
    assertThat(production.getRepairRules().get(0).getCosts().get(0).getResource(), is("PUs"));
    assertThat(production.getRepairRules().get(0).getCosts().get(0).getQuantity(), is("1"));
    assertThat(
        production.getRepairRules().get(0).getResults().get(0).getResourceOrUnit(), is("factory"));
    assertThat(production.getRepairRules().get(0).getResults().get(0).getQuantity(), is("2"));

    assertThat(production.getRepairFrontiers(), hasSize(1));
    assertThat(production.getRepairFrontiers().get(0).getName(), is("repair"));
    assertThat(production.getRepairFrontiers().get(0).getRepairRules(), hasSize(1));
    assertThat(
        production.getRepairFrontiers().get(0).getRepairRules().get(0).getName(),
        is("repairFactory"));

    assertThat(production.getProductionFrontiers(), hasSize(1));
    assertThat(production.getProductionFrontiers().get(0).getName(), is("NeutralFrontier"));
    assertThat(production.getProductionFrontiers().get(0).getFrontierRules(), hasSize(2));
    assertThat(
        production.getProductionFrontiers().get(0).getFrontierRules().get(0).getName(),
        is("buyInfantry"));
    assertThat(
        production.getProductionFrontiers().get(0).getFrontierRules().get(1).getName(),
        is("buyArtillery"));

    assertThat(production.getPlayerProductions(), hasSize(2));
    assertThat(production.getPlayerProductions().get(0).getPlayer(), is("Russians"));
    assertThat(production.getPlayerProductions().get(0).getFrontier(), is("RussiansFrontier"));
    assertThat(production.getPlayerProductions().get(1).getPlayer(), is("Italians"));
    assertThat(production.getPlayerProductions().get(1).getFrontier(), is("ItaliansFrontier"));

    assertThat(production.getPlayerRepairs(), hasSize(2));
    assertThat(production.getPlayerRepairs().get(0), is("France"));
    assertThat(production.getPlayerRepairs().get(0), is("repair"));
    assertThat(production.getPlayerRepairs().get(1), is("Russia"));
    assertThat(production.getPlayerRepairs().get(1), is("repair"));
  }
}
