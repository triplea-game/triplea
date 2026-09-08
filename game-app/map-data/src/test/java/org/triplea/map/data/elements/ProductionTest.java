package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class ProductionTest {

  @Test
  void productionParsingTest() {
    final Production production = parseMapXml("production.xml").getProduction();
    assertThat(production).isNotNull();
    assertThat(production.getProductionRules()).hasSize(2);

    assertThat(production.getProductionRules().get(0)).isNotNull();
    assertThat(production.getProductionRules().get(0).getName()).isEqualTo("buyInfantry");

    assertThat(production.getProductionRules().get(0).getCosts()).hasSize(2);
    assertThat(production.getProductionRules().get(0).getCosts().get(0).getResource())
        .isEqualTo("PUs");
    assertThat(production.getProductionRules().get(0).getCosts().get(0).getQuantity()).isEqualTo(2);
    assertThat(production.getProductionRules().get(0).getCosts().get(1).getResource())
        .isEqualTo("Oil");
    assertThat(production.getProductionRules().get(0).getCosts().get(1).getQuantity()).isEqualTo(3);

    assertThat(production.getProductionRules().get(0).getResults().get(0).getResourceOrUnit())
        .isEqualTo("Infantry");
    assertThat(production.getProductionRules().get(0).getResults().get(0).getQuantity())
        .isEqualTo(1);
    assertThat(production.getProductionRules().get(0).getResults().get(1).getResourceOrUnit())
        .isEqualTo("Elite");
    assertThat(production.getProductionRules().get(0).getResults().get(1).getQuantity())
        .isEqualTo(5);

    assertThat(production.getProductionRules().get(1).getName()).isEqualTo("buyTank");
    assertThat(production.getProductionRules().get(1).getCosts()).hasSize(1);
    assertThat(production.getProductionRules().get(1).getCosts().get(0).getResource())
        .isEqualTo("PUs");
    assertThat(production.getProductionRules().get(1).getCosts().get(0).getQuantity()).isEqualTo(5);
    assertThat(production.getProductionRules().get(1).getResults().get(0).getResourceOrUnit())
        .isEqualTo("Tank");
    assertThat(production.getProductionRules().get(1).getResults().get(0).getQuantity())
        .isEqualTo(1);

    assertThat(production.getRepairRules()).hasSize(1);
    assertThat(production.getRepairRules().get(0).getName())
        .isEqualTo("repairFactoryIndustrialTechnology");
    assertThat(production.getRepairRules().get(0).getCosts().get(0).getResource()).isEqualTo("PUs");
    assertThat(production.getRepairRules().get(0).getCosts().get(0).getQuantity()).isEqualTo(1);
    assertThat(production.getRepairRules().get(0).getResults().get(0).getResourceOrUnit())
        .isEqualTo("factory");
    assertThat(production.getRepairRules().get(0).getResults().get(0).getQuantity()).isEqualTo(2);

    assertThat(production.getRepairFrontiers()).hasSize(1);
    assertThat(production.getRepairFrontiers().get(0).getName()).isEqualTo("repair");
    assertThat(production.getRepairFrontiers().get(0).getRepairRules()).hasSize(1);
    assertThat(production.getRepairFrontiers().get(0).getRepairRules().get(0).getName())
        .isEqualTo("repairFactory");

    assertThat(production.getProductionFrontiers()).hasSize(1);
    assertThat(production.getProductionFrontiers().get(0).getName()).isEqualTo("NeutralFrontier");
    assertThat(production.getProductionFrontiers().get(0).getFrontierRules()).hasSize(2);
    assertThat(production.getProductionFrontiers().get(0).getFrontierRules().get(0).getName())
        .isEqualTo("buyInfantry");
    assertThat(production.getProductionFrontiers().get(0).getFrontierRules().get(1).getName())
        .isEqualTo("buyArtillery");

    assertThat(production.getPlayerProductions()).hasSize(2);
    assertThat(production.getPlayerProductions().get(0).getPlayer()).isEqualTo("Russians");
    assertThat(production.getPlayerProductions().get(0).getFrontier())
        .isEqualTo("RussiansFrontier");
    assertThat(production.getPlayerProductions().get(1).getPlayer()).isEqualTo("Italians");
    assertThat(production.getPlayerProductions().get(1).getFrontier())
        .isEqualTo("ItaliansFrontier");

    assertThat(production.getPlayerRepairs()).hasSize(2);
    assertThat(production.getPlayerRepairs().get(0).getPlayer()).isEqualTo("France");
    assertThat(production.getPlayerRepairs().get(0).getFrontier()).isEqualTo("repair");
    assertThat(production.getPlayerRepairs().get(1).getPlayer()).isEqualTo("Russia");
    assertThat(production.getPlayerRepairs().get(1).getFrontier()).isEqualTo("repair");
  }
}
