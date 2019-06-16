package org.triplea.server.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;

/**
 * In this test we set up some mock objects to return a report
 * count since yesterday that we will compare to a configurable
 * max value. If at max or over, then we expect 'false', meaning
 * an additional report is not allowed.
 */
@ExtendWith(MockitoExtension.class)
class ErrorReportGateKeeperTest {
  private static final int MAX_REPORTS_PER_DAY = 3;
  private static final Instant INSTANT = Instant.now();
  private static final String USER_IP = "user-ip-value";

  @Mock
  private Clock clock;

  @Mock
  private ErrorReportingDao dao;

  private ErrorReportGateKeeper errorReportGateKeeper;

  @BeforeEach
  void setup() {
    errorReportGateKeeper = ErrorReportGateKeeper.builder()
        .clock(clock)
        .dao(dao)
        .maxReportsPerDay(MAX_REPORTS_PER_DAY)
        .build();
  }

  @Test
  void allowedIfUnderMax() {
    givenErrorReportCount(MAX_REPORTS_PER_DAY - 1);
    assertThat(
        errorReportGateKeeper.test(USER_IP),
        is(true));
  }

  private void givenErrorReportCount(final int count) {
    when(clock.instant()).thenReturn(INSTANT);

    final Instant oneDayAgo = INSTANT.minus(1, ChronoUnit.DAYS);

    when(dao.countRecordsByIpSince(USER_IP, oneDayAgo)).thenReturn(count);
  }

  @Test
  void notAllowedIfAtMax() {
    givenErrorReportCount(MAX_REPORTS_PER_DAY);
    assertThat(
        errorReportGateKeeper.test(USER_IP),
        is(false));
  }

  @Test
  void notAllowedIfExceedingMax() {
    givenErrorReportCount(MAX_REPORTS_PER_DAY + 1);
    assertThat(
        errorReportGateKeeper.test(USER_IP),
        is(false));
  }

}
