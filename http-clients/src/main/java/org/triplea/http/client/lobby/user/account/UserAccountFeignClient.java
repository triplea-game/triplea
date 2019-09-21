package org.triplea.http.client.lobby.user.account;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface UserAccountFeignClient {
  @RequestLine("POST " + UserAccountClient.CHANGE_PASSWORD_PATH)
  void changePassword(@HeaderMap Map<String, Object> headers, String newPassword);

  @RequestLine("GET " + UserAccountClient.FETCH_EMAIL_PATH)
  FetchEmailResponse fetchEmail(@HeaderMap Map<String, Object> headers);

  @RequestLine("POST " + UserAccountClient.CHANGE_EMAIL_PATH)
  void changeEmail(@HeaderMap Map<String, Object> headers, String newEmail);
}
