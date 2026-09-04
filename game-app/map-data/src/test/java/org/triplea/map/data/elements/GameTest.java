package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

public class GameTest {

  @Test
  void readGamePlayTag() {
    final Game game = parseMapXml("game.xml");

    assertThat(game.getInfo()).isNotNull();
    assertThat(game.getDiceSides()).isNotNull();
    assertThat(game.getVariableList()).isNotNull();
    assertThat(game.getMap()).isNotNull();
    assertThat(game.getResourceList()).isNotNull();
    assertThat(game.getPlayerList()).isNotNull();
    assertThat(game.getUnitList()).isNotNull();
    assertThat(game.getRelationshipTypes()).isNotNull();
    assertThat(game.getTerritoryEffectList()).isNotNull();
    assertThat(game.getProduction()).isNotNull();
    assertThat(game.getTechnology()).isNotNull();
    assertThat(game.getAttachmentList()).isNotNull();
    assertThat(game.getInitialize()).isNotNull();
    assertThat(game.getPropertyList()).isNotNull();
  }
}
