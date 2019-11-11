package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.test.common.StringToInputStream.asInputStream;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class StringToInputStreamTest {

  @Test
  void nullInput() throws Exception {
    final InputStream inputStream = asInputStream(null);
    assertThat(inputStream.read(), is(-1));
  }

  @Test
  void emptyInput() throws Exception {
    final InputStream inputStream = asInputStream("");
    assertThat(inputStream.read(), is(-1));
  }

  @Test
  void exampleInput() {
    final InputStream inputStream = asInputStream("example input");

    final Scanner s = new Scanner(inputStream, StandardCharsets.UTF_8);
    assertThat(s.nextLine(), is("example input"));
  }
}
