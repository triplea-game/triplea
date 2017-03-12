package games.strategy.engine.pbem;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.help.HelpSupport;
import games.strategy.util.Util;

/**
 * A poster for www.tripleawarclub.org forum
 * We log in and out every time we post, so we don't need to keep state.
 */
public class TripleAWarClubForumPoster extends AbstractForumPoster {
  private static final long serialVersionUID = -4017550807078258152L;
  private static final String WAR_CLUB_FORUM_URL = "http://www.tripleawarclub.org/modules/newbb";
  private static Pattern s_XOOPS_TOKEN_REQUEST =
      Pattern.compile(".*XOOPS_TOKEN_REQUEST[^>]*value=\"([^\"]*)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  /**
   * Logs into the website
   *
   * @throws Exception
   *         if login fails
   */
  private HttpContext login(CloseableHttpClient client) throws Exception {
    HttpPost httpPost = new HttpPost("http://www.tripleawarclub.org/user.php");
    CookieStore cookieStore = new BasicCookieStore();
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
    List<NameValuePair> pairs = new ArrayList<>();
    pairs.add(new BasicNameValuePair("uname", getUsername()));
    pairs.add(new BasicNameValuePair("pass", getPassword()));
    pairs.add(new BasicNameValuePair("submit", "Login"));
    pairs.add(new BasicNameValuePair("rememberme", "On"));
    pairs.add(new BasicNameValuePair("xoops_redirect", "/"));
    pairs.add(new BasicNameValuePair("op", "login"));
    httpPost.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
    HttpProxy.addProxy(httpPost);
    try (CloseableHttpResponse response = client.execute(httpPost, httpContext)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status != HttpURLConnection.HTTP_OK) {
        throw new Exception("Login failed, server returned status: " + status);
      }
      final String body = Util.getStringFromInputStream(response.getEntity().getContent());
      final String lowerBody = body.toLowerCase();
      if (lowerBody.contains("incorrect login!")) {
        throw new Exception("Incorrect login credentials");
      }
      if (!lowerBody.contains("thank you for logging in")) {
        System.out.println("Unknown login error, site response " + body);
        throw new Exception("Unknown login error");
      }
    }
    return httpContext;
  }

  /**
   * Post the turn summary and save game to the forum
   * After login we must load the post page to get the XOOPS_TOKEN_REQUEST (which I think is CSRF nounce)
   * then we can post the reply
   *
   * @param summary
   *        the forum summary
   * @param subject
   *        the forum subject
   * @return true if the post was successful
   */
  @Override
  public boolean postTurnSummary(final String summary, final String subject) {
    try (CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build()) {
      HttpContext httpContext = login(client);
      // load the reply page
      final String s_forumId = "20";
      final String url =
          WAR_CLUB_FORUM_URL + "/reply.php?forum=" + s_forumId + "&topic_id=" + URLEncoder.encode(m_topicId, StandardCharsets.UTF_8.name());
      String XOOPS_TOKEN_REQUEST;
      HttpGet httpGet = new HttpGet(url);
      HttpProxy.addProxy(httpGet);
      try (CloseableHttpResponse response = client.execute(httpGet, httpContext)) {
        final int status = response.getStatusLine().getStatusCode();
        if (status != HttpURLConnection.HTTP_OK) {
          throw new Exception("Could not load reply page: " + url + ". Site returned " + status);
        }
        final String body = Util.getStringFromInputStream(response.getEntity().getContent());
        final Matcher m = s_XOOPS_TOKEN_REQUEST.matcher(body);
        if (!m.matches()) {
          throw new Exception("Unable to find 'XOOPS_TOKEN_REQUEST' form field on reply page");
        }
        XOOPS_TOKEN_REQUEST = m.group(1);
      }

      MultipartEntityBuilder builder = MultipartEntityBuilder.create()
          .addTextBody("subject", subject)
          .addTextBody("message", summary)
          .addTextBody("forum", s_forumId)
          .addTextBody("topic_id", m_topicId)
          .addTextBody("XOOPS_TOKEN_REQUEST", XOOPS_TOKEN_REQUEST)
          .addTextBody("xoops_upload_file[]", "userfile")
          .addTextBody("contents_submit", "Submit")
          .addTextBody("doxcode", "1")
          .addTextBody("dosmiley", "1")
          .addTextBody("dohtml", "1")
          .addTextBody("dobr", "1")
          .addTextBody("editor", "dhtmltextarea");
      if (m_includeSaveGame && m_saveGameFile != null) {
        builder.addBinaryBody("userfile", m_saveGameFile, ContentType.APPLICATION_OCTET_STREAM, m_saveGameFileName);
      }
      HttpEntity entity = builder.build();
      HttpPost httpPost = new HttpPost(WAR_CLUB_FORUM_URL + "/post.php");
      HttpProxy.addProxy(httpPost);
      httpPost.setEntity(entity);

      try (CloseableHttpResponse response = client.execute(httpPost, httpContext)) {
        final int status = response.getStatusLine().getStatusCode();
        if (status != HttpURLConnection.HTTP_OK) {
          throw new Exception("Posting summary failed, the server returned status: " + status);
        }
        final String body = Util.getStringFromInputStream(response.getEntity().getContent());
        if (!body.toLowerCase().contains("thanks for your submission!")) {
          throw new Exception("Posting summary failed, the server didn't respond with thank you message");
        }
        m_turnSummaryRef =
            "www.tripleawarclub.org/modules/newbb/viewtopic.php?topic_id=" + m_topicId + "&forum=" + s_forumId;
      }
      // now logout, this is just to be nice, so we don't care if this fails
      try {
        httpGet = new HttpGet("http://www.tripleawarclub.org/user.php?op=logout");
        HttpProxy.addProxy(httpGet);
        client.execute(httpGet, httpContext);
      } catch (Exception e) {
        ClientLogger.logQuietly("Failed to log out", e);
      }
    } catch (final Exception e) {
      m_turnSummaryRef = e.getMessage();
      ClientLogger.logQuietly(e);
      return false;
    }
    return true;
  }

  @Override
  public String getTestMessage() {
    return "Testing, this will take a couple of seconds...";
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("tripleAWarClubForum.html");
  }

  @Override
  public IForumPoster doClone() {
    final TripleAWarClubForumPoster clone = new TripleAWarClubForumPoster();
    clone.setTopicId(getTopicId());
    clone.setIncludeSaveGame(getIncludeSaveGame());
    clone.setAlsoPostAfterCombatMove(getAlsoPostAfterCombatMove());
    clone.setPassword(getPassword());
    clone.setUsername(getUsername());
    return clone;
  }

  @Override
  public boolean supportsSaveGame() {
    return true;
  }

  @Override
  public String getDisplayName() {
    return "TripleaWarClub.org";
  }

  @Override
  public void viewPosted() {
    final String url = WAR_CLUB_FORUM_URL + "/viewtopic.php?topic_id=" + m_topicId;
    OpenFileUtility.openURL(url);
  }
}
