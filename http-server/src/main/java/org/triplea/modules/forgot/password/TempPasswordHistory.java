package org.triplea.modules.forgot.password;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import org.triplea.db.dao.TempPasswordHistoryDao;

/**
 * Class for accessing temp password history. The history is used for audit and rate limiting so a
 * user cannot create unlimited temp passwords requests.
 */
@AllArgsConstructor
public class TempPasswordHistory {
  @VisibleForTesting static final int MAX_TEMP_PASSWORD_REQUESTS_PER_DAY = 3;

  private final TempPasswordHistoryDao tempPasswordHistoryDao;

  boolean allowRequestFromAddress(final String address) {
    return tempPasswordHistoryDao.countRequestsFromAddress(address)
        < MAX_TEMP_PASSWORD_REQUESTS_PER_DAY;
  }

  public void recordTempPasswordRequest(final String address, final String username) {
    tempPasswordHistoryDao.recordTempPasswordRequest(address, username);
  }
}
