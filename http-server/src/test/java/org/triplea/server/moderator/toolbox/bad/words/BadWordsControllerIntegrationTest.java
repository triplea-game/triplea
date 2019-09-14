package org.triplea.server.moderator.toolbox.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;
import org.triplea.server.http.AbstractDropwizardTest;

class BadWordsControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxBadWordsClient client =
      AbstractDropwizardTest.newClient(ToolboxBadWordsClient::newClient);

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void removeBadWord() {
    client.removeBadWord("awful");
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void addBadWord() {
    client.addBadWord("horrible");
  }

  @Test
  void getBadWords() {
    assertThat(client.getBadWords(), not(empty()));
  }
}
