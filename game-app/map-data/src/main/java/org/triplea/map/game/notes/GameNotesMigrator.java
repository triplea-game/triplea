package org.triplea.map.game.notes;

import java.nio.file.Path;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.io.FileUtils;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.PropertyList;

/**
 * Migrates game notes from XML file to be in a standalone file. In v2.5 and before, game notes were
 * found within a game file XML. In 2.6 and later game notes are in their own file. This migrator
 * moves notes, as needed, from XML to a stand-alone file.
 */
@Builder
@Slf4j
public class GameNotesMigrator {
  public static void extractGameNotes(Path xmlGameFile) {
    if (GameNotes.gameNotesFileExistsForGameXmlFile(xmlGameFile)) {
      return;
    }
    final String gameNotes = readGameNotesFromXml(xmlGameFile);
    final String fileNameToWrite = GameNotes.createExpectedNotesFileName(xmlGameFile);
    final Path fileToWrite = xmlGameFile.resolveSibling(fileNameToWrite);
    log.info("Writing game notes file: " + fileToWrite.toAbsolutePath());
    FileUtils.writeToFile(fileToWrite, gameNotes);
  }

  private static String readGameNotesFromXml(final Path xmlGameFile) {
    final Game game =
        FileUtils.openInputStream(
            xmlGameFile,
            input -> {
              try {
                return new XmlMapper(input).mapXmlToObject(Game.class);
              } catch (final XmlParsingException e) {
                log.warn(
                    "Unable to parse file (consider deleting this file or relocating it outside "
                        + "of the games folder): {}, error: {}",
                    xmlGameFile.toAbsolutePath(),
                    e.getMessage());
                return null;
              }
            });

    if (game == null) {
      return "";
    }

    // get 'value' attribute of 'notes' property
    return getGameNotesProperty(game)
        .map(PropertyList.Property::getValue)
        // otherwise look for 'value' child node of 'notes' property
        .or(
            () ->
                getGameNotesProperty(game)
                    .map(PropertyList.Property::getValueProperty)
                    .map(PropertyList.Property.Value::getData))
        .orElse("");
  }

  private static Optional<PropertyList.Property> getGameNotesProperty(final Game game) {
    return game.getPropertyList().getProperties().stream()
        .filter(property -> property.getName() != null)
        .filter(property -> property.getName().equalsIgnoreCase("notes"))
        .findAny();
  }
}
