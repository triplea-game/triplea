package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class ResourceListTest {

  @Test
  void relationshipTypesParsingTest() {
    final ResourceList resourceList = parseMapXml("resource-list.xml").getResourceList();
    assertThat(resourceList, is(notNullValue()));
    assertThat(resourceList.getResources().get(0).getName(), is("PUs"));
    assertThat(resourceList.getResources().get(1).getName(), is("Gold"));
    assertThat(resourceList.getResources().get(1).getIsDisplayedFor(), is("player1"));
  }
}
