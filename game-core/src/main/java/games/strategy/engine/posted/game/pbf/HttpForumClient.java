package games.strategy.engine.posted.game.pbf;

import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

@Log
@AllArgsConstructor
class HttpForumClient implements ForumClient {

  private static final Load load = new Load(LoadSettings.builder().build());
  private final String forumUrl;

  @Override
  public GetUserIdResult getUserId(final String username) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final Map<?, ?> jsonObject = queryUserInfo(username, client);
      return buildGetUserIdResult(username, jsonObject);
    } catch (final IOException e) {
      throw new ForumPostingException(e);
    }
  }

  private Map<?, ?> queryUserInfo(final String username, final CloseableHttpClient client)
      throws IOException {
    final HttpGet post = new HttpGet(forumUrl + "/api/user/username/" + username);
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      return (Map<?, ?>) load.loadFromString(EntityUtils.toString(response.getEntity()));
    }
  }

  private static GetUserIdResult buildGetUserIdResult(
      final String username, final Map<?, ?> jsonObject) {
    if (!jsonObject.containsKey("uid")) {
      return GetUserIdResult.builder()
          .errorMessage(String.format("User %s doesn't exist.", username))
          .build();
    } else if (1 == (Integer) jsonObject.get("banned")) {
      return GetUserIdResult.builder()
          .errorMessage(String.format("User %s doesn't exist.", username))
          .build();
    } else if (1 != (Integer) jsonObject.get("email:confirmed")) {
      return GetUserIdResult.builder().errorMessage("Your email isn't confirmed yet!").build();
    } else {
      return GetUserIdResult.builder()
          .userId((Integer) jsonObject.get("uid"))
          .errorMessage("Your email isn't confirmed yet!")
          .build();
    }
  }
}
