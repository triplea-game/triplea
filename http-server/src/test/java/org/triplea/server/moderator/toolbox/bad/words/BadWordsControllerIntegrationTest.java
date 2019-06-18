package org.triplea.server.moderator.toolbox.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;
import org.triplea.server.http.AbstractDropwizardTest;

class BadWordsControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxBadWordsClient client =
      AbstractDropwizardTest.newClient(ToolboxBadWordsClient::newClient);

  private static final ToolboxBadWordsClient clientWithBadKey =
      AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxBadWordsClient::newClient);

  @Test
  void removeBadWord() {
    client.removeBadWord("awful");
  }

  @Test
  void removeBadWordNotAuthorized() {
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.removeBadWord("bad"));
  }

  @Test
  void addBadWord() {
    client.addBadWord("horrible");
  }

  @Test
  void addBadWordNotAuthorized() {
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.removeBadWord("terrible"));
  }

  @Test
  void getBadWords() {
    assertThat(client.getBadWords(), not(empty()));
  }

  @Test
  void getBadWordsNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::getBadWords);
  }
}
