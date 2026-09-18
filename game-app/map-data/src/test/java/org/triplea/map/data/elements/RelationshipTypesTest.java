package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class RelationshipTypesTest {
  @Test
  void relationshipTypesParsingTest() {
    final RelationshipTypes relationshipTypes =
        parseMapXml("relationship-types.xml").getRelationshipTypes();
    assertThat(relationshipTypes).isNotNull();
    assertThat(relationshipTypes.getRelationshipTypes()).hasSize(2);

    assertThat(relationshipTypes.getRelationshipTypes().get(0).getName()).isEqualTo("war");
    assertThat(relationshipTypes.getRelationshipTypes().get(1).getName()).isEqualTo("peace");
  }
}
