package org.triplea.map.data.elements;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;


class VariableListTest {
  @Test
  void variableListParsing() {
    final VariableList variableList = parseMapXml("variable-list.xml").getVariableList();
    assertThat(variableList.getVariables(), hasSize(2));
    assertThat(variableList.getVariables().get(0).getName(), is("AllHeroUnits"));
    assertThat(variableList.getVariables().get(0).getElements(), hasSize(4));
    assertThat(variableList.getVariables().get(0).getElements().get(0).getName(), is("Owl-Form"));
    assertThat(variableList.getVariables().get(0).getElements().get(1).getName(), is("Bear-Form"));
    assertThat(variableList.getVariables().get(0).getElements().get(2).getName(), is("Sevis"));
    assertThat(variableList.getVariables().get(0).getElements().get(3).getName(), is("Sian-tsu"));

    assertThat(variableList.getVariables().get(1).getElements(), hasSize(2));
    assertThat(variableList.getVariables().get(1).getName(), is("AllAllianceHeroUnits"));
    assertThat(variableList.getVariables().get(1).getElements().get(0).getName(), is("Arthur"));
    assertThat(variableList.getVariables().get(1).getElements().get(1).getName(), is("Khorman"));
  }
}
