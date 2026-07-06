package games.strategy.engine.random;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
  @NonNls private static final String DICE_ROLLER_PATH = "/api/roll";
  private static final Gson GSON = new Gson();

  @Nonnull private final URI diceRollerUri;

  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String toAddress;

  @Getter(onMethod_ = @Override)
  @Nonnull
  private final String ccAddress;

  @Override
  public String getDisplayName() {
    return diceRollerUri.toString();
  }

  @Override
  public String postRequest(final int max, final int numDice) throws IOException {
    try (CloseableHttpClient httpClient =
        HttpClientBuilder.create().setRedirectStrategy(new AdvancedRedirectStrategy()).build()) {
      final HttpPost httpPost = new HttpPost(DICE_ROLLER_PATH);
      final List<NameValuePair> params =
          ImmutableList.of(
              new BasicNameValuePair("times", String.valueOf(numDice)),
              new BasicNameValuePair("max", String.valueOf(max)),
              new BasicNameValuePair("email1", getToAddress()),
              new BasicNameValuePair("email2", getCcAddress()));
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
    final JsonObject response;
    try {
      response = GSON.fromJson(string, JsonObject.class);
    } catch (final JsonSyntaxException e) {
      throw new IllegalStateException("String '" + string + "' has an invalid format.", e);
    }
    if (response == null || !response.has("status")) {
      throw new IllegalStateException("String '" + string + "' has an invalid format.");
    }
    if (!"OK".equals(response.get("status").getAsString())) {
      throw new DiceServerException(formatErrors(response));
    }

    final JsonArray dice = response.getAsJsonObject("result").getAsJsonArray("dice");
    final int[] result = new int[dice.size()];
    for (int i = 0; i < dice.size(); i++) {
      final int value = dice.get(i).getAsInt();
      Preconditions.checkState(value != 0, "Die can't be zero: '" + string + "'");
      // -1 since we are 0 based
      result[i] = value - 1;
    }
    return result;
  }

  private static String formatErrors(final JsonObject response) {
    final JsonArray errors = response.getAsJsonArray("errors");
    if (errors == null || errors.isEmpty()) {
      return "Unknown dice server error";
    }
    final List<String> messages = new ArrayList<>();
    for (final JsonElement error : errors) {
      messages.add(error.getAsString());
    }
    return String.join("; ", messages);
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
