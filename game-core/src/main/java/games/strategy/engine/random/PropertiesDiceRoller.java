package games.strategy.engine.random;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.io.FileUtils;
import lombok.extern.java.Log;

/**
 * A pbem dice roller that reads its configuration from a properties file.
 */
@Log
public final class PropertiesDiceRoller implements IRemoteDiceServer {
  /**
   * Loads the property dice rollers from the properties file.
   *
   * @return the collection of available dice rollers
   */
  public static Collection<PropertiesDiceRoller> loadFromFile() {
    final File f = new File(ClientFileSystemHelper.getRootFolder(), "dice_servers");
    if (!f.exists()) {
      throw new IllegalStateException("No dice server folder:" + f);
    }
    final List<Properties> propFiles = new ArrayList<>();
    for (final File file : FileUtils.listFiles(f)) {
      if (!file.isDirectory() && file.getName().endsWith(".properties")) {
        try {
          try (InputStream fin = new FileInputStream(file)) {
            final Properties props = new Properties();
            props.load(fin);
            propFiles.add(props);
          }
        } catch (final IOException e) {
          log.log(Level.SEVERE, "Failed to read dice server properties: " + file.getAbsolutePath(), e);
        }
      }
    }
    propFiles.sort(Comparator.comparingInt(o -> Integer.parseInt(o.getProperty("order"))));
    final List<PropertiesDiceRoller> rollers = new ArrayList<>();
    for (final Properties prop : propFiles) {
      rollers.add(new PropertiesDiceRoller(prop));
    }
    return rollers;
  }

  private final Properties props;
  private String toAddress;
  private String ccAddress;
  private String gameId;

  @VisibleForTesting
  PropertiesDiceRoller(final Properties props) {
    this.props = props;
  }

  @Override
  public String getDisplayName() {
    return props.getProperty(PropertyKeys.DISPLAY_NAME);
  }

  @Override
  public String postRequest(final int max, final int numDice, final String subjectMessage, final String gameId)
      throws IOException {
    final String normalizedGameId = gameId.trim().isEmpty() ? "TripleA" : gameId;
    String message = normalizedGameId + ":" + subjectMessage;
    final int maxLength = Integer.valueOf(props.getProperty("message.maxlength"));
    if (message.length() > maxLength) {
      message = message.substring(0, maxLength - 1);
    }
    try (CloseableHttpClient httpClient =
        HttpClientBuilder.create().setRedirectStrategy(new AdvancedRedirectStrategy()).build()) {
      final HttpPost httpPost = new HttpPost(props.getProperty("path"));
      final List<NameValuePair> params = ImmutableList.of(
          new BasicNameValuePair("numdice", String.valueOf(numDice)),
          new BasicNameValuePair("numsides", String.valueOf(max)),
          new BasicNameValuePair("subject", message),
          new BasicNameValuePair("roller", getToAddress()),
          new BasicNameValuePair("gm", getCcAddress()));
      httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
      httpPost.addHeader("User-Agent", "triplea/" + ClientContext.engineVersion());
      final String host = props.getProperty("host");
      final int port = Integer.parseInt(props.getProperty("port", "-1"));
      final String scheme = props.getProperty("scheme", "http");
      final HttpHost hostConfig = new HttpHost(host, port, scheme);
      HttpProxy.addProxy(httpPost);
      try (CloseableHttpResponse response = httpClient.execute(hostConfig, httpPost)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  @Override
  public int[] getDice(final String string, final int count) throws IOException, InvocationTargetException {
    final String errorStartString = props.getProperty("error.start");
    final String errorEndString = props.getProperty("error.end");
    // if the error strings are defined
    if (errorStartString != null && errorStartString.length() > 0 && errorEndString != null
        && errorEndString.length() > 0) {
      final int startIndex = string.indexOf(errorStartString);
      if (startIndex >= 0) {
        final int endIndex = string.indexOf(errorEndString, (startIndex + errorStartString.length()));
        if (endIndex > 0) {
          final String error = string.substring(startIndex + errorStartString.length(), endIndex);
          throw new InvocationTargetException(null, error);
        }
      }
    }
    final String rollStartString;
    final String rollEndString;
    if (count == 1) {
      rollStartString = props.getProperty("roll.single.start");
      rollEndString = props.getProperty("roll.single.end");
    } else {
      rollStartString = props.getProperty("roll.multiple.start");
      rollEndString = props.getProperty("roll.multiple.end");
    }
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
      return Splitter
          .on(',')
          .omitEmptyStrings()
          .trimResults()
          .splitToList(string.substring(startIndex, endIndex))
          .stream()
          .mapToInt(Integer::parseInt)
          // -1 since we are 0 based
          .map(i -> i - 1)
          .toArray();
    } catch (final NumberFormatException ex) {
      log.log(Level.SEVERE, "Number format parsing: " + string, ex);
      throw new IOException(ex.getMessage());
    }
  }

  public void setToAddress(final String toAddress) {
    this.toAddress = toAddress;
  }

  public void setCcAddress(final String ccAddress) {
    this.ccAddress = ccAddress;
  }

  public void setGameId(final String gameId) {
    this.gameId = gameId;
  }

  @Override
  public String getToAddress() {
    return toAddress;
  }

  @Override
  public String getCcAddress() {
    return ccAddress;
  }

  @Override
  public String getGameId() {
    return gameId;
  }

  @VisibleForTesting
  interface PropertyKeys {
    String DISPLAY_NAME = "name";
  }

  private static class AdvancedRedirectStrategy extends LaxRedirectStrategy {
    @Override
    public HttpUriRequest getRedirect(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context) throws ProtocolException {
      final URI uri = getLocationURI(request, response, context);
      final String method = request.getRequestLine().getMethod();
      if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
        return new HttpHead(uri);
      } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
        return new HttpGet(uri);
      } else {
        final int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_TEMPORARY_REDIRECT || status == HttpStatus.SC_MOVED_PERMANENTLY
            || status == HttpStatus.SC_MOVED_TEMPORARILY) {
          return RequestBuilder.copy(request).setUri(uri).build();
        }
        return new HttpGet(uri);
      }
    }
  }
}
