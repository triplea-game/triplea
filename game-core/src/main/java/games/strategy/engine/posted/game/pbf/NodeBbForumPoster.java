package games.strategy.engine.posted.game.pbf;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.triplea.awt.OpenFileUtility;
import org.triplea.util.Arrays;

/**
 * Posts turn summaries to a NodeBB based forum of your choice.
 *
 * <p>URL format is {@code https://your.forumurl.com/api/v2/topics/<topicID>}.
 */
public class NodeBbForumPoster {

  public static final String AXIS_AND_ALLIES_ORG_DISPLAY_NAME = "www.axisandallies.org/forums/";
  public static final String TRIPLEA_FORUM_DISPLAY_NAME = "forums.triplea-game.org";

  private final Load load = new Load(LoadSettings.builder().build());
  private final int topicId;
  private final String username;
  private final String password;
  private final String forumUrl;

  @Builder
  public static class ForumPostingParameters {
    @Nonnull private final Integer topicId;
    @Nonnull private final char[] username;
    @Nonnull private final char[] password;
    @Nonnull private final String forumUrl;
  }

  @Builder
  public static class SaveGameParameter {
    @Nonnull private final Path path;
    @Nonnull private final String displayName;
  }

  private NodeBbForumPoster(final ForumPostingParameters forumPostingParameters) {
    this.topicId = forumPostingParameters.topicId;
    this.forumUrl = forumPostingParameters.forumUrl;
    this.username =
        Arrays.withSensitiveArrayAndReturn(() -> forumPostingParameters.username, String::new);
    this.password =
        Arrays.withSensitiveArrayAndReturn(() -> forumPostingParameters.password, String::new);
  }

  /**
   * Creates a {@link NodeBbForumPoster} instance based on the given arguments and the configured
   * settings.
   */
  public static NodeBbForumPoster newInstanceByName(final String name, final int topicId) {
    switch (name) {
      case NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME:
        return new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .username(ClientSetting.tripleaForumUsername.getValueOrThrow())
                .password(ClientSetting.tripleaForumPassword.getValueOrThrow())
                .forumUrl(UrlConstants.TRIPLEA_FORUM)
                .build());
      case NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME:
        return new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .username(ClientSetting.aaForumUsername.getValueOrThrow())
                .password(ClientSetting.aaForumPassword.getValueOrThrow())
                .forumUrl(UrlConstants.AXIS_AND_ALLIES_FORUM)
                .build());
      default:
        throw new IllegalArgumentException(String.format("String '%s' must be a valid name", name));
    }
  }

  public static ImmutableSet<String> availablePosters() {
    return ImmutableSet.of(
        NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME,
        NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME);
  }

  /**
   * Called when the turn summary should be posted.
   *
   * @param summary the forum summary
   * @param title the forum title
   * @return true if the post was successful
   */
  public CompletableFuture<String> postTurnSummary(
      final String summary, final String title, @Nullable final SaveGameParameter saveGame) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client);
      final String token = getToken(client, userId);
      try {
        post(client, token, "### " + title + "\n" + summary, saveGame);
        return CompletableFuture.completedFuture("Successfully posted!");
      } finally {
        deleteToken(client, userId, token);
      }
    } catch (final IOException | IllegalStateException e) {
      final CompletableFuture<String> result = new CompletableFuture<>();
      result.completeExceptionally(e);
      return result;
    }
  }

  private void post(
      final CloseableHttpClient client,
      final String token,
      final String text,
      final SaveGameParameter saveGame)
      throws IOException {
    final HttpPost post = new HttpPost(forumUrl + "/api/v2/topics/" + topicId);
    addTokenHeader(post, token);
    post.setEntity(
        new UrlEncodedFormEntity(
            List.of(
                new BasicNameValuePair(
                    "content",
                    text + ((saveGame != null) ? uploadSaveGame(client, token, saveGame) : ""))),
            StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final int code = response.getStatusLine().getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {
        final var message =
            response.getEntity() == null
                ? ""
                : " and message '" + EntityUtils.toString(response.getEntity()) + '\'';
        throw new IllegalStateException(
            String.format("Forum responded with code %s%s", code, message));
      }
    }
  }

  private String uploadSaveGame(
      final CloseableHttpClient client, final String token, final SaveGameParameter saveGame)
      throws IOException {
    final HttpPost fileUpload = new HttpPost(forumUrl + "/api/v2/util/upload");
    fileUpload.setEntity(
        MultipartEntityBuilder.create()
            .addBinaryBody(
                "files[]",
                saveGame.path.toFile(),
                ContentType.APPLICATION_OCTET_STREAM,
                saveGame.displayName)
            .build());
    HttpProxy.addProxy(fileUpload);
    addTokenHeader(fileUpload, token);
    try (CloseableHttpResponse response = client.execute(fileUpload)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String json = EntityUtils.toString(response.getEntity());
        final String url =
            (String) ((Map<?, ?>) ((List<?>) load.loadFromString(json)).get(0)).get("url");
        return "\n[Savegame](" + url + ")";
      }
      throw new IllegalStateException(
          "Failed to upload savegame, server returned Error Code "
              + status
              + "\nMessage:\n"
              + EntityUtils.toString(response.getEntity()));
    }
  }

  private void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete =
        new HttpDelete(forumUrl + "/api/v2/users/" + userId + "/tokens/" + token);
    HttpProxy.addProxy(httpDelete);
    addTokenHeader(httpDelete, token);
    client.execute(httpDelete).close(); // ignore errors, execute and then close
  }

  private int getUserId(final CloseableHttpClient client) throws IOException {
    final Map<?, ?> jsonObject = queryUserInfo(client);
    checkUser(jsonObject);
    return (Integer) jsonObject.get("uid");
  }

  private void checkUser(final Map<?, ?> jsonObject) {
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

  private Map<?, ?> queryUserInfo(final CloseableHttpClient client) throws IOException {
    final HttpGet post = new HttpGet(forumUrl + "/api/user/username/" + username);
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      return (Map<?, ?>) load.loadFromString(EntityUtils.toString(response.getEntity()));
    }
  }

  private NameValuePair newPasswordParameter() {
    return new BasicNameValuePair("password", password);
  }

  private String getToken(final CloseableHttpClient client, final int userId) throws IOException {
    final HttpPost post = new HttpPost(forumUrl + "/api/v2/users/" + userId + "/tokens");
    post.setEntity(
        new UrlEncodedFormEntity(List.of(newPasswordParameter()), StandardCharsets.UTF_8));
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

  /** Opens a browser and go to the forum post, identified by the forumId. */
  public void viewPosted() {
    OpenFileUtility.openUrl(forumUrl + "/topic/" + topicId);
  }

  private static void addTokenHeader(final HttpRequestBase request, final String token) {
    request.addHeader("Authorization", "Bearer " + token);
  }
}
