package org.triplea.lobby.server.db;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
final class ErrorReportingDaoTest {
  private static final ErrorReportingDao REPORTING_DAO =
      JdbiDatabase.newConnection().onDemand(ErrorReportingDao.class);

  /**
   * Simple check that if we insert a record we'll get a new record in the expected dataset.
   */
  @DataSet("error_reporting/pre-insert.yml")
  @ExpectedDataSet(value = "error_reporting/post-insert.yml")
  @Test
  void insertRow() {
    REPORTING_DAO.insertHistoryRecord("second");
  }

  /**
   * Dates in dataset span from: 2016-01-01 23:59:20.0 to 2016-01-03 23:59:20.0.
   * In this test we'll bisect the various date and verify we select the correct number of records.
   */
  @DataSet("error_reporting/select.yml")
  @Test
  void insertionsSince() {

    Arrays.asList("first", "second").forEach(userIp -> MatcherAssert.assertThat(
        "All records are dated 2016-01-xx, now should have no records",
        REPORTING_DAO.countRecordsByIpSince(userIp, Instant.now()),
        Is.is(0)));

    MatcherAssert.assertThat(
        "All records are dated 2016-01-xx, now should have no records",
        REPORTING_DAO.countRecordsByIpSince("first", Instant.now()),
        Is.is(0));

    final Instant farPast = LocalDateTime.of(2000, 1, 1, 0, 0)
        .toInstant(ZoneOffset.UTC);

    MatcherAssert.assertThat(
        "There is one record with 'second', we'll search for a date in far past",
        REPORTING_DAO.countRecordsByIpSince("second", farPast),
        Is.is(1));

    MatcherAssert.assertThat(
        "should select only last record of first userIp",
        REPORTING_DAO.countRecordsByIpSince(
            "first",
            LocalDateTime.of(2016, 1, 3, 23, 0, 0)
                .toInstant(ZoneOffset.UTC)),
        Is.is(1));

    MatcherAssert.assertThat(
        "should select last two records of first userIp",
        REPORTING_DAO.countRecordsByIpSince(
            "first",
            LocalDateTime.of(2016, 1, 2, 23, 0, 0)
                .toInstant(ZoneOffset.UTC)),
        Is.is(2));

    MatcherAssert.assertThat(
        "should select all three records of first userIp",
        REPORTING_DAO.countRecordsByIpSince(
            "first",
            LocalDateTime.of(2016, 1, 1, 23, 0, 0)
                .toInstant(ZoneOffset.UTC)),
        Is.is(3));
  }

  @DataSet("error_reporting/pre-purge.yml")
  @ExpectedDataSet(value = "error_reporting/post-purge.yml")
  @Test
  void purgeOld() {
    REPORTING_DAO.purgeOld(
        LocalDateTime.of(2016, 1, 3, 23, 0, 0)
            .toInstant(ZoneOffset.UTC));
  }
}
