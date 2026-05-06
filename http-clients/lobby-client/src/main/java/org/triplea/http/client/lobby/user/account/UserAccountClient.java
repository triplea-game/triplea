package org.triplea.http.client.lobby.user.account;

import feign.RequestLine;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

public interface UserAccountClient {

  static UserAccountClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        UserAccountClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ServerPaths.CHANGE_PASSWORD_PATH)
  void changePassword(String newPassword);

  @RequestLine("GET " + ServerPaths.FETCH_EMAIL_PATH)
  FetchEmailResponse fetchEmail();

  @RequestLine("POST " + ServerPaths.CHANGE_EMAIL_PATH)
  void changeEmail(String newEmail);
}
