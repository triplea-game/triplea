package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class ResourceListTest {

  @Test
  void relationshipTypesParsingTest() {
    final ResourceList resourceList = parseMapXml("resource-list.xml").getResourceList();
    assertThat(resourceList).isNotNull();
    assertThat(resourceList.getResources().get(0).getName()).isEqualTo("PUs");
    assertThat(resourceList.getResources().get(1).getName()).isEqualTo("Gold");
    assertThat(resourceList.getResources().get(1).getIsDisplayedFor()).isEqualTo("player1");
  }
}
