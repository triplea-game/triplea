package org.triplea.http.client.lobby.user.account;

import feign.RequestLine;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

public interface UserAccountClient {

  String CHANGE_PASSWORD_PATH = "/user-account/change-password";
  String FETCH_EMAIL_PATH = "/user-account/fetch-email";
  String CHANGE_EMAIL_PATH = "/user-account/change-email";

  static UserAccountClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        UserAccountClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + UserAccountClient.CHANGE_PASSWORD_PATH)
  void changePassword(String newPassword);

  @RequestLine("GET " + UserAccountClient.FETCH_EMAIL_PATH)
  FetchEmailResponse fetchEmail();

  @RequestLine("POST " + UserAccountClient.CHANGE_EMAIL_PATH)
  void changeEmail(String newEmail);
}
