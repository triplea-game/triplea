package org.triplea.map.game.notes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

  /** Callback to be invoked if we start generating YAML files. */
  private final Consumer<Runnable> progressIndicator;

  /** Path to where downloaded maps can be found. */
  private final Path downloadedMapsFolder;

  public void extractGameNotes() {
    final Collection<Path> xmlFilesMissingGameNotesFile =
        findAllGameXmlFiles(downloadedMapsFolder).stream()
            .filter(Predicate.not(GameNotes::gameNotesFileExistsForGameXmlFile))
            .collect(Collectors.toSet());

    // For each game XML missing a game-notes file, generate the game-notes file.
    // If we have a number of game notes files to create, run in a progress dialog.

    final Runnable migrationJob =
        () ->
            xmlFilesMissingGameNotesFile.forEach(
                xmlGameFile -> {
                  final String gameNotes = readGameNotesFromXml(xmlGameFile);
                  final String fileNameToWrite = GameNotes.createExpectedNotesFileName(xmlGameFile);
                  final Path fileToWrite = xmlGameFile.resolveSibling(fileNameToWrite);
                  log.info("Writing game notes file: " + fileToWrite.toFile().getAbsolutePath());
                  FileUtils.writeToFile(fileToWrite, gameNotes);
                });

    if (xmlFilesMissingGameNotesFile.size() > 3) {
      progressIndicator.accept(migrationJob);
    } else {
      migrationJob.run();
    }
  }

  private Collection<Path> findAllGameXmlFiles(final Path rootPath) {
    final Collection<Path> xmlFiles = new ArrayList<>();

    for (final Path child : FileUtils.listFiles(rootPath)) {
      if (Files.isDirectory(child) && child.getFileName().toString().equalsIgnoreCase("games")) {
        xmlFiles.addAll(FileUtils.findXmlFiles(child, 1));
      } else if (Files.isDirectory(child)) {
        xmlFiles.addAll(findAllGameXmlFiles(child));
      }
    }
    return xmlFiles;
  }

  private String readGameNotesFromXml(final Path xmlGameFile) {
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

  private Optional<PropertyList.Property> getGameNotesProperty(final Game game) {
    return game.getPropertyList().getProperties().stream()
        .filter(property -> property.getName() != null)
        .filter(property -> property.getName().equalsIgnoreCase("notes"))
        .findAny();
  }
}
