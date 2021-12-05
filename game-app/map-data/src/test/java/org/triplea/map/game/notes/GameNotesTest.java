package org.triplea.map.game.notes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.triplea.io.FileUtils;

class GameNotesTest {

  @Test
  void loadGameNotes() {
    // create notes file
    final Path notesFile = Path.of("game.notes.html");
    FileUtils.writeToFile(notesFile, "<blink>Game notes!</blink>");
    notesFile.toFile().deleteOnExit();

    // create dummy game XML file
    final Path gameXmlPath = Path.of("game.xml");
    FileUtils.writeToFile(gameXmlPath, "dummy content");
    gameXmlPath.toFile().deleteOnExit();

    final String result = GameNotes.loadGameNotes(gameXmlPath);

    assertThat(
        result,
        is(
            "<h1>Game Name</h1>"
                + "Path: "
                + gameXmlPath.toAbsolutePath()
                + "<br>"
                + "<blink>Game notes!</blink>"));
  }

  @Test
  void createExpectedNotesFileName() {
    assertThat(GameNotes.createExpectedNotesFileName(Path.of("game.xml")), is("game.notes.html"));
    assertThat(GameNotes.createExpectedNotesFileName(Path.of("xml.xml")), is("xml.notes.html"));

    assertThrows(
        IllegalArgumentException.class,
        () -> GameNotes.createExpectedNotesFileName(Path.of("notes.html")));
  }

  @Test
  void gameNotesFileExistsForGameXmlFile_NegativeCase() {
    final Path gameXmlPath = Path.of("stand-alone-game-file.xml");
    FileUtils.writeToFile(gameXmlPath, "dummy content");
    gameXmlPath.toFile().deleteOnExit();

    assertThat(GameNotes.gameNotesFileExistsForGameXmlFile(gameXmlPath), is(false));
  }

  @Test
  void gameNotesFileExistsForGameXmlFile_PositiveCase() {
    final Path gameXmlPath = Path.of("game-file-with-notes.xml");
    FileUtils.writeToFile(gameXmlPath, "dummy content");
    gameXmlPath.toFile().deleteOnExit();

    final Path notesFile = Path.of("game-file-with-notes.notes.html");
    FileUtils.writeToFile(notesFile, "dummy content");
    notesFile.toFile().deleteOnExit();

    assertThat(GameNotes.gameNotesFileExistsForGameXmlFile(gameXmlPath), is(true));
  }
}
