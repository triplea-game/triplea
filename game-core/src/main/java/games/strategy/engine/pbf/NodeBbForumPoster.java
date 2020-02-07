package games.strategy.engine.pbf;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
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

/**
 * Posts turn summaries to a NodeBB based forum of your choice.
 *
 * <p>URL format is {@code https://your.forumurl.com/api/v2/topics/<topicID>}.
 */
@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
abstract class NodeBbForumPoster implements IForumPoster {

  private final Load load = new Load(LoadSettings.builder().build());
  private final int topicId;
  private final String username;
  private final String password;

  abstract String getForumUrl();

  @Override
  public CompletableFuture<String> postTurnSummary(
      final String summary, final String title, final Path path) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client);
      final String token = getToken(client, userId);
      try {
        post(client, token, "### " + title + "\n" + summary, path);
        return CompletableFuture.completedFuture("Successfully posted!");
      } finally {
        deleteToken(client, userId, token);
      }
    } catch (final IOException | IllegalStateException e) {
      log.log(Level.SEVERE, "Failed to post game to forum", e);
      final CompletableFuture<String> result = new CompletableFuture<>();
      result.completeExceptionally(e);
      return result;
    }
  }

  private void post(
      final CloseableHttpClient client, final String token, final String text, final Path path)
      throws IOException {
    final HttpPost post = new HttpPost(getForumUrl() + "/api/v2/topics/" + topicId);
    addTokenHeader(post, token);
    post.setEntity(
        new UrlEncodedFormEntity(
            List.of(
                new BasicNameValuePair(
                    "content", text + ((path != null) ? uploadSaveGame(client, token, path) : ""))),
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
      final CloseableHttpClient client, final String token, final Path path) throws IOException {
    final HttpPost fileUpload = new HttpPost(getForumUrl() + "/api/v2/util/upload");
    fileUpload.setEntity(
        MultipartEntityBuilder.create()
            .addBinaryBody(
                "files[]",
                path.toFile(),
                ContentType.APPLICATION_OCTET_STREAM,
                path.getFileName().toString())
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
        new HttpDelete(getForumUrl() + "/api/v2/users/" + userId + "/tokens/" + token);
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
    final HttpGet post = new HttpGet(getForumUrl() + "/api/user/username/" + username);
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      return (Map<?, ?>) load.loadFromString(EntityUtils.toString(response.getEntity()));
    }
  }

  private NameValuePair newPasswordParameter() {
    return new BasicNameValuePair("password", password);
  }

  private String getToken(final CloseableHttpClient client, final int userId) throws IOException {
    final HttpPost post = new HttpPost(getForumUrl() + "/api/v2/users/" + userId + "/tokens");
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
            "Failed to retrieve Token. Code: " + code + " Message: " + jsonObject.get("message"));
      }
      throw new IllegalStateException(
          "Failed to retrieve Token, server did not return correct response: "
              + response.getStatusLine()
              + "; JSON: "
              + rawJson);
    }
  }

  @Override
  public void viewPosted() {
    OpenFileUtility.openUrl(getForumUrl() + "/topic/" + topicId);
  }

  @Override
  public String getTestMessage() {
    return "Testing... This may take a while";
  }

  private static void addTokenHeader(final HttpRequestBase request, final String token) {
    request.addHeader("Authorization", "Bearer " + token);
  }
}
