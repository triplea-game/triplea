package org.triplea.lobby.server.db;

import java.time.Instant;
import java.util.Optional;

import org.triplea.lobby.server.User;

/**
 * Utility to create/read/delete muted macs (there is no update).
 */
public interface MutedMacDao {
  void addMutedMac(User mutedUser, Instant muteExpires, User moderator);

  Optional<Instant> getMacUnmuteTime(String mac);

  boolean isMacMuted(String mac);
}
