package org.triplea.lobby.server.db;

import java.time.Instant;
import java.util.Optional;

import org.triplea.lobby.server.User;

/**
 * Utility to create/read/delete muted usernames (there is no update).
 */
public interface MutedUsernameDao {
  void addMutedUsername(User mutedUser, Instant muteExpires, User moderator);

  Optional<Instant> getUsernameUnmuteTime(String username);

  boolean isUsernameMuted(String username);
}
