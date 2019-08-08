package org.triplea.lobby.server.db;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import org.triplea.lobby.server.User;

/** Data access object for the banned MAC table. */
public interface BannedMacDao {
  /**
   * Adds the specified banned MAC to the table if it does not exist or updates the instant at which
   * the ban will expire if it already exists.
   *
   * @param bannedUser The user whose MAC will be banned.
   * @param banTill The instant at which the ban will expire or {@code null} to ban the MAC forever.
   * @param moderator The moderator executing the ban.
   * @throws IllegalStateException If an error occurs while adding, updating, or removing the ban.
   */
  void addBannedMac(User bannedUser, @Nullable Instant banTill, User moderator);

  /**
   * Indicates if the specified MAC is banned relative to the checkTime provided.
   *
   * @param ipAddress IP of the user to query for ban.
   * @param mac The MAC to query for a ban.
   * @return If MAC or IP was banned, returns the timestamp of the ban expiration.
   */
  Optional<Timestamp> isMacBanned(InetAddress ipAddress, String mac);
}
