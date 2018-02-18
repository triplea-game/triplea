package games.strategy.net;

import javax.annotation.Nullable;

/**
 * A collection of methods useful for writing tests that use instances of {@link IMessenger}.
 */
public final class MessengerTestUtils {
  private MessengerTestUtils() {}

  /**
   * Shuts down the specified messenger unconditionally. Any exception will be written to stdout.
   *
   * @param messenger The messenger to shut down; if {@code null}, this method does nothing.
   */
  public static void shutDownQuietly(final @Nullable IMessenger messenger) {
    if (messenger != null) {
      try {
        messenger.shutDown();
      } catch (final Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }
}
