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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;

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

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.startup.ui.editors.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.io.FileUtils;

/**
 * A pbem dice roller that reads its configuration from a properties file.
 */
public class PropertiesDiceRoller implements IRemoteDiceServer {
  private static final long serialVersionUID = 6481409417543119539L;

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
          ClientLogger.logQuietly("Failed to read dice server properties: " + file.getAbsolutePath(), e);
        }
      }
    }
    Collections.sort(propFiles, (o1, o2) -> {
      final int n1 = Integer.parseInt(o1.getProperty("order"));
      final int n2 = Integer.parseInt(o2.getProperty("order"));
      return n1 - n2;
    });
    final List<PropertiesDiceRoller> rollers = new ArrayList<>();
    for (final Properties prop : propFiles) {
      rollers.add(new PropertiesDiceRoller(prop));
    }
    return rollers;
  }

  private final Properties m_props;
  private String m_toAddress;
  private String m_ccAddress;
  private String m_gameId;

  public PropertiesDiceRoller(final Properties props) {
    m_props = props;
  }

  @Override
  public String getDisplayName() {
    return m_props.getProperty("name");
  }

  @Override
  public EditorPanel getEditor() {
    return new DiceServerEditor(this);
  }

  @Override
  public boolean sendsEmail() {
    final String property = m_props.getProperty("send.email");
    if (property == null) {
      return true;
    }
    return Boolean.valueOf(property);
  }

  @Override
  public String postRequest(final int max, final int numDice, final String subjectMessage, String gameId,
      final String gameUuid) throws IOException {
    if (gameId.trim().length() == 0) {
      gameId = "TripleA";
    }
    String message = gameId + ":" + subjectMessage;
    final int maxLength = Integer.valueOf(m_props.getProperty("message.maxlength"));
    if (message.length() > maxLength) {
      message = message.substring(0, maxLength - 1);
    }
    try (CloseableHttpClient httpClient =
        HttpClientBuilder.create().setRedirectStrategy(new AdvancedRedirectStrategy()).build()) {
      final HttpPost httpPost = new HttpPost(m_props.getProperty("path"));
      final List<NameValuePair> params = new ArrayList<>(8);
      params.add(new BasicNameValuePair("numdice", "" + numDice));
      params.add(new BasicNameValuePair("numsides", "" + max));
      params.add(new BasicNameValuePair("modroll", "No"));
      params.add(new BasicNameValuePair("numroll", "" + 1));
      params.add(new BasicNameValuePair("subject", message));
      params.add(new BasicNameValuePair("roller", getToAddress()));
      params.add(new BasicNameValuePair("gm", getCcAddress()));
      params.add(new BasicNameValuePair("send", "true"));
      httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
      httpPost.addHeader("User-Agent", "triplea/" + ClientContext.engineVersion());
      // this is to allow a dice server to allow the user to request the emails for the game
      // rather than sending out email for each roll
      httpPost.addHeader("X-Triplea-Game-UUID", gameUuid);
      final String host = m_props.getProperty("host");
      final int port = Integer.parseInt(m_props.getProperty("port", "80"));
      final String scheme = m_props.getProperty("scheme", "http");
      final HttpHost hostConfig = new HttpHost(host, port, scheme);
      HttpProxy.addProxy(httpPost);
      try (CloseableHttpResponse response = httpClient.execute(hostConfig, httpPost)) {
        return EntityUtils.toString(response.getEntity());
      }
    }
  }

  @Override
  public String getInfoText() {
    return m_props.getProperty("infotext");
  }

  /**
   * @throws IOException
   *         if there was an error parsing the string.
   */
  @Override
  public int[] getDice(final String string, final int count) throws IOException, InvocationTargetException {
    final String errorStartString = m_props.getProperty("error.start");
    final String errorEndString = m_props.getProperty("error.end");
    // if the error strings are defined
    if ((errorStartString != null)
        && (errorStartString.length() > 0)
        && (errorEndString != null)
        && (errorEndString.length() > 0)) {
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
      rollStartString = m_props.getProperty("roll.single.start");
      rollEndString = m_props.getProperty("roll.single.end");
    } else {
      rollStartString = m_props.getProperty("roll.multiple.start");
      rollEndString = m_props.getProperty("roll.multiple.end");
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
    final StringTokenizer tokenizer = new StringTokenizer(string.substring(startIndex, endIndex), " ,", false);
    final int[] dice = new int[count];
    for (int i = 0; i < count; i++) {
      try {
        // -1 since we are 0 based
        dice[i] = Integer.parseInt(tokenizer.nextToken()) - 1;
      } catch (final NumberFormatException ex) {
        ClientLogger.logQuietly("Number format parsing: " + string, ex);
        throw new IOException(ex.getMessage());
      }
    }
    return dice;
  }

  @Override
  public String getToAddress() {
    return m_toAddress;
  }

  @Override
  public void setToAddress(final String toAddress) {
    m_toAddress = toAddress;
  }

  @Override
  public String getCcAddress() {
    return m_ccAddress;
  }

  @Override
  public void setCcAddress(final String ccAddress) {
    m_ccAddress = ccAddress;
  }

  @Override
  public boolean supportsGameId() {
    final String gameid = m_props.getProperty("gameid");
    return "true".equals(gameid);
  }

  @Override
  public void setGameId(final String gameId) {
    m_gameId = gameId;
  }

  @Override
  public String getGameId() {
    return m_gameId;
  }

  @Override
  public String getHelpText() {
    return getInfoText();
  }

  @Override
  public boolean equals(final Object other) {
    return (other instanceof PropertiesDiceRoller)
        && Objects.equals(getDisplayName(), ((IBean) other).getDisplayName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getDisplayName());
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
        if ((status == HttpStatus.SC_TEMPORARY_REDIRECT)
            || (status == HttpStatus.SC_MOVED_PERMANENTLY)
            || (status == HttpStatus.SC_MOVED_TEMPORARILY)) {
          return RequestBuilder.copy(request).setUri(uri).build();
        }
        return new HttpGet(uri);
      }
    }
  }
}
