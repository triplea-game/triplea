package org.triplea.map.game.notes;

import com.google.common.base.Preconditions;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import org.triplea.io.FileUtils;
import org.triplea.java.StringUtils;

@UtilityClass
public class GameNotes {
  /**
   * Given the path to an XML file, reads an expected corresponding game notes and returns the
   * contents of that file. Returns an empty String if the game notes file does not exist or if
   * there are any errors reading the file.
   *
   * @param xmlGameFile Path to game-xml file whose game notes we will be loading.
   */
  public static String loadGameNotes(final Path xmlGameFile) {
    Preconditions.checkArgument(
        Files.exists(xmlGameFile),
        "Error, expected file did not exist: " + xmlGameFile.toAbsolutePath());
    Preconditions.checkArgument(
        !Files.isDirectory(xmlGameFile),
        "Error, expected file was not a file: " + xmlGameFile.toAbsolutePath());

    final String notesFileName = createExpectedNotesFileName(xmlGameFile);
    final Path notesFile = xmlGameFile.resolveSibling(notesFileName);

    return Files.exists(notesFile) ? FileUtils.readContents(notesFile).orElse("") : "";
  }

  /**
   * Given a game-xml-file, returns the expected name of the companion file that should contain the
   * corresponding game (html) notes.
   */
  static String createExpectedNotesFileName(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.getFileName().toString().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toAbsolutePath());
    return StringUtils.truncateEnding(gameXmlFile.getFileName().toString(), ".xml") + ".notes.html";
  }

  /** For a given game-xml-file, checks if there is a companion notes html file that exists. */
  static boolean gameNotesFileExistsForGameXmlFile(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.getFileName().toString().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toAbsolutePath());

    final String expectedNotesFile = createExpectedNotesFileName(gameXmlFile);
    return Files.exists(gameXmlFile.resolveSibling(expectedNotesFile));
  }
}
