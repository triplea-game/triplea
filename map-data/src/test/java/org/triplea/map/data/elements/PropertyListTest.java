package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class PropertyListTest {

  @Test
  void propertyParsing() {
    final PropertyList propertyList = parseMapXml("property-list.xml").getPropertyList();
    assertThat(propertyList, is(notNullValue()));
    assertThat(propertyList.getProperties(), hasSize(5));

    assertThat(propertyList.getProperties().get(0).getValue(), is("propValue"));
    assertThat(propertyList.getProperties().get(0).getName(), is("propName"));
    assertThat(propertyList.getProperties().get(0).isEditable(), is(true));
    assertThat(propertyList.getProperties().get(0).getPlayer(), is("player1"));
    assertThat(propertyList.getProperties().get(0).getMin(), is(nullValue()));
    assertThat(propertyList.getProperties().get(0).getMax(), is(nullValue()));

    assertThat(propertyList.getProperties().get(1).getValue(), is("100"));
    assertThat(propertyList.getProperties().get(1).getName(), is("number"));
    assertThat(propertyList.getProperties().get(1).getMin(), is(1));
    assertThat(propertyList.getProperties().get(1).getMax(), is(1000));

    assertThat(propertyList.getProperties().get(2).getValue(), is(""));
    assertThat(propertyList.getProperties().get(2).getName(), is("notes"));
    assertThat(propertyList.getProperties().get(2).isEditable(), is(false));
    assertThat(propertyList.getProperties().get(2).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(2).getValueProperty(), is(notNullValue()));
    assertThat(propertyList.getProperties().get(2).getValueProperty().getData(), is("Notes here"));

    assertThat(propertyList.getProperties().get(3).getValue(), is(""));
    assertThat(propertyList.getProperties().get(3).getName(), is("stringProperty"));
    assertThat(propertyList.getProperties().get(3).isEditable(), is(false));
    assertThat(propertyList.getProperties().get(3).getPlayer(), is(""));

    assertThat(propertyList.getProperties().get(4).getValue(), is(""));
    assertThat(propertyList.getProperties().get(4).getName(), is("numberProperty"));
    assertThat(propertyList.getProperties().get(4).isEditable(), is(false));
    assertThat(propertyList.getProperties().get(4).getPlayer(), is(""));
    assertThat(propertyList.getProperties().get(4).getNumberProperty(), is(notNullValue()));
    assertThat(propertyList.getProperties().get(4).getNumberProperty().getMin(), is(123));
    assertThat(propertyList.getProperties().get(4).getNumberProperty().getMax(), is(999));
  }
}
