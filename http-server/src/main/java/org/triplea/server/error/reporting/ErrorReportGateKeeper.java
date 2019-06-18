package org.triplea.server.error.reporting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

import org.triplea.lobby.server.db.dao.ErrorReportingDao;

import lombok.Builder;
import lombok.NonNull;

/**
 * An object to check if we should accept an error report request from our users and use that
 * request to create a bug tracking ticket. A reason to deny is if rate limit has been exceeded.
 */
@Builder
public class ErrorReportGateKeeper implements Predicate<String> {

  @NonNull
  private final ErrorReportingDao dao;
  @NonNull
  private final Clock clock;
  @NonNull
  private final Integer maxReportsPerDay;

  /**
   * True if we should allow the error request. False indicates
   * the user has submitted too many error reports.
   */
  @Override
  public boolean test(final String clientIp) {
    checkNotNull(clientIp);
    checkArgument(!clientIp.isEmpty());

    final Instant oneDayAgo = clock.instant().minus(1, ChronoUnit.DAYS);

    return dao.countRecordsByIpSince(clientIp, oneDayAgo) < maxReportsPerDay;
  }
}
