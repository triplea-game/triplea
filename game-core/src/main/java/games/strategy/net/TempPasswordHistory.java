package games.strategy.net;

import java.net.InetAddress;

/**
 * Interface for accessing temp password history. The history is used for audit and rate limiting so
 * a user cannot create unlimited temp passwords requests.
 */
public interface TempPasswordHistory {
  int countRequestsFromAddress(InetAddress address);

  void recordTempPasswordRequest(InetAddress address, String username);
}
