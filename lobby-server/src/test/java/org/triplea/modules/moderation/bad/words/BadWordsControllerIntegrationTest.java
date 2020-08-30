package org.triplea.modules.moderation.bad.words;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(
    value = LobbyServerTest.LOBBY_USER_DATASET + ", integration/bad_word.yml",
    useSequenceFiltering = false)
class BadWordsControllerIntegrationTest extends ProtectedEndpointTest<ToolboxBadWordsClient> {

  BadWordsControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ToolboxBadWordsClient::newClient);
  }

  @Test
  void removeBadWord() {
    verifyEndpoint(client -> client.removeBadWord("awful"));
  }

  @Test
  void addBadWord() {
    verifyEndpoint(client -> client.addBadWord("horrible"));
  }

  @Test
  void getBadWords() {
    verifyEndpointReturningCollection(ToolboxBadWordsClient::getBadWords);
  }
}
