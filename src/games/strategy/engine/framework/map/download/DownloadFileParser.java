package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import games.strategy.util.Version;

/** Utility class to parse an available map list file config file - used to determine which maps are available for download */
public final class DownloadFileParser {

  private DownloadFileParser() {}

  public static enum Tags {
    mapType, version, mapName, game, description
  }

  public static enum ValueType {
    MAP, MAP_TOOL, MAP_SKIN, MAP_MOD
  }

  public static List<DownloadFileDescription> parse(final InputStream is) {
    final List<DownloadFileDescription> rVal = new ArrayList<DownloadFileDescription>();
    try {
      final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      parser.parse(new InputSource(is), new DefaultHandler() {
        private StringBuilder content = new StringBuilder();
        private String url;
        private String description;
        private String mapName;
        private Version version;
        private DownloadFileDescription.DownloadType downloadType;

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
          content.append(ch, start, length);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
          final String elementName = qName;


          if (elementName.equals(Tags.description.toString())) {
            description = content.toString().trim();
          } else if (elementName.equals("url")) {
            url = content.toString().trim();
          } else if (elementName.equals(Tags.mapName.toString())) {
            mapName = content.toString().trim();
          } else if (elementName.equals(Tags.mapType.toString())) {
            downloadType = DownloadFileDescription.DownloadType.valueOf(content.toString().trim());
          } else if (elementName.equals(Tags.version.toString())) {
            this.version = new Version(content.toString().trim());
          } else if (elementName.equals(Tags.game.toString())) {
            if (downloadType == null) {
              downloadType = DownloadFileDescription.DownloadType.MAP;
            }
            DownloadFileDescription downloadFileDescription =
                new DownloadFileDescription(url, description, mapName, version, downloadType);
            rVal.add(downloadFileDescription);
            // clear optional properties
            version = null;
            downloadType = null;
          } else if (!elementName.equals("games")) {
            throw new IllegalStateException("unexpected tag:" + elementName);
          }
          content = new StringBuilder();
        }
      });
    } catch (final SAXParseException e) {
      e.printStackTrace();
      throw new IllegalStateException("Could not parse xml error at line:" + e.getLineNumber() + " column:"
          + e.getColumnNumber() + " error:" + e.getMessage());
    } catch (final Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
    validate(rVal);
    return rVal;
  }

  private static void validate(final List<DownloadFileDescription> downloads) {
    final Set<String> urls = new HashSet<String>();
    final Set<String> names = new HashSet<String>();


    for (DownloadFileDescription download : downloads) {
      checkState(!download.getUrl().isEmpty());
      if (!download.isDummyUrl()) {
        checkState(!download.getDescription().isEmpty());
        checkState(!download.getMapName().isEmpty());
        checkState(!download.getUrl().isEmpty());

        checkState(names.add(download.getMapName()), "duplicate mapName:" + download.getMapName());
        checkState(urls.add(download.getUrl()), "duplicate url:" + download.getUrl());
        // verify we can parse a URL
        download.newURL();
      }
    }
  }
}
