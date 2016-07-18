package games.strategy.engine.framework.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.util.UrlStreams;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.triplea.Constants;

public class NewGameChooserEntry {
  private final URI m_url;
  private GameData m_data;
  private boolean m_gameDataFullyLoaded = false;
  private final String m_gameNameAndMapNameProperty;


  public static Comparator<NewGameChooserEntry> getComparator() {
    return new Comparator<NewGameChooserEntry>() {
      @Override
      public int compare(final NewGameChooserEntry o1, final NewGameChooserEntry o2) {
        return getLowerCaseComparable(o1).compareTo(getLowerCaseComparable(o2));
      }
      private String getLowerCaseComparable(NewGameChooserEntry newGameChooserEntry) {
        return newGameChooserEntry.getGameData().getGameName().toLowerCase();
      }
    };
  }


  public NewGameChooserEntry(final URI uri)
      throws IOException, GameParseException, SAXException, EngineVersionException {
    m_url = uri;
    final AtomicReference<String> gameName = new AtomicReference<>();

    Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if(!inputStream.isPresent()) {
      throw new IOException("Failed to open an input stream to: " + uri);
    }

    try (InputStream input = inputStream.get()) {
      final boolean delayParsing = GameRunner2.getDelayedParsing();
      m_data = new GameParser().parse(input, gameName, delayParsing);
      m_gameDataFullyLoaded = !delayParsing;
      m_gameNameAndMapNameProperty = getGameName() + ":" + getMapNameProperty();
    }
  }

  public void fullyParseGameData() throws GameParseException {
    // TODO: We should be setting this in the the constructor. At this point, you have to call methods in the
    // correct order for things to work, and that is bads.
    m_data = null;

    final AtomicReference<String> gameName = new AtomicReference<>();

    Optional<InputStream> inputStream = UrlStreams.openStream(m_url);
    if(!inputStream.isPresent()) {
      return;
    }

    try (InputStream input = inputStream.get()) {
      m_data = new GameParser().parse(input, gameName, false);
      m_gameDataFullyLoaded = true;

    } catch (final EngineVersionException e) {
      ClientLogger.logQuietly(e);
      throw new GameParseException(m_url.toString(), e.getMessage());
    } catch (final SAXParseException e) {
      String msg = "Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber();
      ClientLogger.logError(msg, e);
      throw new GameParseException(m_url.toString(), e.getMessage());
    } catch (final Exception e) {
      String msg = "Could not parse:" + m_url;
      ClientLogger.logError(msg, e);
      throw new GameParseException(m_url.toString(), e.getMessage());
    }
  }

  /**
   * Do not use this if possible. Instead try to remove the bad map from the GameChooserModel.
   * If that fails, then do a short parse so the user doesn't get a null pointer error.
   */
  public void delayParseGameData() {
    m_data = null;

    final AtomicReference<String> gameName = new AtomicReference<>();
    Optional<InputStream> inputStream = UrlStreams.openStream(m_url);
    if(!inputStream.isPresent()) {
      return;
    }
    try (InputStream input = inputStream.get()) {
      m_data = new GameParser().parse(input, gameName, true);
      m_gameDataFullyLoaded = false;
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final SAXParseException e) {
      System.err.println(
          "Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
      ClientLogger.logQuietly(e);
    } catch (final Exception e) {
      System.err.println("Could not parse:" + m_url);
      ClientLogger.logQuietly(e);
    }
  }

  public boolean isGameDataLoaded() {
    return m_gameDataFullyLoaded;
  }

  public String getGameName() {
    return m_data.getGameName();
  }

  // the user may have selected a map skin instead of this map folder, so don't use this for anything except our
  // equals/hashcode below
  private String getMapNameProperty() {
    final String mapName = (String) m_data.getProperties().get(Constants.MAP_NAME);
    if (mapName == null || mapName.trim().length() == 0) {
      throw new IllegalStateException("Map name property not set on game");
    }
    return mapName;
  }

  public String getGameNameAndMapNameProperty() {
    return m_gameNameAndMapNameProperty;
  }

  @Override
  public String toString() {
    return getGameName();
  }

  public GameData getGameData() {
    return m_data;
  }

  public URI getURI() {
    return m_url;
  }

  public String getLocation() {
    final String raw = m_url.toString();
    final String base = ClientFileSystemHelper.getRootFolder().toURI().toString() + "maps";
    if (raw.startsWith(base)) {
      return raw.substring(base.length());
    }
    if (raw.startsWith("jar:" + base)) {
      return raw.substring("jar:".length() + base.length());
    }
    return raw;
  }

  @Override
  public int hashCode() {
    return getGameNameAndMapNameProperty().hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NewGameChooserEntry other = (NewGameChooserEntry) obj;
    if (m_data == null) {
      if (other.m_data != null) {
        return false;
      }
    } else {
      if (other.m_data == null) {
        return false;
      }
    }
    return this.getGameNameAndMapNameProperty().equals(other.getGameNameAndMapNameProperty());
  }
}
