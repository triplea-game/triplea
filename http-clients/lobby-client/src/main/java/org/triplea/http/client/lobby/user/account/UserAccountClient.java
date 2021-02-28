package org.triplea.http.client.lobby.user.account;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccountClient {

  public static final String CHANGE_PASSWORD_PATH = "/user-account/change-password";
  public static final String FETCH_EMAIL_PATH = "/user-account/fetch-email";
  public static final String CHANGE_EMAIL_PATH = "/user-account/change-email";

  private final AuthenticationHeaders authenticationHeaders;
  private final UserAccountFeignClient userAccountFeignClient;

  public static UserAccountClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new UserAccountClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(UserAccountFeignClient.class, serverUri).get());
  }

  public void changePassword(final String newPassword) {
    userAccountFeignClient.changePassword(authenticationHeaders.createHeaders(), newPassword);
  }

  public String fetchEmail() {
    return userAccountFeignClient.fetchEmail(authenticationHeaders.createHeaders()).getUserEmail();
  }

  public void changeEmail(final String newEmail) {
    userAccountFeignClient.changeEmail(authenticationHeaders.createHeaders(), newEmail);
  }
}
