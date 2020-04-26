package org.triplea.modules.chat.upload;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.lobby.games.LobbyGameDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;
import org.triplea.modules.game.listing.GameListing;

/**
 * Responsible for inserting chat messages into database. Validates that a given request has a
 * matching game-id and api-key pair, otherwise does a no-op. We need to be sure to validate an
 * API-key and game-id match otherwise an attacker with an http client could try to insert incorrect
 * data, game-id's are publicly known, but API-keys are not. Only the actual host should know the
 * api-key.
 *
 * <p>If the ID and API-Key pair are not valid, we will just drop the request and return a 200 If we
 * return a 400 or some other error, we'll give a potential attacker a way to try and guess
 * API-Keys.
 */
@AllArgsConstructor
class ChatUploadModule implements Consumer<ChatMessageUpload> {
  private final LobbyGameDao lobbyGameDao;
  private final BiPredicate<ApiKey, String> gameIdValidator;

  static Consumer<ChatMessageUpload> build(final Jdbi jdbi, final GameListing gameListing) {
    return new ChatUploadModule(
        jdbi.onDemand(LobbyGameDao.class), gameListing::isValidApiKeyAndGameId);
  }

  @Override
  public void accept(final ChatMessageUpload chatMessageUpload) {
    if (gameIdValidator.test(
        ApiKey.of(chatMessageUpload.getApiKey()), chatMessageUpload.getGameId())) {
      lobbyGameDao.recordChat(chatMessageUpload);
    }
  }
}
