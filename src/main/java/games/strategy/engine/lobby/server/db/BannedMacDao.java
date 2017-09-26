package games.strategy.engine.lobby.server.db;

import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nullable;

import games.strategy.util.Tuple;

/**
 * Data access object for the banned MAC table.
 */
public interface BannedMacDao {
  /**
   * Adds the specified banned MAC to the table if it does not exist or updates the instant at which the ban will expire
   * if it already exists.
   *
   * @param mac The MAC to ban.
   * @param banTill The instant at which the ban will expire or {@ode null} to ban the MAC forever.
   */
  void addBannedMac(String mac, @Nullable Instant banTill);

  /**
   * Indicates the specified MAC is banned.
   *
   * @param mac The MAC to query for a ban.
   *
   * @return A tuple whose first element indicates if the MAC is banned or not. If the MAC is banned, the second element
   *         is the instant at which the ban will expire or {@code null} if the MAC is banned forever.
   */
  Tuple<Boolean, /* @Nullable */ Timestamp> isMacBanned(String mac);
}
