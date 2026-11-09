package games.strategy.engine.posted.game.pbem;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Process-wide handler for a failed forum post in {@link PbemMessagePoster#post}. Logs the error
 * unless a UI registers a handler that shows it to the user.
 */
@Slf4j
public final class ForumPostErrorHandler {
  private static volatile Consumer<Throwable> handler =
      error -> log.error("Error Posting to Forum", error);

  private ForumPostErrorHandler() {}

  public static void setHandler(final Consumer<Throwable> handler) {
    ForumPostErrorHandler.handler = Preconditions.checkNotNull(handler);
  }

  static void handle(final Throwable error) {
    handler.accept(error);
  }
}
