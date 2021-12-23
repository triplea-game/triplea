package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class GameTest {

  @Test
  void readGamePlayTag() {
    final Game game = parseMapXml("game.xml");

    assertThat(game.getInfo(), is(notNullValue()));
    assertThat(game.getTriplea(), is(notNullValue()));
    assertThat(game.getDiceSides(), is(notNullValue()));
    assertThat(game.getVariableList(), is(notNullValue()));
    assertThat(game.getMap(), is(notNullValue()));
    assertThat(game.getResourceList(), is(notNullValue()));
    assertThat(game.getPlayerList(), is(notNullValue()));
    assertThat(game.getUnitList(), is(notNullValue()));
    assertThat(game.getRelationshipTypes(), is(notNullValue()));
    assertThat(game.getTerritoryEffectList(), is(notNullValue()));
    assertThat(game.getProduction(), is(notNullValue()));
    assertThat(game.getTechnology(), is(notNullValue()));
    assertThat(game.getAttachmentList(), is(notNullValue()));
    assertThat(game.getInitialize(), is(notNullValue()));
    assertThat(game.getPropertyList(), is(notNullValue()));
  }
}
