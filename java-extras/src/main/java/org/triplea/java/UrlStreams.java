package org.triplea.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import lombok.extern.java.Log;

/** Utility class for opening input streams from URL and URI objects. */
@Log
public final class UrlStreams {
  /** Used to obtain a connection from a given URL. */
  private final Function<URL, URLConnection> urlConnectionFactory;

  protected UrlStreams() {
    // By default just try open a connection, raise any exceptions encountered
    this.urlConnectionFactory =
        (url) -> {
          try {
            return url.openConnection();
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        };
  }

  /** For test, a constructor that allows mock object injection. */
  protected UrlStreams(final Function<URL, URLConnection> connectionFactory) {
    this.urlConnectionFactory = connectionFactory;
  }

  /**
   * Opens an input stream to a given url. Returns Optional.empty() in case there is a failure. The
   * failure message is logged to the user.
   *
   * @return Optional.empty() if there was a failure opening the stream, otherwise an optional
   *     containing an input stream to the parameter uri.
   */
  public static Optional<InputStream> openStream(final URL url) {
    return new UrlStreams().newStream(url);
  }

  /**
   * Opens an input stream to a given uri.
   *
   * @return Optional.empty() if there was a failure opening the stream, otherwise an optional
   *     containing an input stream to the parameter uri.
   * @throws IllegalStateException if the given uri is malformed
   */
  public static Optional<InputStream> openStream(final URI uri) {
    try {
      return UrlStreams.openStream(uri.toURL());
    } catch (final MalformedURLException e) {
      throw new IllegalStateException("Bad uri specified: " + uri, e);
    }
  }

  /**
   * Opens a stream, runs the provided function giving it as input the stream that was opened and
   * then closes the stream returning any value from the function. If the stream cannot be opened or
   * the function returns a null, then a 'Optional.empty' is returned.
   */
  public static <T> Optional<T> openStream(
      final URI uri, final Function<InputStream, T> streamOperation) {
    final Optional<InputStream> stream = openStream(uri);
    if (stream.isPresent()) {
      try (InputStream inputStream = stream.get()) {
        return Optional.ofNullable(streamOperation.apply(inputStream));
      } catch (final IOException e) {
        log.log(Level.SEVERE, "Unable to open: " + uri + ", " + e.getMessage(), e);
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  protected Optional<InputStream> newStream(final URL url) {
    try {
      final URLConnection connection = urlConnectionFactory.apply(url);

      // Turn off URL connection caching to avoid open file leaks. When caching is on, the
      // InputStream returned is left open, even after you call 'InputStream.close()'
      connection.setDefaultUseCaches(
          false); // TODO: verify - setDefaultUseCaches(false) may not be necessary
      connection.setUseCaches(false);
      return Optional.of(connection.getInputStream());
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Unable to open: " + url + ", " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
