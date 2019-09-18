package org.triplea.lobby.server.db.dao;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

@ExtendWith(DBUnitExtension.class)
@Integration
final class ErrorReportingDaoTest {
  private static final ErrorReportingDao REPORTING_DAO =
      JdbiDatabase.newConnection().onDemand(ErrorReportingDao.class);

  /** Simple check that if we insert a record we'll get a new record in the expected dataset. */
  @DataSet(cleanBefore = true, value = "error_reporting/pre-insert.yml")
  @ExpectedDataSet(value = "error_reporting/post-insert.yml")
  @Test
  void insertRow() {
    REPORTING_DAO.insertHistoryRecord("second");
  }

  @DataSet(cleanBefore = true, value = "error_reporting/pre-purge.yml")
  @ExpectedDataSet(value = "error_reporting/post-purge.yml")
  @Test
  void purgeOld() {
    REPORTING_DAO.purgeOld(LocalDateTime.of(2016, 1, 3, 23, 0, 0).toInstant(ZoneOffset.UTC));
  }
}
