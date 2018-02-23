package tools.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import games.strategy.io.IoUtils;
import tools.util.ToolLogger;

public class XmlUpdater {
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";

  /**
   * Utility for updating old game.xml files to the newer format.
   */
  public static void main(final String[] args) throws Exception {
    handleCommandLineArgs(args);
    final File gameXmlFile = new FileOpen("Select xml file", mapFolderLocation, ".xml").getFile();
    if (gameXmlFile == null) {
      ToolLogger.info("No file selected");
      return;
    }
    final InputStream source = XmlUpdater.class.getResourceAsStream("gameupdate.xslt");
    if (source == null) {
      throw new IllegalStateException("Could not find xslt file");
    }
    final Transformer trans = TransformerFactory.newInstance().newTransformer(new StreamSource(source));

    final byte[] resultBytes = IoUtils.writeToMemory(os -> {
      try (InputStream fileInputStream = new FileInputStream(gameXmlFile);
          InputStream gameXmlStream = new BufferedInputStream(fileInputStream)) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        // use a dummy game.dtd, this prevents the xml parser from adding default values
        final URL url = XmlUpdater.class.getResource("");
        final String system = url.toExternalForm();
        final Source xmlSource = new StreamSource(gameXmlStream, system);
        trans.transform(xmlSource, new StreamResult(os));
      } catch (final TransformerException e) {
        throw new IOException(e);
      }
    });
    gameXmlFile.renameTo(new File(gameXmlFile.getAbsolutePath() + ".backup"));
    try (OutputStream outStream = new FileOutputStream(gameXmlFile)) {
      outStream.write(resultBytes);
    }
    ToolLogger.info("Successfully updated:" + gameXmlFile);
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    // arg can only be the map folder location.
    if (args.length == 1) {
      final String value;
      if (args[0].startsWith(TRIPLEA_MAP_FOLDER)) {
        value = getValue(args[0]);
      } else {
        value = args[0];
      }
      final File mapFolder = new File(value);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        ToolLogger.info("Could not find directory: " + value);
      }
    } else if (args.length > 1) {
      ToolLogger.info("Only argument allowed is the map directory.");
    }
    // might be set by -D
    if ((mapFolderLocation == null) || (mapFolderLocation.length() < 1)) {
      final String value = System.getProperty(TRIPLEA_MAP_FOLDER);
      if ((value != null) && (value.length() > 0)) {
        final File mapFolder = new File(value);
        if (mapFolder.exists()) {
          mapFolderLocation = mapFolder;
        } else {
          ToolLogger.info("Could not find directory: " + value);
        }
      }
    }
  }
}
