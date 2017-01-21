package games.strategy.engine.framework.systemcheck;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * This class runs a set of local system checks, like access network, and create a temp file.
 * Each check is always run, and this class records the results of those checks.
 */
public final class LocalSystemChecker {

  private final Set<SystemCheck> systemChecks;

  public LocalSystemChecker() {
    this(ImmutableSet.of(defaultNetworkCheck(), defaultFileSystemCheck()));
  }

  private static SystemCheck defaultNetworkCheck() {
    return new SystemCheck("Can connect to github.com (check network connection)", () -> {
      try {
        final URL url = new URL("http://www.github.com");
        url.openConnection();
      } catch (final Exception e) {
        Throwables.propagate(e);
      }
    });
  }

  private static SystemCheck defaultFileSystemCheck() {
    return new SystemCheck("Can create temporary files (check disk usage, file permissions)", () -> {
      try {
        File.createTempFile("prefix", "suffix");
      } catch (final IOException e) {
        Throwables.propagate(e);
      }
    });
  }


  protected LocalSystemChecker(final Set<SystemCheck> checks) {
    systemChecks = checks;
  }

  /** Return any exceptions encountered while running each check */
  public Set<Exception> getExceptions() {
    final Set<Exception> exceptions = Sets.newHashSet();
    for (final SystemCheck systemCheck : systemChecks) {
      if (systemCheck.getException().isPresent()) {
        exceptions.add(systemCheck.getException().get());
      }
    }
    return exceptions;
  }

  public String getStatusMessage() {
    final StringBuilder sb = new StringBuilder();
    for (final SystemCheck systemCheck : systemChecks) {
      sb.append(systemCheck.getResultMessage()).append("\n");
    }
    return sb.toString();
  }
}
