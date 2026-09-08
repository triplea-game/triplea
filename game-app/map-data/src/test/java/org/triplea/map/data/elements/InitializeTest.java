package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.Initialize.RelationshipInitialize;
import static org.triplea.map.data.elements.Initialize.ResourceInitialize.ResourceGiven;
import static org.triplea.map.data.elements.Initialize.UnitInitialize.HeldUnits;
import static org.triplea.map.data.elements.Initialize.UnitInitialize.UnitPlacement;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Initialize.OwnerInitialize;
import org.triplea.map.data.elements.Initialize.OwnerInitialize.TerritoryOwner;
import org.triplea.map.data.elements.Initialize.RelationshipInitialize.Relationship;
import org.triplea.map.data.elements.Initialize.ResourceInitialize;
import org.triplea.map.data.elements.Initialize.UnitInitialize;

class InitializeTest {

  @Test
  void readInitilizeTag() {
    final Initialize initialize = parseMapXml("initialize.xml").getInitialize();
    final OwnerInitialize ownerInitialize = initialize.getOwnerInitialize();
    final List<TerritoryOwner> territoryOwners = ownerInitialize.getTerritoryOwners();
    assertThat(territoryOwners).hasSize(2);
    assertThat(territoryOwners.get(0).getTerritory()).isEqualTo("Poland");
    assertThat(territoryOwners.get(0).getOwner()).isEqualTo("Polish");
    assertThat(territoryOwners.get(1).getTerritory()).isEqualTo("France");
    assertThat(territoryOwners.get(1).getOwner()).isEqualTo("French");

    final UnitInitialize unitInitialize = initialize.getUnitInitialize();
    final List<UnitPlacement> unitPlacements = unitInitialize.getUnitPlacements();
    assertThat(unitPlacements).hasSize(2);
    assertThat(unitPlacements.get(0).getUnitType()).isEqualTo("Infantry");
    assertThat(unitPlacements.get(0).getTerritory()).isEqualTo("Poland");
    assertThat(unitPlacements.get(0).getQuantity()).isEqualTo(1);
    assertThat(unitPlacements.get(0).getOwner()).isEqualTo("Polish");
    assertThat(unitPlacements.get(0).getHitsTaken()).isEqualTo(1);
    assertThat(unitPlacements.get(0).getUnitDamage()).isEqualTo(2);

    assertThat(unitPlacements.get(1).getUnitType()).isEqualTo("Fighter");
    assertThat(unitPlacements.get(1).getTerritory()).isEqualTo("France");
    assertThat(unitPlacements.get(1).getQuantity()).isEqualTo(1);

    final List<HeldUnits> heldUnits = unitInitialize.getHeldUnits();
    assertThat(heldUnits).hasSize(2);
    assertThat(heldUnits.get(0).getUnitType()).isEqualTo("Caldari");
    assertThat(heldUnits.get(0).getPlayer()).isEqualTo("AI");
    assertThat(heldUnits.get(0).getQuantity()).isEqualTo(2);

    assertThat(heldUnits.get(1).getUnitType()).isEqualTo("Delkon");
    assertThat(heldUnits.get(1).getPlayer()).isEqualTo("AI");
    assertThat(heldUnits.get(1).getQuantity()).isEqualTo(1);

    final ResourceInitialize resourceInitialize = initialize.getResourceInitialize();
    final List<ResourceGiven> resourcesGiven = resourceInitialize.getResourcesGiven();
    assertThat(resourcesGiven).hasSize(2);
    assertThat(resourcesGiven.get(0).getPlayer()).isEqualTo("Anzac");
    assertThat(resourcesGiven.get(0).getResource()).isEqualTo("PUs");
    assertThat(resourcesGiven.get(0).getQuantity()).isEqualTo(20);

    assertThat(resourcesGiven.get(1).getPlayer()).isEqualTo("Dutch");
    assertThat(resourcesGiven.get(1).getResource()).isEqualTo("PUs");
    assertThat(resourcesGiven.get(1).getQuantity()).isEqualTo(0);

    final RelationshipInitialize relationshipInitialize = initialize.getRelationshipInitialize();
    final List<Relationship> relationships = relationshipInitialize.getRelationships();
    assertThat(relationships).hasSize(2);
    assertThat(relationships.get(0).getType()).isEqualTo("Neutrality");
    assertThat(relationships.get(0).getRoundValue()).isEqualTo(1);
    assertThat(relationships.get(0).getPlayer1()).isEqualTo("Western");
    assertThat(relationships.get(0).getPlayer2()).isEqualTo("Southern");

    assertThat(relationships.get(1).getType()).isEqualTo("War");
    assertThat(relationships.get(1).getRoundValue()).isEqualTo(1);
    assertThat(relationships.get(1).getPlayer1()).isEqualTo("Northern");
    assertThat(relationships.get(1).getPlayer2()).isEqualTo("Americans");
  }
}
