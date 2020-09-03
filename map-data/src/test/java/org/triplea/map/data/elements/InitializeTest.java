package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
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
    final Game game = parseMapXml("initialize.xml");
    final Initialize initialize = game.getInitialize();
    final OwnerInitialize ownerInitialize = initialize.getOwnerInitialize();
    final List<TerritoryOwner> territoryOwners = ownerInitialize.getTerritoryOwners();
    assertThat(territoryOwners, hasSize(2));
    assertThat(territoryOwners.get(0).getTerritory(), is("Poland"));
    assertThat(territoryOwners.get(0).getOwner(), is("Polish"));
    assertThat(territoryOwners.get(1).getTerritory(), is("France"));
    assertThat(territoryOwners.get(1).getOwner(), is("French"));

    final UnitInitialize unitInitialize = initialize.getUnitInitialize();
    final List<UnitPlacement> unitPlacements = unitInitialize.getUnitPlacements();
    assertThat(unitPlacements, hasSize(2));
    assertThat(unitPlacements.get(0).getUnitType(), is("Infantry"));
    assertThat(unitPlacements.get(0).getTerritory(), is("Poland"));
    assertThat(unitPlacements.get(0).getQuantity(), is("1"));
    assertThat(unitPlacements.get(0).getOwner(), is("Polish"));
    assertThat(unitPlacements.get(0).getHitsTaken(), is("1"));
    assertThat(unitPlacements.get(0).getUnitDamage(), is("2"));

    assertThat(unitPlacements.get(1).getUnitType(), is("Fighter"));
    assertThat(unitPlacements.get(1).getTerritory(), is("France"));
    assertThat(unitPlacements.get(1).getQuantity(), is("1"));

    final List<HeldUnits> heldUnits = unitInitialize.getHeldUnits();
    assertThat(heldUnits, hasSize(2));
    assertThat(heldUnits.get(0).getUnitType(), is("Caldari"));
    assertThat(heldUnits.get(0).getPlayer(), is("AI"));
    assertThat(heldUnits.get(0).getQuantity(), is("2"));

    assertThat(heldUnits.get(1).getUnitType(), is("Delkon"));
    assertThat(heldUnits.get(1).getPlayer(), is("AI"));
    assertThat(heldUnits.get(1).getQuantity(), is("1"));

    final ResourceInitialize resourceInitialize = initialize.getResourceInitialize();
    final List<ResourceGiven> resourcesGiven = resourceInitialize.getResourcesGiven();
    assertThat(resourcesGiven, hasSize(2));
    assertThat(resourcesGiven.get(0).getPlayer(), is("Anzac"));
    assertThat(resourcesGiven.get(0).getResource(), is("PUs"));
    assertThat(resourcesGiven.get(0).getQuantity(), is("20"));

    assertThat(resourcesGiven.get(1).getPlayer(), is("Dutch"));
    assertThat(resourcesGiven.get(1).getResource(), is("PUs"));
    assertThat(resourcesGiven.get(1).getQuantity(), is("0"));

    final RelationshipInitialize relationshipInitialize = initialize.getRelationshipInitialize();
    final List<Relationship> relationships = relationshipInitialize.getRelationships();
    assertThat(relationships, hasSize(2));
    assertThat(relationships.get(0).getType(), is("Neutrality"));
    assertThat(relationships.get(0).getRoundValue(), is("1"));
    assertThat(relationships.get(0).getPlayer1(), is("Western"));
    assertThat(relationships.get(0).getPlayer2(), is("Southern"));

    assertThat(relationships.get(1).getType(), is("War"));
    assertThat(relationships.get(1).getRoundValue(), is("1"));
    assertThat(relationships.get(1).getPlayer1(), is("Northern"));
    assertThat(relationships.get(1).getPlayer2(), is("Americans"));
  }
}
