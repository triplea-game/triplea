package games.strategy.engine.posted.game.pbf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.yaml.YamlReader;

/**
 * Helper class containing the necessary logic to fetch and revoke login tokens for NodeBB forum
 * software.
 */
@AllArgsConstructor
public class NodeBbTokenGenerator {
  private final String forumUrl;

  /**
   * Data class used to wrap a newly generated token and the {@link #userId} the token was created
   * for in a single object.
   */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static class TokenInfo {
    @Nonnull private final String token;
    private final int userId;
  }

  /**
   * Generates a NodeBB login token.
   *
   * @param username The username to create the token for.
   * @param password The password used by the user to login.
   * @param otp (optional, can be null) The One-Time-Password in case the User has 2FA enabled for
   *     their account.
   * @return The {@link TokenInfo} object containing the newly generated token and the associated
   *     userId of the provided username.
   */
  public TokenInfo generateToken(
      final String username, final String password, @Nullable final String otp) {
    Preconditions.checkNotNull(username);
    Preconditions.checkNotNull(password);

    try (CloseableHttpClient client = buildClient(null)) {
      final int userId = getUserId(client, username);
      return new TokenInfo(getToken(client, userId, password, otp), userId);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to retrieve login token", e);
    }
  }

  /**
   * Revokes a NodeBB login token.
   *
   * @param token The login token to revoke.
   * @param userId The userId that the token was issued for.
   */
  public void revokeToken(final String token, final int userId) {
    try (CloseableHttpClient client = buildClient(token)) {
      deleteToken(client, userId, token);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to revoke login token", e);
    }
  }

  /**
   * Builds the HTTP client used to talk to the NodeBB token APIs. The {@code User-Agent} default
   * header identifies TripleA so that login traffic isn't lumped with anonymous bot traffic by the
   * forum's WAF.
   *
   * @param bearerToken Authorization bearer to attach as a default header. Pass {@code null} for
   *     pre-auth requests.
   */
  @VisibleForTesting
  static CloseableHttpClient buildClient(@Nullable final String bearerToken) {
    final String version = ProductVersionReader.getCurrentVersion().toString();
    final List<Header> defaultHeaders = new ArrayList<>(2);
    defaultHeaders.add(new BasicHeader("User-Agent", "triplea/" + version));
    if (bearerToken != null) {
      defaultHeaders.add(new BasicHeader("Authorization", "Bearer " + bearerToken));
    }
    return HttpClients.custom().setDefaultHeaders(defaultHeaders).disableCookieManagement().build();
  }

  private void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete =
        new HttpDelete(forumUrl + "/api/v2/users/" + userId + "/tokens/" + token);
    HttpProxy.addProxy(httpDelete);
    client.execute(httpDelete).close(); // ignore errors, execute and then close
  }

  private int getUserId(final CloseableHttpClient client, final String username)
      throws IOException {
    final Map<String, Object> jsonObject = queryUserInfo(client, username);
    checkUser(jsonObject, username);
    return (Integer) jsonObject.get("uid");
  }

  private void checkUser(final Map<?, ?> jsonObject, final String username) {
    if (!jsonObject.containsKey("uid")) {
      throw new IllegalStateException(String.format("User %s doesn't exist.", username));
    }
    Object banned = jsonObject.get("banned");
    if (banned instanceof Integer ? (Integer) banned == 1 : (Boolean) banned) {
      throw new IllegalStateException("Your account is banned from the forum.");
    }
    if (1 != (Integer) jsonObject.get("email:confirmed")) {
      throw new IllegalStateException("Your email isn't confirmed yet!");
    }
  }

  private Map<String, Object> queryUserInfo(final CloseableHttpClient client, final String username)
      throws IOException {
    final String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
    final HttpGet get = new HttpGet(forumUrl + "/api/user/username/" + encodedUsername);
    HttpProxy.addProxy(get);
    try (CloseableHttpResponse response = client.execute(get)) {
      return YamlReader.readMap(EntityUtils.toString(response.getEntity()));
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
      final Map<String, Object> jsonObject = YamlReader.readMap(rawJson);
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
