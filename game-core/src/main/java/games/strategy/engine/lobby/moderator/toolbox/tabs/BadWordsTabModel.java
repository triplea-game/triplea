package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.List;

import org.triplea.http.client.moderator.toolbox.AddBadWordArgs;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.moderator.toolbox.RemoveBadWordArgs;

import games.strategy.triplea.settings.ClientSetting;
import lombok.Builder;

@Builder
class BadWordsTabModel {

  private final ModeratorToolboxClient moderatorToolboxClient;

  String removeBadWord(final String wordToRemove) {
    return moderatorToolboxClient.removeBadWord(
        RemoveBadWordArgs.builder()
            .apiKey(ClientSetting.moderatorApiKey.getValueOrThrow())
            .badWord(wordToRemove)
            .build());
  }

  List<String> getBadWords() {
    return moderatorToolboxClient.getBadWords(ClientSetting.moderatorApiKey.getValueOrThrow());
  }

  String addBadWord(final String newBadWord) {
    return moderatorToolboxClient.addBadWord(
        AddBadWordArgs.builder()
            .apiKey(ClientSetting.moderatorApiKey.getValueOrThrow())
            .badWord(newBadWord)
            .build());
  }
}
