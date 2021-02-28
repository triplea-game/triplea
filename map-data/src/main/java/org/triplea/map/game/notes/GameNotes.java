package org.triplea.map.game.notes;

import com.google.common.base.Preconditions;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import org.triplea.io.FileUtils;
import org.triplea.java.StringUtils;

@UtilityClass
public class GameNotes {
  /**
   * Given the path to an XML file, reads an expected corresponding game notes and returns the
   * contents of that file. Returns empty if the game notes file does not exist or if there are any
   * errors reading the file.
   *
   * @param xmlGameFile Path to game-xml file whose game notes we will be loading.
   * @param gameName Name of the game, will be embedded into the game notes as a title.
   */
  public static String loadGameNotes(final Path xmlGameFile, final String gameName) {
    Preconditions.checkArgument(
        xmlGameFile.toFile().exists(),
        "Error, expected file did not exist: " + xmlGameFile.toFile().getAbsolutePath());
    Preconditions.checkArgument(
        xmlGameFile.toFile().isFile(),
        "Error, expected file was not a file: " + xmlGameFile.toFile().getAbsolutePath());

    final String notesFileName = createExpectedNotesFileName(xmlGameFile);
    final Path notesFile = xmlGameFile.resolveSibling(notesFileName);

    final String gameNotes =
        notesFile.toFile().exists() ? FileUtils.readContents(notesFile).orElse("") : "";
    return String.format(
        "<h1>%s</h1>Path: %s<br>%s", gameName, xmlGameFile.toFile().getAbsolutePath(), gameNotes);
  }

  /**
   * Given a game-xml-file, returns the expected name of the companion file that should contain the
   * corresponding game (html) notes.
   */
  static String createExpectedNotesFileName(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.toFile().getName().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toFile().getAbsolutePath());
    return StringUtils.truncateEnding(gameXmlFile.toFile().getName(), ".xml") + ".notes.html";
  }

  /** For a given game-xml-file, checks if there is a companion notes html file that exists. */
  static boolean gameNotesFileExistsForGameXmlFile(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.toFile().getName().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toFile().getAbsolutePath());

    final String expectedNotesFile = createExpectedNotesFileName(gameXmlFile);
    return gameXmlFile.resolveSibling(expectedNotesFile).toFile().exists();
  }
}
