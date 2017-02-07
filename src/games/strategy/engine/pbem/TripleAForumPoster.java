package games.strategy.engine.pbem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
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


  @Override
  public boolean postTurnSummary(String summary, String subject) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost post = new HttpPost("https://forums.triplea-game.org/api/ns/login");
      List<NameValuePair> entity = new ArrayList<>(2);
      entity.add(new BasicNameValuePair("username", getUsername()));
      entity.add(new BasicNameValuePair("password", getPassword()));
      post.setEntity(new UrlEncodedFormEntity(entity, StandardCharsets.UTF_8));
      HttpProxy.addProxy(post);
      try (CloseableHttpResponse response = client.execute(post)) {
        String rawJSON = Util.getStringFromInputStream(response.getEntity().getContent());
        JSONObject jsonObject = new JSONObject(rawJSON);
        if (jsonObject.has("message")) {
          throw new Exception(jsonObject.getString("message"));
        }
        if (jsonObject.getInt("banned") != 0) {
          throw new Exception("Your account is banned from the forum");
        }
        // TEMPORARY, until the login plugin implements such a feature
        HttpGet get = new HttpGet(tripleAForumURL + "/api/user/" + jsonObject.getString("userslug"));
        HttpProxy.addProxy(get);
        try (CloseableHttpResponse getResponse = client.execute(get)) {
          rawJSON = Util.getStringFromInputStream(getResponse.getEntity().getContent());
          if (!new JSONObject(rawJSON).getBoolean("email:confirmed")) {
            throw new Exception("Your email isn't confirmed yet!");
          }
        }
        int id = jsonObject.getInt("uid");
        post = new HttpPost(tripleAForumURL + "/api/v1/users/" + id + "/tokens");
        entity.remove(0);
        post.setEntity(new UrlEncodedFormEntity(entity, StandardCharsets.UTF_8));
        HttpProxy.addProxy(post);
        try (CloseableHttpResponse response2 = client.execute(post)) {
          rawJSON = Util.getStringFromInputStream(response2.getEntity().getContent());
          jsonObject = new JSONObject(rawJSON);
          if (jsonObject.has("code") && jsonObject.getString("code").equalsIgnoreCase("ok")) {
            String token = jsonObject.getJSONObject("payload").getString("token");
            try {
              post = new HttpPost(tripleAForumURL + "/api/v1/topics/" + getTopicId());
              post.addHeader("Authorization", "Bearer " + token);
              summary = "# " + subject + "\n" + summary;
              if (m_includeSaveGame && m_saveGameFile != null) {
                try (CloseableHttpClient loginClient = HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                    .build()) {// TODO hide the warning messages
                  // TODO we change this once the write API recieves an update
                  CookieStore cookieStore = new BasicCookieStore();
                  HttpContext httpContext = new BasicHttpContext();
                  httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
                  HttpGet tokenGET = new HttpGet(tripleAForumURL + "/api/config");
                  HttpProxy.addProxy(tokenGET);
                  try (CloseableHttpResponse tokenResponse = loginClient.execute(tokenGET, httpContext)) {
                    String csrfToken =
                        new JSONObject(Util.getStringFromInputStream(tokenResponse.getEntity().getContent()))
                            .getString("csrf_token");
                    HttpPost login = new HttpPost(tripleAForumURL + "/login");
                    HttpProxy.addProxy(login);
                    login.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                        new BasicNameValuePair("username", getUsername()),
                        new BasicNameValuePair("password", getPassword()))));
                    login.addHeader("x-csrf-token", csrfToken);
                    try (CloseableHttpResponse loginResponse = loginClient.execute(login, httpContext)) {
                      HttpPost fileUpload = new HttpPost(tripleAForumURL + "/api/post/upload");
                      fileUpload.setEntity(MultipartEntityBuilder.create()
                          .addBinaryBody("files[]", m_saveGameFile, ContentType.APPLICATION_OCTET_STREAM,
                              m_saveGameFileName)
                          .addTextBody("cid", "6")
                          .build());
                      HttpProxy.addProxy(fileUpload);
                      fileUpload.addHeader("x-csrf-token", csrfToken);
                      try (CloseableHttpResponse response3 = loginClient.execute(fileUpload, httpContext)) {
                        int status = response3.getStatusLine().getStatusCode();
                        if (status == HttpURLConnection.HTTP_OK) {
                          rawJSON = Util.getStringFromInputStream(response3.getEntity().getContent());
                          jsonObject = new JSONArray(rawJSON).getJSONObject(0);
                          summary += "\n[Savegame](" + jsonObject.getString("url") + ")";
                        } else {
                          throw new Exception("Failed to upload savegame, server returned Error Code " + status);
                        }
                      }
                    }
                  }
                }
              }
              post.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("content", summary)),
                  StandardCharsets.UTF_8));
              HttpProxy.addProxy(post);
              client.execute(post);
              m_turnSummaryRef = "Sucessfully posted!";
              return true;
            } finally {
              HttpDelete delete =
                  new HttpDelete(tripleAForumURL + "/api/v1/users/" + id + "/tokens/" + token);
              delete.addHeader("Authorization", "Bearer " + token);
              client.execute(delete);
            }
          }
        }
      } catch (JSONException e) {
        ClientLogger.logError("Invalid JSON", e);
      } catch (Exception e) {
        m_turnSummaryRef = e.getMessage();
        ClientLogger.logQuietly(e);
      }
    } catch (IOException e) {
      ClientLogger.logQuietly("An error occured while trying to post", e);
    }
    return false;
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
