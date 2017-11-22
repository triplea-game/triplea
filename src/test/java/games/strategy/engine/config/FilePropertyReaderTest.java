package games.strategy.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.junit.experimental.extensions.TemporaryFolder;
import org.junit.experimental.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TemporaryFolderExtension.class)
public final class FilePropertyReaderTest {
  private FilePropertyReader filePropertyReader;
  private TemporaryFolder temporaryFolder;

  @BeforeEach
  public void setup() throws Exception {
    final File file = temporaryFolder.newFile(getClass().getName());
    try (Writer writer = new FileWriter(file)) {
      writer.write("a=b\n");
      writer.write(" 1 = 2 \n");
      writer.write("whitespace =      \n");
    }

    filePropertyReader = new FilePropertyReader(file);
  }

  @Test
  public void constructorWithPath_ShouldThrowExceptionWhenFileDoesNotExist() {
    assertThrows(IllegalArgumentException.class, () -> new FilePropertyReader("path/to/nonexistent/file"));
  }

  @Test
  public void constructorWithFile_ShouldThrowExceptionWhenFileDoesNotExist() {
    assertThrows(IllegalArgumentException.class, () -> new FilePropertyReader(new File("path/to/nonexistent/file")));
  }

  @Test
  public void checkPropertyParsing() {
    assertThat("basic happy case, we wrote 'a=b' in the props file",
        filePropertyReader.readProperty("a"), is("b"));
    assertThat("we are also checking string trimming here",
        filePropertyReader.readProperty("1"), is("2"));
  }

  @Test
  public void checkPropertyNotFound() {
    assertThat("not found is empty value back, same thing as if we did not set the value",
        filePropertyReader.readProperty("notFound"), is(""));

    assertThat("verify trimming, will look like no property found with only whitespace set",
        filePropertyReader.readProperty("whitespace"), is(""));
  }
}
