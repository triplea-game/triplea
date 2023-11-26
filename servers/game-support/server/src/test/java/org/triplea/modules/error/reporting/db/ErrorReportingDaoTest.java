package org.triplea.modules.error.reporting.db;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.server.error.reporting.upload.ErrorReportingDao;
import org.triplea.server.error.reporting.upload.InsertHistoryRecordParams;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(ErrorReportingModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
final class ErrorReportingDaoTest {
  private final ErrorReportingDao errorReportingDao;

  /** Simple check that if we insert a record we'll get a new record in the expected dataset. */
  @DataSet(value = "error_reporting/empty_error_report_history.yml", useSequenceFiltering = false)
  @ExpectedDataSet(value = "error_reporting/error_report_history_post_insert.yml")
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

  @DataSet(value = "error_reporting/error_report_history.yml", useSequenceFiltering = false)
  @ExpectedDataSet(value = "error_reporting/error_report_history_post_purge.yml")
  @Test
  void purgeOld() {
    errorReportingDao.purgeOld(LocalDateTime.of(2016, 1, 3, 23, 0, 0).toInstant(ZoneOffset.UTC));
  }

  @DataSet(
      value = "error_reporting/error_report_history_post_purge.yml",
      useSequenceFiltering = false)
  @Test
  void getErrorReportLinkFoundCase() {
    assertThat(
        errorReportingDao.getErrorReportLink("the_reportTitle2", "the_gameVersion2"),
        isPresentAndIs("the_createdIssueLink2"));
  }

  @DataSet(
      value = "error_reporting/error_report_history_post_purge.yml",
      useSequenceFiltering = false)
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
