package games.strategy.engine.pbem;

import java.io.File;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.framework.startup.ui.editors.MicroWebPosterEditor;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.help.HelpSupport;
import games.strategy.util.Util;

public class TripleAWebPoster implements IWebPoster {
  private static final long serialVersionUID = -3013355800798928625L;
  private String m_host = MicroWebPosterEditor.HTTP_BLANK;
  private Vector<String> m_allHosts = new Vector<>();
  private String m_siteId = "";
  private boolean m_mailSaveGame = true;
  private String m_gameName = "";
  private transient String m_serverMessage = "";
  private transient File m_saveGameFile = null;
  private transient String m_saveGameFileName = "";
  private String[] parties;

  private static Collection<String> getAlliances(final GameData gameData) {
    final Collection<String> rVal = new TreeSet<>();
    for (final String alliance : gameData.getAllianceTracker().getAlliances()) {
      if (gameData.getAllianceTracker().getPlayersInAlliance(alliance).size() > 1) {
        rVal.add(alliance);
      }
    }
    return rVal;
  }

  private static String getProductionData(final GameData gameData) {
    gameData.acquireReadLock();
    try {
      final Collection<String> alliances = getAlliances(gameData);
      final ProductionStat prodStat = new ProductionStat();
      String result = "";
      for (final String alliance : alliances) {
        final int value = (int) prodStat.getValue(alliance, gameData);
        if (!result.equals("")) {
          result += ";";
        }
        result += alliance + "=" + value;
      }
      return result;
    } finally {
      gameData.releaseReadLock();
    }
  }

  @Override
  public boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player,
      final int round) {
    try {
      MultipartEntityBuilder builder = MultipartEntityBuilder.create()
          .addTextBody("siteid", m_siteId);
      if (gameData != null) {
        builder.addTextBody("production", getProductionData(gameData));
      }
      builder
          .addTextBody("gamename", m_gameName)
          .addTextBody("player", player)
          .addTextBody("summary", turnSummary)
          .addTextBody("round", "" + round)
          .addTextBody("sendmail", m_mailSaveGame ? "true" : "false");
      if (m_saveGameFile != null) {
        builder.addBinaryBody("userfile", m_saveGameFile, ContentType.APPLICATION_OCTET_STREAM, m_saveGameFileName);
      }
      m_serverMessage = executePost(m_host, "upload.php", builder.build());
      if (!m_serverMessage.toLowerCase().contains("success")) {
        System.out.println("Unknown error, site response: " + m_serverMessage);
        return false;
      }
    } catch (final Exception e) {
      m_serverMessage = e.getMessage();
      ClientLogger.logQuietly(e);
      return false;
    }
    return true;
  }

  public static String executePost(final String host, final String path, final HttpEntity entity) throws Exception {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(getHostUrlPrefix(host) + path);
      HttpHost hostConfig = new HttpHost(host);
      HttpProxy.addProxy(httpPost);
      httpPost.setEntity(entity);
      try (CloseableHttpResponse response = client.execute(hostConfig, httpPost)) {

        final int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
          throw new Exception("Post command to " + host + " failed, the server returned status: " + status);
        }
        return Util.getStringFromInputStream(response.getEntity().getContent());
      }
    }
  }

  @Override
  public boolean getMailSaveGame() {
    return m_mailSaveGame;
  }

  @Override
  public void setMailSaveGame(final boolean mail) {
    m_mailSaveGame = mail;
  }

  @Override
  public void addSaveGame(final File saveGame, final String fileName) {
    m_saveGameFile = saveGame;
    m_saveGameFileName = fileName;
  }

  @Override
  public EditorPanel getEditor() {
    return new MicroWebPosterEditor(this, parties);
  }

  @Override
  public boolean sameType(final IBean other) {
    return getClass() == other.getClass();
  }

  @Override
  public String getTestMessage() {
    return "Testing, this will take a couple of seconds...";
  }

  @Override
  public String getServerMessage() {
    return m_serverMessage;
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("tripleAMicroWebsite.html");
  }

  @Override
  public IWebPoster doClone() {
    final TripleAWebPoster clone = new TripleAWebPoster();
    clone.setMailSaveGame(getMailSaveGame());
    clone.setHost(getHost());
    clone.setAllHosts(new Vector<>(getAllHosts()));
    clone.setSiteId(getSiteId());
    clone.setGameName(getGameName());
    return clone;
  }

  @Override
  public String getDisplayName() {
    return "TripleA Micro Web Site";
  }

  @Override
  public String getSiteId() {
    return m_siteId;
  }

  @Override
  public String getHost() {
    return m_host;
  }

  @Override
  public Vector<String> getAllHosts() {
    return m_allHosts;
  }

  private static String getHostUrlPrefix(final String host) {
    if (host.endsWith("/")) {
      return host;
    } else {
      return host + "/";
    }
  }

  @Override
  public String getGameName() {
    return m_gameName;
  }

  @Override
  public void setSiteId(final String siteId) {
    m_siteId = siteId;
  }

  @Override
  public void setGameName(final String gameName) {
    m_gameName = gameName;
  }

  @Override
  public void setHost(final String host) {
    m_host = getHostUrlPrefix(host);
  }

  @Override
  public void setAllHosts(final Vector<String> hosts) {
    m_allHosts = hosts;
  }

  @Override
  public void addToAllHosts(final String host) {
    final String hostToAdd = getHostUrlPrefix(host);
    m_allHosts.remove(hostToAdd);
    if (m_allHosts.size() > 10) {
      m_allHosts.subList(10, m_allHosts.size()).clear();
    }
    m_allHosts.add(0, hostToAdd);
  }

  @Override
  public void viewSite() {
    OpenFileUtility.openURL(getHost());
  }

  public void setParties(final String[] parties) {
    this.parties = parties;
  }

  @Override
  public void clearSensitiveInfo() {
    m_allHosts.clear();
  }
}


class ProductionStat extends AbstractStat {
  @Override
  public String getName() {
    return "Production";
  }

  @Override
  public double getValue(final PlayerID player, final GameData data) {
    int rVal = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(place);
      /*
       * Match will Check if terr is a Land Convoy Route and check ownership of neighboring Sea Zone, and also check if
       * territory is
       * contested
       */
      if (ta != null && player != null && player.equals(place.getOwner())
          && Matches.territoryCanCollectIncomeFrom(player, data).match(place)) {
        rVal += ta.getProduction();
      }
    }
    rVal *= Properties.getPU_Multiplier(data);
    return rVal;
  }
}
