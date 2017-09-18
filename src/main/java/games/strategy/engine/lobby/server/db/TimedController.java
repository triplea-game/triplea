package games.strategy.engine.lobby.server.db;

import java.time.Instant;

/**
 * This class allows integration Tests to override the "now time".
 * This enables us to make Tests execution-time-independent which is a good thing.
 */
abstract class TimedController {
  Instant now() {
    return Instant.now();
  }
}
