package org.triplea.http.client.lobby.chat.upload;

import lombok.Builder;
import lombok.Value;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;

/**
 * Parameter object for uploading a chat message, this object is *not* transferred over the wire,
 * {@see ChatMessageUpload}.
 */
@Value
@Builder
public class ChatUploadParams {
  UserName fromPlayer;
  String chatMessage;
  String gameId;

  ChatMessageUpload toChatMessageUpload(final ApiKey apiKey) {
    return ChatMessageUpload.builder()
        .fromPlayer(fromPlayer.getValue())
        .chatMessage(chatMessage)
        .gameId(gameId)
        .apiKey(apiKey.getValue())
        .build();
  }
}
