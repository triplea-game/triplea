package org.triplea.db.dao.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.modules.http.LobbyServerTest;

@RequiredArgsConstructor
class AccessLogDaoTest extends LobbyServerTest {
  private final AccessLogDao accessLogDao;

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  void emptyDataCase() {
    assertThat(accessLogDao.fetchAccessLogRows(0, 1, "%", "%", "%"), hasSize(0));
  }

  /**
   * In this test we verify.: - records are returned in reverse chronological order - data values
   * are as expected
   */
  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
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
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void searchForAllIdentifiesWithExactMatch() {
    final List<AccessLogRecord> data =
        accessLogDao.fetchAccessLogRows(0, 2, "first", "127.0.0.1", "system-id1");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void searchForSystemId() {
    final List<AccessLogRecord> data =
        accessLogDao.fetchAccessLogRows(0, 2, "%", "%", "system-id1");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void searchForUserName() {
    final List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 2, "first", "%", "%");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void searchForIp() {
    final List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 2, "%", "127.0.0.1", "%");

    assertThat(data, hasSize(1));
    verifyIsRowWithFirstUsername(data.get(0));
  }

  /** There are only 2 rows, requesting a row offset of '2' should yield no data. */
  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void requestingRowsOffDataSetReturnsNothing() {
    assertThat(accessLogDao.fetchAccessLogRows(2, 1, "%", "%", "%"), hasSize(0));
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  @ExpectedDataSet("access_log/insert_registered_after.yml")
  void insertRegisteredUserAccessLog() {
    accessLogDao.insertUserAccessRecord("registered", "127.0.0.20", "registered-system-id");
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  @ExpectedDataSet("access_log/insert_anonymous_after.yml")
  void insertAnonymousAccessLog() {
    accessLogDao.insertUserAccessRecord("anonymous", "127.0.0.50", "anonymous-system-id");
  }
}
