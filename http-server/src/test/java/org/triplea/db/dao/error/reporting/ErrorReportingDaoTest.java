package org.triplea.db.dao.error.reporting;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;

final class ErrorReportingDaoTest extends DaoTest {
  private final ErrorReportingDao errorReportingDao = DaoTest.newDao(ErrorReportingDao.class);

  /** Simple check that if we insert a record we'll get a new record in the expected dataset. */
  @DataSet(cleanBefore = true, value = "error_reporting/pre-insert.yml")
  @ExpectedDataSet(value = "error_reporting/post-insert.yml")
  @Test
  void insertRow() {
    errorReportingDao.insertHistoryRecord("second");
  }

  @DataSet(cleanBefore = true, value = "error_reporting/pre-purge.yml")
  @ExpectedDataSet(value = "error_reporting/post-purge.yml")
  @Test
  void purgeOld() {
    errorReportingDao.purgeOld(LocalDateTime.of(2016, 1, 3, 23, 0, 0).toInstant(ZoneOffset.UTC));
  }
}
