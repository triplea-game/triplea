package org.triplea.http.client.lobby.chat.upload;

import static com.github.tomakehurst.wiremock.client.WireMock.post;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ChatUploadClientTest extends WireMockTest {
  private static ChatUploadClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ChatUploadClient::newClient);
  }

  @Test
  void sendGameHostingRequest(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(ChatUploadClient.UPLOAD_CHAT_PATH).willReturn(WireMock.aResponse().withStatus(200)));

    newClient(wireMockServer)
        .uploadChatMessage(
            ApiKey.newKey(),
            ChatUploadParams.builder()
                .gameId("game-id")
                .chatMessage("chat-message")
                .fromPlayer(UserName.of("player"))
                .build());
  }
}
