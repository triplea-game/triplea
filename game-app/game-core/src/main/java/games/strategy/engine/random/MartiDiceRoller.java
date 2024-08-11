package games.strategy.engine.random;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NonNls;
import org.triplea.config.product.ProductVersionReader;

/** A pbem dice roller that reads its configuration from a properties file. */
@Builder
public final class MartiDiceRoller implements IRemoteDiceServer {
  private static final int MESSAGE_MAX_LENGTH = 200;
  private static final String DICE_ROLLER_PATH = "/MARTI.php";

  private final Pattern errorPattern = Pattern.compile("fatal error:(.*)!");

  /**
   * Matches a comma separated list of integers like this: {@literal your dice are: 1,2,3 <p>} or
   * this: {@literal your dice are: 12,34,56 <p>}
   */
  private final Pattern dicePattern =
      Pattern.compile("your dice are:\\s*((?:\\d+(?:,\\d+)*)?)\\s*<p>");

  @Nonnull private final URI diceRollerUri;

  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String toAddress;

  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String ccAddress;

  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String gameId;

  @Override
  public String getDisplayName() {
    return diceRollerUri.toString();
  }

  @Override
  public String postRequest(
      final int max, final int numDice, final String subjectMessage, final String gameId)
      throws IOException {
    @NonNls final String normalizedGameId = gameId.isBlank() ? "TripleA" : gameId;
    @NonNls String message = normalizedGameId + ":" + subjectMessage;
    if (message.length() > MESSAGE_MAX_LENGTH) {
      message = message.substring(0, MESSAGE_MAX_LENGTH - 1);
    }
    try (CloseableHttpClient httpClient =
        HttpClientBuilder.create().setRedirectStrategy(new AdvancedRedirectStrategy()).build()) {
      final HttpPost httpPost = new HttpPost(DICE_ROLLER_PATH);
      final List<NameValuePair> params =
          ImmutableList.of(
              new BasicNameValuePair("numdice", String.valueOf(numDice)),
              new BasicNameValuePair("numsides", String.valueOf(max)),
              new BasicNameValuePair("subject", message),
              new BasicNameValuePair("roller", getToAddress()),
              new BasicNameValuePair("gm", getCcAddress()));
      httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
      httpPost.addHeader("User-Agent", "triplea/" + ProductVersionReader.getCurrentVersion());
      final HttpHost hostConfig =
          new HttpHost(diceRollerUri.getHost(), diceRollerUri.getPort(), diceRollerUri.getScheme());
      HttpProxy.addProxy(httpPost);
      try (CloseableHttpResponse response = httpClient.execute(hostConfig, httpPost)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  @Override
  public int[] getDice(final String string, final int count) throws DiceServerException {
    final Matcher errorMatcher = errorPattern.matcher(string);
    if (errorMatcher.find()) {
      throw new DiceServerException(errorMatcher.group(1));
    }

    final Matcher diceMatcher = dicePattern.matcher(string);
    if (!diceMatcher.find()) {
      throw new IllegalStateException("String '" + string + "' has an invalid format.");
    }
    return Splitter.on(',').omitEmptyStrings().splitToList(diceMatcher.group(1)).stream()
        .mapToInt(Integer::parseInt)
        .peek(i -> Preconditions.checkState(i != 0, "Die can't be zero: '" + string + "'"))
        // -1 since we are 0 based
        .map(i -> i - 1)
        .toArray();
  }

  private static class AdvancedRedirectStrategy extends LaxRedirectStrategy {
    @Override
    public HttpUriRequest getRedirect(
        final HttpRequest request, final HttpResponse response, final HttpContext context)
        throws ProtocolException {
      final URI uri = getLocationURI(request, response, context);
      final String method = request.getRequestLine().getMethod();
      if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
        return new HttpHead(uri);
      } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
        return new HttpGet(uri);
      } else {
        final int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_TEMPORARY_REDIRECT
            || status == HttpStatus.SC_MOVED_PERMANENTLY
            || status == HttpStatus.SC_MOVED_TEMPORARILY) {
          return RequestBuilder.copy(request).setUri(uri).build();
        }
        return new HttpGet(uri);
      }
    }
  }
}
