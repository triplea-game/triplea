package games.strategy.engine.pbem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.help.HelpSupport;
import games.strategy.util.Util;

public class TripleAForumPoster extends AbstractForumPoster {

  private static final long serialVersionUID = -3380344469767981030L;

  public static final String tripleAForumURL = "https://forums.triplea-game.org";

  private NameValuePair username;
  private NameValuePair password;


  @Override
  public boolean postTurnSummary(String summary, String title) {
    username = new BasicNameValuePair("username", getUsername());
    password = new BasicNameValuePair("password", getPassword());
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      int userID = getUserId(client);
      String token = getToken(client, userID);
      try {
        post(client, token, "### " + title + "\n" + summary);
        display("Sucessfully posted!");
        return true;
      } finally {
        deleteToken(client, userID, token);
      }
    } catch (JSONException e) {
      ClientLogger.logError("Invalid JSON", e);
      display(e);
    } catch (IOException e) {
      ClientLogger.logQuietly("A network error occured while trying to post", e);
      display(e);
    } catch (Exception e) {
      ClientLogger.logQuietly(e);
      display(e);
    }
    return false;
  }

  private void display(String message) {
    m_turnSummaryRef = message;
  }

  private void display(Exception e) {
    display(e.getMessage());
  }

  private void post(CloseableHttpClient client, String token, String text) throws Exception {
    HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/topics/" + getTopicId());
    post.addHeader("Authorization", "Bearer " + token);
    if (m_includeSaveGame && m_saveGameFile != null) {
      text += uploadSavegame();
    }
    post.setEntity(new UrlEncodedFormEntity(
        Arrays.asList(new BasicNameValuePair("content", text)),
        StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    client.execute(post);
  }

  private String uploadSavegame() throws Exception {
    try (CloseableHttpClient loginClient = HttpClients.custom().setDefaultRequestConfig(
        RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build()) {
      // TODO hide the warning messages
      // TODO we change this once the write API receives an update
      CookieStore cookieStore = new BasicCookieStore();
      HttpContext httpContext = new BasicHttpContext();
      httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
      String csrfToken = getCSRFToken(loginClient, httpContext);
      sendLoginPOST(loginClient, httpContext, csrfToken);

      try (CloseableHttpResponse response = upload(loginClient, httpContext, csrfToken)) {
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpURLConnection.HTTP_OK) {
          String json = Util.getStringFromInputStream(response.getEntity().getContent());
          return "\n[Savegame](" + new JSONArray(json).getJSONObject(0).getString("url") + ")";
        }
        throw new Exception("Failed to upload savegame, server returned Error Code " + status);
      }
    }
  }

  private CloseableHttpResponse upload(CloseableHttpClient client, HttpContext context, String csrfToken)
      throws ClientProtocolException, IOException {
    HttpPost fileUpload = new HttpPost(tripleAForumURL + "/api/post/upload");
    fileUpload.setEntity(MultipartEntityBuilder.create()
        // the content type affects file extension server side causing the file extension to be renamed into .bin
        .addBinaryBody("files[]", m_saveGameFile, ContentType.create("application/triplea-savegame"),
            m_saveGameFileName)
        .addTextBody("cid", "6")
        .build());
    HttpProxy.addProxy(fileUpload);
    fileUpload.addHeader("x-csrf-token", csrfToken);
    return client.execute(fileUpload, context);
  }

  private void sendLoginPOST(CloseableHttpClient client, HttpContext context, String csrfToken)
      throws ClientProtocolException, IOException {
    HttpPost login = new HttpPost(tripleAForumURL + "/login");
    HttpProxy.addProxy(login);
    login.setEntity(new UrlEncodedFormEntity(Arrays.asList(username, password)));
    login.addHeader("x-csrf-token", csrfToken);
    client.execute(login, context);
  }

  private String getCSRFToken(CloseableHttpClient client, HttpContext context)
      throws ClientProtocolException, IOException {
    HttpGet tokenGET = new HttpGet(tripleAForumURL + "/api/config");
    HttpProxy.addProxy(tokenGET);
    try (CloseableHttpResponse tokenResponse = client.execute(tokenGET, context)) {
      return new JSONObject(Util.getStringFromInputStream(tokenResponse.getEntity().getContent()))
          .getString("csrf_token");
    }
  }

  private void deleteToken(CloseableHttpClient client, int userID, String token)
      throws ClientProtocolException, IOException {
    HttpDelete httpDelete = new HttpDelete(tripleAForumURL + "/api/v1/users/" + userID + "/tokens/" + token);
    httpDelete.addHeader("Authorization", "Bearer " + token);
    client.execute(httpDelete);
  }

  private int getUserId(CloseableHttpClient client) throws Exception {
    JSONObject jsonObject = login(client, Arrays.asList(username, password));
    checkUser(client, jsonObject);
    return jsonObject.getInt("uid");
  }

  private void checkUser(CloseableHttpClient client, JSONObject jsonObject) throws Exception {
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

  private JSONObject login(CloseableHttpClient client, List<NameValuePair> entity)
      throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(tripleAForumURL + "/api/ns/login");
    post.setEntity(new UrlEncodedFormEntity(entity, StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      String rawJSON = Util.getStringFromInputStream(response.getEntity().getContent());
      return new JSONObject(rawJSON);
    }
  }

  private String getToken(CloseableHttpClient client, int userId) throws Exception {
    HttpPost post = new HttpPost(tripleAForumURL + "/api/v1/users/" + userId + "/tokens");
    post.setEntity(new UrlEncodedFormEntity(Arrays.asList(password), StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      String rawJSON = Util.getStringFromInputStream(response.getEntity().getContent());
      JSONObject jsonObject = new JSONObject(rawJSON);
      if (jsonObject.has("code")) {
        String code = jsonObject.getString("code");
        if (code.equalsIgnoreCase("ok")) {
          return jsonObject.getJSONObject("payload").getString("token");
        }
        throw new Exception("Failed to retrieve Token. Code: " + code + " Message: " + jsonObject.getString("message"));
      }
      throw new Exception("Failed to retrieve Token, server did not return correct JSON: " + rawJSON);
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
    return clone;
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("tripleaForum.html");
  }
}
