package games.strategy.engine.pbem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.help.HelpSupport;

public class TripleAForumPoster extends AbstractForumPoster {

  private static final long serialVersionUID = -3380344469767981030L;

  private static final String tripleAForumURL = UrlConstants.TRIPLEA_FORUM.toString();

  @Override
  public boolean postTurnSummary(final String summary, final String title) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client);
      final String token = getToken(client, userId);
      try {
        post(client, token, "### " + title + "\n" + summary);
        turnSummaryRef = "Sucessfully posted!";
        return true;
      } finally {
        deleteToken(client, userId, token);
      }
    } catch (final IOException | IllegalStateException e) {
      ClientLogger.logQuietly("Failed to post game to forum", e);
      turnSummaryRef = e.getMessage();
    }
    return false;
  }

  private void post(final CloseableHttpClient client, final String token, final String text) throws IOException {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/topics/" + getTopicId());
    addTokenHeader(post, token);
    post.setEntity(new UrlEncodedFormEntity(
        Collections.singletonList(new BasicNameValuePair("content",
            text + ((m_includeSaveGame && saveGameFile != null) ? uploadSaveGame(client, token) : ""))),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final int code = response.getStatusLine().getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {
        throw new IllegalStateException("Forum responded with code " + code);
      }
    }
  }

  private String uploadSaveGame(final CloseableHttpClient client, final String token) throws IOException {
    final HttpPost fileUpload = new HttpPost(tripleAForumURL + "/api/v1/util/upload");
    fileUpload.setEntity(MultipartEntityBuilder.create()
        .addBinaryBody("files[]", saveGameFile, ContentType.APPLICATION_OCTET_STREAM, saveGameFileName)
        .build());
    HttpProxy.addProxy(fileUpload);
    addTokenHeader(fileUpload, token);
    try (CloseableHttpResponse response = client.execute(fileUpload)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String json = EntityUtils.toString(response.getEntity());
        return "\n[Savegame](" + new JSONArray(json).getJSONObject(0).getString("url") + ")";
      }
      throw new IllegalStateException("Failed to upload savegame, server returned Error Code " + status);
    }
  }

  private static void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete = new HttpDelete(tripleAForumURL + "/api/v1/users/" + userId + "/tokens/" + token);
    addTokenHeader(httpDelete, token);
    try (CloseableHttpResponse response = client.execute(httpDelete)) {
      final int code = response.getStatusLine().getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {
        // Cleanup failed, this is not a severe issue
        ClientLogger.logQuietly("Failed to delete API Token after posting, server responded with error code " + code);
      }
    }
  }

  private int getUserId(final CloseableHttpClient client) throws IOException {
    final JSONObject jsonObject = login(client);
    checkUser(jsonObject);
    return jsonObject.getInt("uid");
  }

  private static void checkUser(final JSONObject jsonObject) {
    if (jsonObject.has("message")) {
      throw new IllegalStateException(jsonObject.getString("message"));
    }
    if (jsonObject.getInt("banned") != 0) {
      throw new IllegalStateException("Your account is banned from the forum");
    }
    if (jsonObject.getInt("email:confirmed") != 1) {
      throw new IllegalStateException("Your email isn't confirmed yet!");
    }
  }

  private JSONObject login(final CloseableHttpClient client) throws IOException {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/ns/login");
    post.setEntity(new UrlEncodedFormEntity(
        Arrays.asList(newUsernameParameter(), newPasswordParameter()),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = EntityUtils.toString(response.getEntity());
      return new JSONObject(rawJson);
    }
  }

  private NameValuePair newUsernameParameter() {
    return new BasicNameValuePair("username", getUsername());
  }

  private NameValuePair newPasswordParameter() {
    return new BasicNameValuePair("password", getPassword());
  }

  private String getToken(final CloseableHttpClient client, final int userId) throws IOException {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/users/" + userId + "/tokens");
    post.setEntity(new UrlEncodedFormEntity(
        Collections.singletonList(newPasswordParameter()),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = EntityUtils.toString(response.getEntity());
      final JSONObject jsonObject = new JSONObject(rawJson);
      if (jsonObject.has("code")) {
        final String code = jsonObject.getString("code");
        if (code.equalsIgnoreCase("ok")) {
          return jsonObject.getJSONObject("payload").getString("token");
        }
        throw new IllegalStateException(
            "Failed to retrieve Token. Code: " + code + " Message: " + jsonObject.getString("message"));
      }
      throw new IllegalStateException("Failed to retrieve Token, server did not return correct JSON: " + rawJson);
    }
  }

  @Override
  public boolean supportsSaveGame() {
    return true;
  }

  @Override
  public String getDisplayName() {
    return "forums.triplea-game.org";
  }


  @Override
  public void viewPosted() {
    OpenFileUtility.openUrl(tripleAForumURL + "/topic/" + m_topicId);
  }


  @Override
  public String getTestMessage() {
    return "Testing... This may take a while";
  }

  @Override
  public IForumPoster doClone() {
    final TripleAForumPoster clone = new TripleAForumPoster();
    clone.setTopicId(getTopicId());
    clone.setIncludeSaveGame(getIncludeSaveGame());
    clone.setAlsoPostAfterCombatMove(getAlsoPostAfterCombatMove());
    clone.setPassword(getPassword());
    clone.setUsername(getUsername());
    clone.setCredentialsSaved(areCredentialsSaved());
    return clone;
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("tripleaForum.html");
  }

  private static void addTokenHeader(final HttpRequestBase request, final String token) {
    request.addHeader("Authorization", "Bearer " + token);
  }
}
