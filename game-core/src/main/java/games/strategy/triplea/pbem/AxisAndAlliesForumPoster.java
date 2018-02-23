package games.strategy.triplea.pbem;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.apache.http.util.EntityUtils;

import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.pbem.AbstractForumPoster;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.help.HelpSupport;
import games.strategy.util.Interruptibles;

/**
 * Post turn summary to www.axisandallies.org to the thread identified by the forumId
 * URL format: https://www.axisandallies.org/forums/index.php?topic=[forumId],
 * like https://www.axisandallies.org/forums/index.php?topic=25878
 * The poster logs in, and out every time it posts, this way we don't nee to manage any state between posts
 */
public class AxisAndAlliesForumPoster extends AbstractForumPoster {
  private static final long serialVersionUID = 8896923978584346664L;
  // the patterns used to extract values from hidden form fields posted to the server
  public static final Pattern NUM_REPLIES_PATTERN =
      Pattern.compile(".*name=\"num_replies\" value=\"(\\d+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  public static final Pattern SEQ_NUM_PATTERN =
      Pattern.compile(".*name=\"seqnum\"\\svalue=\"(\\d+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  public static final Pattern SC_PATTERN =
      Pattern.compile(".*name=\"sc\"\\svalue=\"(\\w+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  // Pattern that matches if the "Notify me of replies" checkbox is checked
  public static final Pattern NOTIFY_PATTERN =
      Pattern.compile(".*id=\"check_notify\"\\schecked=\"checked\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  // 3 patterns used for error handling
  public static final Pattern AN_ERROR_OCCURRED_PATTERN =
      Pattern.compile(".*An Error Has Occurred.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  public static final Pattern ERROR_TEXT_PATTERN = Pattern
      .compile(".*<tr\\s+class=\"windowbg\">\\s*<td[^>]*>([^<]*)</td>.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  public static final Pattern ERROR_LIST_PATTERN =
      Pattern.compile(".*id=\"error_list[^>]*>\\s+([^<]*)\\s+<.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  /**
   * Logs into axisandallies.org
   * nb: Username and password are posted in clear text
   *
   * @throws Exception
   *         if login fails
   */
  private HttpContext login(final CloseableHttpClient client) throws Exception {
    final HttpPost httpPost = new HttpPost(UrlConstants.AXIS_AND_ALLIES_FORUM + "?action=login2");
    final CookieStore cookieStore = new BasicCookieStore();
    final HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
    HttpProxy.addProxy(httpPost);
    httpPost.addHeader("Accept", "*/*");
    httpPost.addHeader("Accept-Language", "en-us");
    httpPost.addHeader("Cache-Control", "no-cache");

    final List<NameValuePair> parameters = new ArrayList<>(2);
    parameters.add(new BasicNameValuePair("user", getUsername()));
    parameters.add(new BasicNameValuePair("passwrd", getPassword()));
    httpPost.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
    try (CloseableHttpResponse response = client.execute(httpPost, httpContext)) {
      int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String body = EntityUtils.toString(response.getEntity());
        if (body.toLowerCase().contains("password incorrect")) {
          throw new Exception("Incorrect Password");
        }
        // site responds with 200, and a refresh header
        final Header refreshHeader = response.getFirstHeader("Refresh");
        if (refreshHeader == null) {
          throw new Exception("Missing refresh header after login");
        }
        // refresh: 0; URL=http://...
        final String value = refreshHeader.getValue();
        final Pattern p = Pattern.compile("[^;]*;\\s*url=(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher m = p.matcher(value);
        if (m.matches()) {
          final String url = m.group(1);
          final HttpGet httpGet = new HttpGet(url);
          HttpProxy.addProxy(httpGet);
          try (CloseableHttpResponse response2 = client.execute(httpGet, httpContext)) {
            status = response2.getStatusLine().getStatusCode();
            if (status != 200) {
              // something is probably wrong, but there is not much we can do about it, we handle errors when we post
            }
          }
        } else {
          throw new Exception("The refresh header didn't contain a URL");
        }
      } else {
        throw new Exception("Failed to login to forum, server responded with status code: " + status);
      }
    }
    return httpContext;
  }

  @Override
  public boolean postTurnSummary(final String message, final String subject) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      final HttpContext httpContext = login(client);
      // Now we load the post page, and find the hidden fields needed to post
      final HttpGet httpGet =
          new HttpGet(UrlConstants.AXIS_AND_ALLIES_FORUM + "?action=post;topic=" + m_topicId + ".0");
      HttpProxy.addProxy(httpGet);
      try (CloseableHttpResponse response = client.execute(httpGet, httpContext)) {
        int status = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());
        if (status == 200) {
          final String numReplies;
          Matcher m = NUM_REPLIES_PATTERN.matcher(body);
          if (m.matches()) {
            numReplies = m.group(1);
          } else {
            throw new Exception("Hidden field 'num_replies' not found on page");
          }
          m = SEQ_NUM_PATTERN.matcher(body);
          final String seqNum;
          if (m.matches()) {
            seqNum = m.group(1);
          } else {
            throw new Exception("Hidden field 'seqnum' not found on page");
          }
          m = SC_PATTERN.matcher(body);
          final String sc;
          if (m.matches()) {
            sc = m.group(1);
          } else {
            throw new Exception("Hidden field 'sc' not found on page");
          }
          // now we have the required hidden fields to reply to
          final HttpPost httpPost = new HttpPost(UrlConstants.AXIS_AND_ALLIES_FORUM + "?action=post2;start=0;board=40");
          // Construct the multi part post
          final MultipartEntityBuilder builder = MultipartEntityBuilder.create()
              .addTextBody("topic", m_topicId)
              .addTextBody("subject", subject)
              .addTextBody("icon", "xx")
              .addTextBody("message", message)
              // If the user has chosen to receive notifications, ensure this setting is passed on
              .addTextBody("notify", NOTIFY_PATTERN.matcher(body).matches() ? "1" : "0");
          if (m_includeSaveGame && (saveGameFile != null)) {
            builder.addBinaryBody("attachment[]", saveGameFile, ContentType.APPLICATION_OCTET_STREAM,
                saveGameFileName);
          }
          builder
              .addTextBody("post", "Post")
              .addTextBody("num_replies", numReplies)
              .addTextBody("additional_options", "1")
              .addTextBody("sc", sc)
              .addTextBody("seqnum", seqNum);
          httpPost.setEntity(builder.build());
          // add headers
          httpPost.addHeader("Referer", UrlConstants.AXIS_AND_ALLIES_FORUM + "?action=post;topic="
              + m_topicId + ".0;num_replies=" + numReplies);
          httpPost.addHeader("Accept", "*/*");
          // the site has spam prevention which means you can't post until 15 seconds after login
          if (!Interruptibles.sleep(15 * 1000)) {
            return false;
          }
          httpPost.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
          HttpProxy.addProxy(httpPost);
          try (CloseableHttpResponse response2 = client.execute(httpPost, httpContext)) {
            status = response2.getStatusLine().getStatusCode();
            body = EntityUtils.toString(response2.getEntity());
            if (status == HttpURLConnection.HTTP_MOVED_TEMP) {
              // site responds with a 302 redirect back to the forum index (board=40)
              // The syntax for post is ".....topic=xx.yy" where xx is the thread id, and yy is the post number in the
              // given thread
              // since the site is lenient we can just give a high post_number to go to the last post in the thread
              turnSummaryRef = UrlConstants.AXIS_AND_ALLIES_FORUM + "?topic=" + m_topicId + ".10000";
            } else {
              // these two patterns find general errors, where the first pattern checks if the error text appears,
              // the second pattern extracts the error message. This could be the "The last posting from your IP was
              // less
              // than 15 seconds
              // ago.Please try again later"
              // this patter finds errors that are marked in red (for instance "You are not allowed to post URLs", or
              // "Some one else has posted while you vere reading"
              Matcher matcher = ERROR_LIST_PATTERN.matcher(body);
              if (matcher.matches()) {
                throw new Exception("The site gave an error: '" + matcher.group(1) + "'");
              }
              matcher = AN_ERROR_OCCURRED_PATTERN.matcher(body);
              if (matcher.matches()) {
                matcher = ERROR_TEXT_PATTERN.matcher(body);
                if (matcher.matches()) {
                  throw new Exception("The site gave an error: '" + matcher.group(1) + "'");
                }
              }
              final Header refreshHeader = response2.getFirstHeader("Refresh");
              if (refreshHeader != null) {
                // sometimes the message will be flagged as spam, and a refresh url is given
                // refresh: 0; URL=http://...topic=26114.new%3bspam=true#new
                final String value = refreshHeader.getValue();
                final Pattern p =
                    Pattern.compile("[^;]*;\\s*url=.*spam=true.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                m = p.matcher(value);
                if (m.matches()) {
                  throw new Exception("The summary was posted but was flagged as spam");
                }
              }
              throw new Exception(
                  "Unknown error, please contact the forum owner and also post a bug to the tripleA development team");
            }
          }
        } else {
          throw new Exception("Unable to load forum post " + m_topicId);
        }
      }
    } catch (final Exception e) {
      turnSummaryRef = e.getMessage();
      return false;
    }
    return true;
  }

  @Override
  public String getDisplayName() {
    return "AxisAndAllies.org";
  }

  @Override
  public IForumPoster doClone() {
    final AxisAndAlliesForumPoster clone = new AxisAndAlliesForumPoster();
    clone.setTopicId(getTopicId());
    clone.setIncludeSaveGame(getIncludeSaveGame());
    clone.setAlsoPostAfterCombatMove(getAlsoPostAfterCombatMove());
    clone.setPassword(getPassword());
    clone.setUsername(getUsername());
    clone.setCredentialsSaved(areCredentialsSaved());
    return clone;
  }

  @Override
  public boolean supportsSaveGame() {
    return true;
  }

  @Override
  public void viewPosted() {
    OpenFileUtility.openUrl(UrlConstants.AXIS_AND_ALLIES_FORUM + "?topic=" + m_topicId + ".10000");
  }

  @Override
  public String getTestMessage() {
    return "Testing, this will take about 20 seconds...";
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("axisAndAlliesForum.html");
  }
}
