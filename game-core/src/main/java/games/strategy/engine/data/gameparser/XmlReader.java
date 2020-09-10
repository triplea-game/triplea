package games.strategy.engine.data.gameparser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Utility class to read an XML file. */
public final class XmlReader {

  public static final String DTD_FILE_NAME = "game.dtd";

  private XmlReader() {}

  static Element parseDom(
      final String mapName, final InputStream stream, final Collection<SAXParseException> errorsSax)
      throws GameParseException {
    try {
      return getDocument(mapName, stream, errorsSax).getDocumentElement();
    } catch (final SAXException | IOException | ParserConfigurationException e) {
      throw new GameParseException("failed to parse XML document", e);
    }
  }

  private static Document getDocument(
      final String mapName, final InputStream input, final Collection<SAXParseException> errorsSax)
      throws IOException, SAXException, ParserConfigurationException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(true);
    // Not mandatory, but better than relying on the default implementation to prevent XXE
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "file");
    // get the dtd location
    final String dtdFile = "/games/strategy/engine/xml/" + DTD_FILE_NAME;
    final URL url = GameParser.class.getResource(dtdFile);
    if (url == null) {
      throw new RuntimeException(
          String.format("Map: %s, Could not find in classpath %s", mapName, dtdFile));
    }
    final DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setErrorHandler(
        new ErrorHandler() {
          @Override
          public void fatalError(final SAXParseException exception) {
            errorsSax.add(exception);
          }

          @Override
          public void error(final SAXParseException exception) {
            errorsSax.add(exception);
          }

          @Override
          public void warning(final SAXParseException exception) {
            errorsSax.add(exception);
          }
        });
    final String dtdSystem = url.toExternalForm();
    final String system = dtdSystem.substring(0, dtdSystem.length() - DTD_FILE_NAME.length());
    return builder.parse(input, system);
  }
}
