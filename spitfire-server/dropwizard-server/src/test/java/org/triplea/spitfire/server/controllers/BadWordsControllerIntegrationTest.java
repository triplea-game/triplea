package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class BadWordsControllerIntegrationTest extends ControllerIntegrationTest {

  private final ToolboxBadWordsClient client;

  BadWordsControllerIntegrationTest(final URI localhost) {
    this.client = ToolboxBadWordsClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void badRequests() {
    assertBadRequest(() -> client.addBadWord(""));
    assertBadRequest(() -> client.removeBadWord(""));
  }

  @Test
  void listBadWords() {
    assertThat(client.getBadWords(), is(not(empty())));
  }

  @Test
  void removeBadWord() {
    final List<String> badWords = client.getBadWords();

    // remember the first entry
    final String firstBadWord = badWords.get(0);

    // remove the first entry
    client.removeBadWord(firstBadWord);

    // make sure entry is removed from listing
    assertThat(client.getBadWords(), not(hasItem(firstBadWord)));
  }

  @Test
  void addBadWord() {
    assertThat(client.getBadWords(), not(hasItem("bad-word-to-be-added")));

    client.addBadWord("bad-word-to-be-added");

    assertThat(client.getBadWords(), hasItem("bad-word-to-be-added"));
  }
}
