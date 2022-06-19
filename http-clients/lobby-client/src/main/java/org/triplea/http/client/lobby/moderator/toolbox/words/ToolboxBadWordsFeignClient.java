package org.triplea.http.client.lobby.moderator.toolbox.words;

import feign.RequestLine;
import java.util.List;

interface ToolboxBadWordsFeignClient {
  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH)
  void removeBadWord(String word);

  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_ADD_PATH)
  void addBadWord(String word);

  @RequestLine("GET " + ToolboxBadWordsClient.BAD_WORD_GET_PATH)
  List<String> getBadWords();
}
