package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;

class TestDataFileReaderTest {
  @NonNls private static final String SAMPLE_FILE_PATH = "example/example_file.txt";
  private static final String EXPECTED_CONTENT =
      "A file with some example text"
          + System.lineSeparator()
          + "and a second line"
          + System.lineSeparator();

  @Test
  void fileNotFoundCase() {
    assertThrows(
        TestDataFileReader.TestDataFileNotFound.class,
        () -> TestDataFileReader.readContents("DNE"));
  }

  @Test
  void readSampleFileFromResources() {
    final String content = TestDataFileReader.readContents(SAMPLE_FILE_PATH);

    assertThat(content, is(EXPECTED_CONTENT));
  }

  @Test
  void readSampleFileFromProjectRoot() {
    final String content = TestDataFileReader.readContents("LICENSE");

    assertThat(content, notNullValue());
  }
}
