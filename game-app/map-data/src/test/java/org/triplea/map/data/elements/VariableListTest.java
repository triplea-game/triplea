package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class VariableListTest {
  @Test
  void variableListParsing() {
    final VariableList variableList = parseMapXml("variable-list.xml").getVariableList();
    assertThat(variableList.getVariables()).hasSize(2);
    assertThat(variableList.getVariables().get(0).getName()).isEqualTo("AllHeroUnits");
    assertThat(variableList.getVariables().get(0).getElements()).hasSize(4);
    assertThat(variableList.getVariables().get(0).getElements().get(0).getName())
        .isEqualTo("Owl-Form");
    assertThat(variableList.getVariables().get(0).getElements().get(1).getName())
        .isEqualTo("Bear-Form");
    assertThat(variableList.getVariables().get(0).getElements().get(2).getName())
        .isEqualTo("Sevis");
    assertThat(variableList.getVariables().get(0).getElements().get(3).getName())
        .isEqualTo("Sian-tsu");

    assertThat(variableList.getVariables().get(1).getElements()).hasSize(2);
    assertThat(variableList.getVariables().get(1).getName()).isEqualTo("AllAllianceHeroUnits");
    assertThat(variableList.getVariables().get(1).getElements().get(0).getName())
        .isEqualTo("Arthur");
    assertThat(variableList.getVariables().get(1).getElements().get(1).getName())
        .isEqualTo("Khorman");
  }
}
