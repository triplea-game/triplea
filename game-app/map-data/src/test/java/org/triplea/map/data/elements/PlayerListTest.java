package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class PlayerListTest {

  @Test
  void mapTagParsing() {
    final PlayerList playerList = parseMapXml("player-list.xml").getPlayerList();
    assertThat(playerList).isNotNull();
    assertThat(playerList.getPlayers()).isNotNull();
    assertThat(playerList.getPlayers()).hasSize(2);

    assertThat(playerList.getPlayers().get(0)).isNotNull();
    assertThat(playerList.getPlayers().get(0).getName()).isEqualTo("player1");
    assertThat(playerList.getPlayers().get(0).getOptional()).isNull();
    assertThat(playerList.getPlayers().get(0).getCanBeDisabled()).isNull();
    assertThat(playerList.getPlayers().get(0).getDefaultType()).isNull();
    assertThat(playerList.getPlayers().get(0).getIsHidden()).isNull();

    assertThat(playerList.getPlayers().get(1)).isNotNull();
    assertThat(playerList.getPlayers().get(1).getName()).isEqualTo("player2");
    assertThat(playerList.getPlayers().get(1).getOptional()).isTrue();
    assertThat(playerList.getPlayers().get(1).getCanBeDisabled()).isTrue();
    assertThat(playerList.getPlayers().get(1).getDefaultType()).isEqualTo("AI");
    assertThat(playerList.getPlayers().get(1).getIsHidden()).isTrue();

    assertThat(playerList.getAlliances()).isNotNull();
    assertThat(playerList.getAlliances()).hasSize(1);
    assertThat(playerList.getAlliances().get(0)).isNotNull();
    assertThat(playerList.getAlliances().get(0).getPlayer()).isEqualTo("player1");
    assertThat(playerList.getAlliances().get(0).getAlliance()).isEqualTo("alliance1");
  }
}
