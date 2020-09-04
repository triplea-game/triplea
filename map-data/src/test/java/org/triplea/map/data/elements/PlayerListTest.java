package org.triplea.map.data.elements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import org.junit.jupiter.api.Test;

class PlayerListTest {

  @Test
  void mapTagParsing() {
    final PlayerList playerList = parseMapXml("player-list.xml").getPlayerList();
    assertThat(playerList, is(notNullValue()));
    assertThat(playerList.getPlayers(), is(notNullValue()));
    assertThat(playerList.getPlayers(), hasSize(2));

    assertThat(playerList.getPlayers().get(0), is(notNullValue()));
    assertThat(playerList.getPlayers().get(0).getName(), is("player1"));
    assertThat(playerList.getPlayers().get(0).getOptional(), is("false"));
    assertThat(playerList.getPlayers().get(0).getCanBeDisabled(), is("false"));
    assertThat(playerList.getPlayers().get(0).getDefaultType(), is("Human"));
    assertThat(playerList.getPlayers().get(0).getIsHidden(), is("false"));

    assertThat(playerList.getPlayers().get(1), is(notNullValue()));
    assertThat(playerList.getPlayers().get(1).getName(), is("player2"));
    assertThat(playerList.getPlayers().get(1).getOptional(), is("true"));
    assertThat(playerList.getPlayers().get(1).getCanBeDisabled(), is("true"));
    assertThat(playerList.getPlayers().get(1).getDefaultType(), is("AI"));
    assertThat(playerList.getPlayers().get(1).getIsHidden(), is("true"));

    assertThat(playerList.getAlliances(), is(notNullValue()));
    assertThat(playerList.getAlliances(), hasSize(1));
    assertThat(playerList.getAlliances().get(0), is(notNullValue()));
    assertThat(playerList.getAlliances().get(0).getPlayer(), is("player1"));
    assertThat(playerList.getAlliances().get(0).getAlliance(), is("alliance1"));
  }
}
