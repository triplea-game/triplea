package org.triplea.db.dao.error.reporting;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.db.dao.DaoTest;

final class ErrorReportingDaoTest extends DaoTest {
  private final ErrorReportingDao errorReportingDao = DaoTest.newDao(ErrorReportingDao.class);

  /** Simple check that if we insert a record we'll get a new record in the expected dataset. */
  @DataSet(cleanBefore = true, value = "error_reporting/pre-insert.yml")
  @ExpectedDataSet(value = "error_reporting/post-insert.yml")
  @Test
  void insertRow() {
    errorReportingDao.insertHistoryRecord(
        InsertHistoryRecordParams.builder()
            .ip("the_userIp")
            .systemId("the_systemId")
            .title("the_reportTitle")
            .gameVersion("the_gameVersion")
            .githubIssueLink("the_createdIssueLink")
            .build());
  }

  @DataSet(cleanBefore = true, value = "error_reporting/pre-purge.yml")
  @ExpectedDataSet(value = "error_reporting/post-purge.yml")
  @Test
  void purgeOld() {
    errorReportingDao.purgeOld(LocalDateTime.of(2016, 1, 3, 23, 0, 0).toInstant(ZoneOffset.UTC));
  }

  @DataSet(cleanBefore = true, value = "error_reporting/post-purge.yml")
  @Test
  void getErrorReportLinkFoundCase() {
    assertThat(
        errorReportingDao.getErrorReportLink("the_reportTitle2", "the_gameVersion2"),
        isPresentAndIs("the_createdIssueLink2"));
  }

  @DataSet(cleanBefore = true, value = "error_reporting/post-purge.yml")
  @ParameterizedTest
  @MethodSource
  void getErrorReportLinkNotFoundCases(final String title, final String version) {
    assertThat(
        errorReportingDao.getErrorReportLink(title, version), //
        OptionalMatchers.isEmpty());
  }

  @SuppressWarnings("unused")
  private static List<Arguments> getErrorReportLinkNotFoundCases() {
    return List.of(
        Arguments.of("title-not-found", "version-not-found"),
        Arguments.of("title-not-found", "the_gameVersion2"),
        Arguments.of("the_reportTitle2", "version-not-found"));
  }
}
