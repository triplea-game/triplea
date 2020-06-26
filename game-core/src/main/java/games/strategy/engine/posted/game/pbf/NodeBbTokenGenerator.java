package games.strategy.engine.posted.game.pbf;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

@AllArgsConstructor
public class NodeBbTokenGenerator {
  private final String forumUrl;
  private final Load load = new Load(LoadSettings.builder().build());

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static class TokenInfo {
    @Nonnull private final String token;
    private final int userId;
  }

  public TokenInfo generateToken(
      final String username, final String password, @Nullable final String otp) {
    Preconditions.checkNotNull(username);
    Preconditions.checkNotNull(password);

    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client, username);
      return new TokenInfo(getToken(client, userId, password, otp), userId);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to retrieve login token", e);
    }
  }

  public void revokeToken(final String token, final int userId) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      deleteToken(client, userId, token);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to revoke login token", e);
    }
  }

  private void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete =
        new HttpDelete(forumUrl + "/api/v2/users/" + userId + "/tokens/" + token);
    HttpProxy.addProxy(httpDelete);
    httpDelete.addHeader("Authorization", "Bearer " + token);
    client.execute(httpDelete).close(); // ignore errors, execute and then close
  }

  private int getUserId(final CloseableHttpClient client, final String username)
      throws IOException {
    final Map<?, ?> jsonObject = queryUserInfo(client, username);
    checkUser(jsonObject, username);
    return (Integer) jsonObject.get("uid");
  }

  private void checkUser(final Map<?, ?> jsonObject, final String username) {
    if (!jsonObject.containsKey("uid")) {
      throw new IllegalStateException(String.format("User %s doesn't exist.", username));
    }
    if (1 == (Integer) jsonObject.get("banned")) {
      throw new IllegalStateException("Your account is banned from the forum.");
    }
    if (1 != (Integer) jsonObject.get("email:confirmed")) {
      throw new IllegalStateException("Your email isn't confirmed yet!");
    }
  }

  private Map<?, ?> queryUserInfo(final CloseableHttpClient client, final String username)
      throws IOException {
    final String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
    final HttpGet post = new HttpGet(forumUrl + "/api/user/username/" + encodedUsername);
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      return (Map<?, ?>) load.loadFromString(EntityUtils.toString(response.getEntity()));
    }
  }

  private String getToken(
      final CloseableHttpClient client,
      final int userId,
      final String password,
      @Nullable final String otp)
      throws IOException {
    final HttpPost post = new HttpPost(forumUrl + "/api/v2/users/" + userId + "/tokens");
    post.setEntity(
        new UrlEncodedFormEntity(
            List.of(new BasicNameValuePair("password", password)), StandardCharsets.UTF_8));
    if (otp != null) {
      post.addHeader("x-two-factor-authentication", otp);
    }
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = EntityUtils.toString(response.getEntity());
      final Map<?, ?> jsonObject = (Map<?, ?>) load.loadFromString(rawJson);
      if (jsonObject.containsKey("code")) {
        final String code = (String) Preconditions.checkNotNull(jsonObject.get("code"));
        if (code.equalsIgnoreCase("ok")) {
          return (String)
              Preconditions.checkNotNull((Map<?, ?>) jsonObject.get("payload")).get("token");
        }
        throw new IllegalStateException(
            "Incorrect password or server error.\nReturn Code: "
                + code
                + "\nMessage from server: "
                + jsonObject.get("message"));
      }
      throw new IllegalStateException(
          "Error, bad server response: " + response.getStatusLine() + "; JSON: " + rawJson);
    }
  }
}
