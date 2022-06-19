package org.triplea.http.client.lobby.user.account;

import feign.RequestLine;

interface UserAccountFeignClient {
  @RequestLine("POST " + UserAccountClient.CHANGE_PASSWORD_PATH)
  void changePassword(String newPassword);

  @RequestLine("GET " + UserAccountClient.FETCH_EMAIL_PATH)
  FetchEmailResponse fetchEmail();

  @RequestLine("POST " + UserAccountClient.CHANGE_EMAIL_PATH)
  void changeEmail(String newEmail);
}
