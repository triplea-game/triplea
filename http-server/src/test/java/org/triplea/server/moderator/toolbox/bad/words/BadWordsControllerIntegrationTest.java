package org.triplea.server.moderator.toolbox.bad.words;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;
import org.triplea.server.http.ProtectedEndpointTest;

class BadWordsControllerIntegrationTest extends ProtectedEndpointTest<ToolboxBadWordsClient> {

  BadWordsControllerIntegrationTest() {
    super(ToolboxBadWordsClient::newClient);
  }

  @Test
  void removeBadWord() {
    verifyEndpointReturningVoid(client -> client.removeBadWord("awful"));
  }

  @Test
  void addBadWord() {
    verifyEndpointReturningVoid(client -> client.addBadWord("horrible"));
  }

  @Test
  void getBadWords() {
    verifyEndpointReturningCollection(ToolboxBadWordsClient::getBadWords);
  }
}
