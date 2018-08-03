package games.strategy.engine.auto.health.check;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import lombok.extern.java.Log;

/**
 * This class runs a set of local system checks, like access network, and create a temp file.
 * Each check is always run, and this class records the results of those checks.
 */
@Log
public final class LocalSystemChecker {

  public static void launch() {
    new Thread(LocalSystemChecker::checkLocalSystem).start();
  }

  private static void checkLocalSystem() {
    final LocalSystemChecker localSystemChecker = new LocalSystemChecker();
    final Collection<Exception> exceptions = localSystemChecker.getExceptions();
    if (!exceptions.isEmpty()) {
      log.warning(String.format(
          "Warning!! %d system checks failed. Some game features may not be available or may not work correctly.%n%s",
          exceptions.size(), localSystemChecker.getStatusMessage()));
    }
  }


  private final Set<SystemCheck> systemChecks;

  private LocalSystemChecker() {
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
        // TODO: check that file is actually created and deleted
        File.createTempFile("prefix", "suffix").delete();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /** Return any exceptions encountered while running each check. */
  public Set<Exception> getExceptions() {
    return systemChecks.stream()
        .map(SystemCheck::getException)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  private String getStatusMessage() {
    return systemChecks.stream()
        .map(SystemCheck::getResultMessage)
        .collect(Collectors.joining("\n"));
  }
}
