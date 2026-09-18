package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class PropertyListTest {

  @Test
  void propertyParsing() {
    final PropertyList propertyList = parseMapXml("property-list.xml").getPropertyList();
    assertThat(propertyList).isNotNull();
    assertThat(propertyList.getProperties()).hasSize(5);

    assertThat(propertyList.getProperties().get(0).getValue()).isEqualTo("propValue");
    assertThat(propertyList.getProperties().get(0).getName()).isEqualTo("propName");
    assertThat(propertyList.getProperties().get(0).getEditable()).isTrue();
    assertThat(propertyList.getProperties().get(0).getPlayer()).isEqualTo("player1");
    assertThat(propertyList.getProperties().get(0).getMin()).isNull();
    assertThat(propertyList.getProperties().get(0).getMax()).isNull();

    assertThat(propertyList.getProperties().get(1).getValue()).isEqualTo("100");
    assertThat(propertyList.getProperties().get(1).getName()).isEqualTo("number");
    assertThat(propertyList.getProperties().get(1).getMin()).isEqualTo(1);
    assertThat(propertyList.getProperties().get(1).getMax()).isEqualTo(1000);

    assertThat(propertyList.getProperties().get(2).getValue()).isNull();
    assertThat(propertyList.getProperties().get(2).getName()).isEqualTo("notes");
    assertThat(propertyList.getProperties().get(2).getEditable()).isNull();
    assertThat(propertyList.getProperties().get(2).getPlayer()).isNull();
    assertThat(propertyList.getProperties().get(2).getValueProperty()).isNotNull();
    assertThat(propertyList.getProperties().get(2).getValueProperty().getData())
        .isEqualTo("Notes here");

    assertThat(propertyList.getProperties().get(3).getValue()).isNull();
    assertThat(propertyList.getProperties().get(3).getName()).isEqualTo("stringProperty");
    assertThat(propertyList.getProperties().get(3).getEditable()).isNull();
    assertThat(propertyList.getProperties().get(3).getPlayer()).isNull();

    assertThat(propertyList.getProperties().get(4).getValue()).isNull();
    assertThat(propertyList.getProperties().get(4).getName()).isEqualTo("numberProperty");
    assertThat(propertyList.getProperties().get(4).getEditable()).isNull();
    assertThat(propertyList.getProperties().get(4).getPlayer()).isNull();
    assertThat(propertyList.getProperties().get(4).getNumberProperty()).isNotNull();
    assertThat(propertyList.getProperties().get(4).getNumberProperty().getMin()).isEqualTo(123);
    assertThat(propertyList.getProperties().get(4).getNumberProperty().getMax()).isEqualTo(999);
  }
}
