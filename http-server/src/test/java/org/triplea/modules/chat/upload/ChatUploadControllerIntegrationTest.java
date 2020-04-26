package org.triplea.modules.chat.upload;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.upload.ChatUploadClient;
import org.triplea.http.client.lobby.chat.upload.ChatUploadParams;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class ChatUploadControllerIntegrationTest extends ProtectedEndpointTest<ChatUploadClient> {
  ChatUploadControllerIntegrationTest() {
    super(AllowedUserRole.HOST, ChatUploadClient::newClient);
  }

  @Test
  void uploadChat() {
    // TODO: need to post a game and get a game-id.

    verifyEndpoint(
        client ->
            client.uploadChatMessage(
                ApiKey.newKey(),
                ChatUploadParams.builder()
                    .fromPlayer(UserName.of("player"))
                    .chatMessage("chat")
                    .gameId("game-id")
                    .build()));
  }
}
