package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class PropertyListTest {

  @Test
  void propertyParsing() {
    final PropertyList propertyList = parseMapXml("property-list.xml").getPropertyList();
    assertThat(propertyList, is(notNullValue()));
    assertThat(propertyList.getProperties(), hasSize(6));

    assertThat(propertyList.getProperties().get(0).getValue(), is("propValue"));
    assertThat(propertyList.getProperties().get(0).getName(), is("propName"));
    assertThat(propertyList.getProperties().get(0).getEditable(), is("true"));
    assertThat(propertyList.getProperties().get(0).getPlayer(), is("player1"));

    assertThat(propertyList.getProperties().get(1).getValue(), is(""));
    assertThat(propertyList.getProperties().get(1).getName(), is("propName1"));
    assertThat(propertyList.getProperties().get(1).getEditable(), is("false"));
    assertThat(propertyList.getProperties().get(1).getPlayer(), is(""));

    assertThat(propertyList.getProperties().get(2).getValue(), is(""));
    assertThat(propertyList.getProperties().get(2).getName(), is("notes"));
    assertThat(propertyList.getProperties().get(2).getEditable(), is("false"));
    assertThat(propertyList.getProperties().get(2).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(2).getValueProperty(), is(notNullValue()));
    assertThat(propertyList.getProperties().get(2).getValueProperty().getData(), is("Notes here"));

    assertThat(propertyList.getProperties().get(3).getValue(), is(""));
    assertThat(propertyList.getProperties().get(3).getName(), is("booleanProperty"));
    assertThat(propertyList.getProperties().get(3).getEditable(), is("false"));
    assertThat(propertyList.getProperties().get(3).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(3).getBooleanProperty(), is(notNullValue()));

    assertThat(propertyList.getProperties().get(4).getValue(), is(""));
    assertThat(propertyList.getProperties().get(4).getName(), is("stringProperty"));
    assertThat(propertyList.getProperties().get(4).getEditable(), is("false"));
    assertThat(propertyList.getProperties().get(4).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(4).getStringProperty(), is(notNullValue()));

    assertThat(propertyList.getProperties().get(5).getValue(), is(""));
    assertThat(propertyList.getProperties().get(5).getName(), is("numberProperty"));
    assertThat(propertyList.getProperties().get(5).getEditable(), is("false"));
    assertThat(propertyList.getProperties().get(5).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(5).getNumberProperty(), is(notNullValue()));
    assertThat(propertyList.getProperties().get(5).getNumberProperty().getMin(), is("123"));
    assertThat(propertyList.getProperties().get(5).getNumberProperty().getMax(), is("999"));
  }
}
