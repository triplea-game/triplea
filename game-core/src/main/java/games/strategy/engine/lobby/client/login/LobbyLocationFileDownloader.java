package games.strategy.engine.lobby.client.login;

import java.io.File;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;

/**
 * Humble object pattern. Wraps file downloading.
 */
@FunctionalInterface
@VisibleForTesting
interface LobbyLocationFileDownloader {
  Optional<File> download(final String url);
}
