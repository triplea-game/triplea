package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class RelationshipTypesTest {
  @Test
  void relationshipTypesParsingTest() {
    final RelationshipTypes relationshipTypes =
        parseMapXml("relationship-types.xml").getRelationshipTypes();
    assertThat(relationshipTypes, is(notNullValue()));
    assertThat(relationshipTypes.getRelationshipTypes(), hasSize(2));

    assertThat(relationshipTypes.getRelationshipTypes().get(0).getName(), is("war"));
    assertThat(relationshipTypes.getRelationshipTypes().get(1).getName(), is("peace"));
  }
}
