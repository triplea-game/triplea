package org.triplea.http.client.lobby.user.account;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccountClient {

  public static final String CHANGE_PASSWORD_PATH = "/user-account/change-password";
  public static final String FETCH_EMAIL_PATH = "/user-account/fetch-email";
  public static final String CHANGE_EMAIL_PATH = "/user-account/change-email";

  private final UserAccountFeignClient userAccountFeignClient;

  public static UserAccountClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new UserAccountClient(
        HttpClient.newClient(
            UserAccountFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public void changePassword(final String newPassword) {
    userAccountFeignClient.changePassword(newPassword);
  }

  public String fetchEmail() {
    return userAccountFeignClient.fetchEmail().getUserEmail();
  }

  public void changeEmail(final String newEmail) {
    userAccountFeignClient.changeEmail(newEmail);
  }
}
