package org.triplea.lobby.server.db;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Utility to create/read/delete muted macs (there is no update).
 */
public interface MutedMacDao {
  void addMutedMac(
      InetAddress netAddress,
      String hashedMac,
      @Nullable Instant muteTill,
      String moderatorName);

  Optional<Instant> getMacUnmuteTime(String mac);

  boolean isMacMuted(Instant nowTime, String mac);
}
