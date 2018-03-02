package games.strategy.engine.auto.health.check;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

/**
 * This class runs a set of local system checks, like access network, and create a temp file.
 * Each check is always run, and this class records the results of those checks.
 */
public final class LocalSystemChecker {

  private final Set<SystemCheck> systemChecks;

  public LocalSystemChecker() {
    this(ImmutableSet.of(defaultNetworkCheck(), defaultFileSystemCheck()));
  }

  @VisibleForTesting
  LocalSystemChecker(final Set<SystemCheck> checks) {
    systemChecks = checks;
  }

  private static SystemCheck defaultNetworkCheck() {
    return new SystemCheck("Can connect to github.com (check network connection)", () -> {
      try {
        final int connectTimeoutInMilliseconds = 20000;
        final URL url = new URL("https://github.com");
        final URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(connectTimeoutInMilliseconds);
        urlConnection.connect();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static SystemCheck defaultFileSystemCheck() {
    return new SystemCheck("Can create temporary files (check disk usage, file permissions)", () -> {
      try {
        File.createTempFile("prefix", "suffix").delete();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /** Return any exceptions encountered while running each check. */
  public Set<Exception> getExceptions() {
    return systemChecks.stream()
        .filter(systemCheck -> systemCheck.getException().isPresent())
        .map(systemCheck -> systemCheck.getException().get())
        .collect(Collectors.toSet());
  }

  public String getStatusMessage() {
    return systemChecks.stream()
        .map(systemCheck -> systemCheck.getResultMessage() + "\n")
        .collect(Collectors.joining());
  }
}
