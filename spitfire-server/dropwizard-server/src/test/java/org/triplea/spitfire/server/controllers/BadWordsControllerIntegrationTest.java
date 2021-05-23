package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(
    value = SpitfireServerTestExtension.LOBBY_USER_DATASET + ", integration/bad_word.yml",
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
