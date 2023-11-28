package org.triplea.db.dao.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class AccessLogDaoTest {
  private static final String EMPTY_ACCESS_LOG =
      "access_log/user_role.yml,access_log/lobby_user.yml";
  private static final String ACCESS_LOG_TABLES =
      "access_log/user_role.yml,access_log/lobby_user.yml,access_log/access_log.yml";

  private final AccessLogDao accessLogDao;

  @Test
  @DataSet(cleanBefore = true, value = EMPTY_ACCESS_LOG, useSequenceFiltering = false)
  void emptyDataCase() {
    assertThat(accessLogDao.fetchAccessLogRows(0, 1, "%", "%", "%"), is(empty()));
  }

  /**
   * In this test we verify.: - records are returned in reverse chronological order - data values
   * are as expected
   */
  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void fetchTwoRows() {
    List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 1, "%", "%", "%");
    assertThat(data, hasSize(1));

    verifyIsRowWithSecondUsername(data.get(0));

    data = accessLogDao.fetchAccessLogRows(1, 1, "%", "%", "%");
    assertThat(data, hasSize(1));

    verifyIsRowWithFirstUsername(data.get(0));
  }

  private void verifyIsRowWithSecondUsername(final AccessLogRecord accessLogRecord) {
    assertThat(accessLogRecord.getAccessTime(), isInstant(2016, 1, 3, 23, 59, 20));
    assertThat(accessLogRecord.getIp(), is("127.0.0.2"));
    assertThat(accessLogRecord.getSystemId(), is("system-id2"));
    assertThat(accessLogRecord.getUsername(), is("second"));
    assertThat(accessLogRecord.isRegistered(), is(false));
  }

  private void verifyIsRowWithFirstUsername(final AccessLogRecord accessLogRecord) {
    assertThat(accessLogRecord.getAccessTime(), isInstant(2016, 1, 1, 23, 59, 20));
    assertThat(accessLogRecord.getIp(), is("127.0.0.1"));
    assertThat(accessLogRecord.getSystemId(), is("system-id1"));
    assertThat(accessLogRecord.getUsername(), is("first"));
    assertThat(accessLogRecord.isRegistered(), is(true));
  }

  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void searchForAllIdentifiesWithExactMatch() {
    final List<AccessLogRecord> data =
        accessLogDao.fetchAccessLogRows(0, 2, "first", "127.0.0.1", "system-id1");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void searchForSystemId() {
    final List<AccessLogRecord> data =
        accessLogDao.fetchAccessLogRows(0, 2, "%", "%", "system-id1");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void searchForUserName() {
    final List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 2, "first", "%", "%");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void searchForIp() {
    final List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 2, "%", "127.0.0.1", "%");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  /** There are only 2 rows, requesting a row offset of '2' should yield no data. */
  @Test
  @DataSet(value = ACCESS_LOG_TABLES, useSequenceFiltering = false)
  void requestingRowsOffDataSetReturnsNothing() {
    assertThat(accessLogDao.fetchAccessLogRows(2, 1, "%", "%", "%"), is(empty()));
  }

  @Test
  @DataSet(cleanBefore = true, value = EMPTY_ACCESS_LOG, useSequenceFiltering = false)
  @ExpectedDataSet(value = "access_log/access_log_post_insert.yml", orderBy = "username")
  void insertAccessLogRecords() {
    accessLogDao.insertUserAccessRecord("anonymous", "127.0.0.50", "anonymous-system-id");
    accessLogDao.insertUserAccessRecord("registered_user", "127.0.0.20", "registered-system-id");
  }
}
