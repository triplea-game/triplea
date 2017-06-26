package games.strategy.engine.pbem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import org.json.JSONArray;
import org.json.JSONObject;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.help.HelpSupport;
import games.strategy.util.Util;

public class TripleAForumPoster extends AbstractForumPoster {

  private static final long serialVersionUID = -3380344469767981030L;

  private static final String tripleAForumURL = "https://forums.triplea-game.org";

  @Override
  public boolean postTurnSummary(final String summary, final String title) {
    final NameValuePair username = new BasicNameValuePair("username", getUsername());
    final NameValuePair password = new BasicNameValuePair("password", getPassword());
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final int userId = getUserId(client, username, password);
      final String token = getToken(client, userId, password);
      try {
        post(client, token, "### " + title + "\n" + summary);
        m_turnSummaryRef = "Sucessfully posted!";
        return true;
      } finally {
        deleteToken(client, userId, token);
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      m_turnSummaryRef = e.getMessage();
    }
    return false;
  }

  private void post(final CloseableHttpClient client, final String token, String text) throws Exception {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/topics/" + getTopicId());
    addTokenHeader(post, token);
    if (m_includeSaveGame && m_saveGameFile != null) {
      text += uploadSaveGame(client, token);
    }
    post.setEntity(new UrlEncodedFormEntity(
        Collections.singletonList(new BasicNameValuePair("content", text)),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    client.execute(post);
  }

  private String uploadSaveGame(final CloseableHttpClient client, final String token) throws Exception {
    final HttpPost fileUpload = new HttpPost(tripleAForumURL + "/api/v1/util/upload");
    fileUpload.setEntity(MultipartEntityBuilder.create()
        .addBinaryBody("files[]", m_saveGameFile, ContentType.APPLICATION_OCTET_STREAM, m_saveGameFileName)
        .build());
    HttpProxy.addProxy(fileUpload);
    addTokenHeader(fileUpload, token);
    try (CloseableHttpResponse response = client.execute(fileUpload)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String json = Util.getStringFromInputStream(response.getEntity().getContent());
        return "\n[Savegame](" + new JSONArray(json).getJSONObject(0).getString("url") + ")";
      }
      throw new Exception("Failed to upload savegame, server returned Error Code " + status);
    }
  }

  private static void deleteToken(final CloseableHttpClient client, final int userId, final String token)
      throws IOException {
    final HttpDelete httpDelete = new HttpDelete(tripleAForumURL + "/api/v1/users/" + userId + "/tokens/" + token);
    addTokenHeader(httpDelete, token);
    client.execute(httpDelete);
  }

  private static int getUserId(final CloseableHttpClient client, final NameValuePair username,
      final NameValuePair password) throws Exception {
    final JSONObject jsonObject = login(client, Arrays.asList(username, password));
    checkUser(jsonObject);
    return jsonObject.getInt("uid");
  }

  private static void checkUser(final JSONObject jsonObject) throws Exception {
    if (jsonObject.has("message")) {
      throw new Exception(jsonObject.getString("message"));
    }
    if (jsonObject.getInt("banned") != 0) {
      throw new Exception("Your account is banned from the forum");
    }
    if (jsonObject.getInt("email:confirmed") != 1) {
      throw new Exception("Your email isn't confirmed yet!");
    }
  }

  private static JSONObject login(final CloseableHttpClient client, final List<NameValuePair> entity)
      throws IOException {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/ns/login");
    post.setEntity(new UrlEncodedFormEntity(entity, StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = Util.getStringFromInputStream(response.getEntity().getContent());
      return new JSONObject(rawJson);
    }
  }

  private static String getToken(final CloseableHttpClient client, final int userId, final NameValuePair password)
      throws Exception {
    final HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/users/" + userId + "/tokens");
    post.setEntity(new UrlEncodedFormEntity(Collections.singletonList(password), StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final String rawJson = Util.getStringFromInputStream(response.getEntity().getContent());
      final JSONObject jsonObject = new JSONObject(rawJson);
      if (jsonObject.has("code")) {
        final String code = jsonObject.getString("code");
        if (code.equalsIgnoreCase("ok")) {
          return jsonObject.getJSONObject("payload").getString("token");
        }
        throw new Exception("Failed to retrieve Token. Code: " + code + " Message: " + jsonObject.getString("message"));
      }
      throw new Exception("Failed to retrieve Token, server did not return correct JSON: " + rawJson);
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
    OpenFileUtility.openURL(tripleAForumURL + "/topic/" + m_topicId);
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
