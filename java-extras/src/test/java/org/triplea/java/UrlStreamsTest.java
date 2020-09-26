package org.triplea.java;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UrlStreamsTest {

  private UrlStreams testObj;

  private URL fakeUrl;

  //  @Mock private URLConnection mockUrlConnection;
  //  @Mock private InputStream mockInputStream;

  @BeforeEach
  void setUp() throws Exception {
    // set up the test object with a function that will return a mocked url connection
    testObj =
        new UrlStreams(
            url ->
                new URLConnection(url) {
                  @Override
                  public void connect() {}
                });

    fakeUrl = new URL("http://well-formed-url.com");
  }

  /** Check that we turned off caching on a mocked UrlConnection. */
  @Test
  void cacheIsOff() {
    final URLConnection connection = testObj.newUrlConnection(fakeUrl);

    assertThat(connection.getUseCaches(), is(false));
    assertThat(connection.getDefaultUseCaches(), is(false));
  }

  @Test
  void testErrorSuppressionWhenThereIsNoError() {
    testObj =
        new UrlStreams(
            url ->
                new URLConnection(url) {
                  @Override
                  public void connect() {}

                  @Override
                  public InputStream getInputStream() {
                    return new ByteArrayInputStream(new byte[0]);
                  }
                });

    final Optional<InputStream> stream = testObj.newStream(fakeUrl);

    assertThat("No issues connecting, we should have an input stream back.", stream, isPresent());
  }

  @Test
  void testErrorSuppression() {
    testObj =
        new UrlStreams(
            url ->
                new URLConnection(url) {
                  @Override
                  public void connect() {}

                  @Override
                  public InputStream getInputStream() throws IOException {
                    throw new IOException("simulating an IOException being thrown");
                  }
                });

    final Optional<InputStream> stream = testObj.newStream(fakeUrl);

    assertThat(
        "No exceptions expected, but a failure to connect should return an empty object.",
        stream,
        isEmpty());
  }
}
