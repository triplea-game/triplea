package games.strategy.net;

import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/** A collection of methods useful for writing tests that use instances of {@link IMessenger}. */
@Slf4j
public final class MessengerTestUtils {
  private MessengerTestUtils() {}

  /**
   * Shuts down the specified messenger unconditionally. Any exception will be logged.
   *
   * @param messenger The messenger to shut down; if {@code null}, this method does nothing.
   */
  public static void shutDownQuietly(final @Nullable IMessenger messenger) {
    if (messenger != null) {
      try {
        messenger.shutDown();
      } catch (final Exception e) {
        log.warn("Failed to shut down messenger", e);
      }
    }
  }
}
