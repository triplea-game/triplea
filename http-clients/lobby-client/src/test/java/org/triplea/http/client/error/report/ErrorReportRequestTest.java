package org.triplea.http.client.error.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ErrorReportRequestTest {

  /** Validates input/output conversions of game-version -> simplified game version. */
  @ParameterizedTest
  @MethodSource
  void getSimpleGameVersion(String gameVersionInput, String expectedOutput) {
    var errorReportRequest =
        ErrorReportRequest.builder()
            .gameVersion(gameVersionInput)
            .body("body")
            .title("title")
            .build();
    String result = errorReportRequest.getSimpleGameVersion();

    assertThat(result, is(expectedOutput));
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> getSimpleGameVersion() {
    return Stream.of(
        Arguments.of("", ""),
        Arguments.of("0", "0"),
        Arguments.of("10+0", "10"),
        Arguments.of("1.1", "1.1"),
        Arguments.of("2.1.1", "2.1"),
        Arguments.of("2.1.1.1", "2.1"),
        Arguments.of("3.1+1", "3.1"),
        Arguments.of("300.100+1", "300.100"),
        Arguments.of("3.1+1+1", "3.1"),
        Arguments.of("random", "random"));
  }
}
