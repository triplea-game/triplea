package games.strategy.engine.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Test;

public class PropertyFileReaderTest {

  private PropertyFileReader testObj;

  /**
   * Sets up a test object with a throw-away temp file with some basic
   * test data written to it. Subsequent tests will verify parsing
   * based on this data.
   */
  @Before
  public void setup() throws Exception {
    final File tempFile = File.createTempFile("test", "tmp");
    tempFile.deleteOnExit();

    final FileWriter writer = new FileWriter(tempFile);

    writer.write("a=b\n");
    writer.write(" 1 = 2 \n");
    writer.write("whitespace =      \n");

    writer.close();

    testObj = new PropertyFileReader(tempFile);
  }


  @Test
  public void checkPropertyParsing() {
    assertThat("basic happy case, we wrote 'a=b' in the props file",
        testObj.readProperty("a"), is("b"));
    assertThat("we are also checking string trimming here",
        testObj.readProperty("1"), is("2"));
  }

  @Test
  public void checkPropertyNotFound() {
    assertThat("not found is empty value back, same thing as if we did not set the value",
        testObj.readProperty("notFound"), is(""));

    assertThat("verify trimming, will look like no property found with only whitespace set",
        testObj.readProperty("whitespace"), is(""));
  }


  @Test(expected = NullPointerException.class)
  public void throwsNullPointerOnNullInput() {
    testObj.readProperty(null);
  }

  /**
   * Empty input parameter would be a mistake from the client, throw an error rather than continuing.
   */
  @Test(expected = IllegalArgumentException.class)
  public void throwIfInputIsEmpty() {
    testObj.readProperty("  ");
  }

}
