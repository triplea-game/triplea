package org.triplea.java;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlStreamsTest {

  private UrlStreams testObj;

  private URL fakeUrl;

  @Mock private URLConnection mockUrlConnection;
  @Mock private InputStream mockInputStream;

  @BeforeEach
  void setUp() throws Exception {
    // set up the test object with a function that will return a mocked url connection
    testObj = new UrlStreams(url -> mockUrlConnection);
    fakeUrl = new URL("http://well-formed-url.com");
  }

  /** Check that we turned off caching on a mocked UrlConnection. */
  @Test
  void cacheIsOff() throws Exception {
    when(mockUrlConnection.getInputStream()).thenReturn(mockInputStream);

    final Optional<InputStream> connection = testObj.newStream(fakeUrl);

    assertThat(
        "expecting the same mocked http connection object back",
        connection.get(),
        sameInstance(mockInputStream));
    verify(mockUrlConnection).setUseCaches(false);
    verify(mockUrlConnection).setDefaultUseCaches(false);
  }

  @Test
  void testErrorSuppressionWhenThereIsNoError() throws Exception {
    when(mockUrlConnection.getInputStream()).thenReturn(mockInputStream);

    final Optional<InputStream> stream = testObj.newStream(fakeUrl);

    assertThat("No issues connecting, we should have an input stream back.", stream, isPresent());
  }

  @Test
  void testErrorSuppression() throws Exception {
    when(mockUrlConnection.getInputStream())
        .thenThrow(new IOException("simulating an IOException being thrown"));

    final Optional<InputStream> stream = testObj.newStream(fakeUrl);

    assertThat(
        "No exceptions expected, but a failure to connect should return an empty object.",
        stream,
        isEmpty());
  }
}
