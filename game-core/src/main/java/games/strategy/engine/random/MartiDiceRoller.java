package games.strategy.engine.random;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;
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

/** A pbem dice roller that reads its configuration from a properties file. */
@Log
@Builder
public final class MartiDiceRoller implements IRemoteDiceServer {
  private static final int MESSAGE_MAX_LENGTH = 200;
  private static final String DICE_ROLLER_PATH = "/marti.php";

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
    final String normalizedGameId = gameId.trim().isEmpty() ? "TripleA" : gameId;
    String message = normalizedGameId + ":" + subjectMessage;
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
      httpPost.addHeader("User-Agent", "triplea/" + ClientContext.engineVersion());
      final HttpHost hostConfig =
          new HttpHost(diceRollerUri.getHost(), diceRollerUri.getPort(), diceRollerUri.getScheme());
      HttpProxy.addProxy(httpPost);
      try (CloseableHttpResponse response = httpClient.execute(hostConfig, httpPost)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  @Override
  public int[] getDice(final String string, final int count)
      throws IOException, InvocationTargetException {
    final String errorStartString = "fatal error:";
    final String errorEndString = "!";
    final int errorStringStartIndex = string.indexOf(errorStartString);
    if (errorStringStartIndex >= 0) {
      final int endIndex =
          string.indexOf(errorEndString, (errorStringStartIndex + errorStartString.length()));
      if (endIndex > 0) {
        final String error =
            string.substring(errorStringStartIndex + errorStartString.length(), endIndex);
        throw new InvocationTargetException(null, error);
      }
    }

    final String rollStartString = "your dice are:";
    final String rollEndString = "<p>";
    int startIndex = string.indexOf(rollStartString);
    if (startIndex == -1) {
      throw new IOException("Could not find start index, text returned is:" + string);
    }
    startIndex += rollStartString.length();
    final int endIndex = string.indexOf(rollEndString, startIndex);
    if (endIndex == -1) {
      throw new IOException("Could not find end index");
    }
    try {
      return Splitter.on(',').omitEmptyStrings().trimResults()
          .splitToList(string.substring(startIndex, endIndex)).stream()
          .mapToInt(Integer::parseInt)
          // -1 since we are 0 based
          .map(i -> i - 1)
          .toArray();
    } catch (final NumberFormatException ex) {
      log.log(Level.SEVERE, "Number format parsing: " + string, ex);
      throw new IOException(ex.getMessage());
    }
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
