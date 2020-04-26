package org.triplea.http.client.lobby.chat.upload;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/** Http client to send chat message history to the lobby server. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatUploadClient {
  public static final String UPLOAD_CHAT_PATH = "/lobby/chat/upload";
  private final AuthenticationHeaders authenticationHeaders;
  private final ChatUploadFeignClient chatUploadFeignClient;

  public static ChatUploadClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ChatUploadClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ChatUploadFeignClient.class, serverUri).get());
  }

  public void uploadChatMessage(
      final ApiKey apiKey, final ChatUploadParams uploadChatMessageParams) {
    chatUploadFeignClient.uploadChat(
        authenticationHeaders.createHeaders(), uploadChatMessageParams.toChatMessageUpload(apiKey));
  }
}
