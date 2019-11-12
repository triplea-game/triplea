package org.triplea.lobby.server.db.dao.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.DaoTest;

class AccessLogDaoTest extends DaoTest {
  private final AccessLogDao accessLogDao = DaoTest.newDao(AccessLogDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  void emptyDataCase() {
    assertThat(accessLogDao.fetchAccessLogRows(0, 1), hasSize(0));
  }

  /**
   * In this test we verify.: - records are returned in reverse chronological order - data values
   * are as expected
   */
  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void fetchTwoRows() {
    List<AccessLogRecord> data = accessLogDao.fetchAccessLogRows(0, 1);
    assertThat(data, hasSize(1));

    assertThat(data.get(0).getAccessTime(), is(Instant.parse("2016-01-03T23:59:20.0Z")));
    assertThat(data.get(0).getIp(), is("127.0.0.2"));
    assertThat(data.get(0).getSystemId(), is("system-id2"));
    assertThat(data.get(0).getUsername(), is("second"));
    assertThat(data.get(0).isRegistered(), is(false));

    data = accessLogDao.fetchAccessLogRows(1, 1);
    assertThat(data, hasSize(1));

    assertThat(data.get(0).getAccessTime(), is(Instant.parse("2016-01-01T23:59:20.0Z")));
    assertThat(data.get(0).getIp(), is("127.0.0.1"));
    assertThat(data.get(0).getSystemId(), is("system-id1"));
    assertThat(data.get(0).getUsername(), is("first"));
    assertThat(data.get(0).isRegistered(), is(true));
  }

  /** There are only 2 rows, requesting a row offset of '2' should yield no data. */
  @Test
  @DataSet(cleanBefore = true, value = "access_log/two_rows.yml")
  void requestingRowsOffDataSetReturnsNothing() {
    assertThat(accessLogDao.fetchAccessLogRows(2, 1), hasSize(0));
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  @ExpectedDataSet("access_log/insert_registered_after.yml")
  void insertRegisteredUserAccessLog() {
    accessLogDao.insertRegisteredUserRecord("registered", "127.0.0.20", "registered-system-id");
  }

  @Test
  @DataSet(cleanBefore = true, value = "access_log/empty_data.yml")
  @ExpectedDataSet("access_log/insert_anonymous_after.yml")
  void insertAnonymousAccessLog() {
    accessLogDao.insertAnonymousUserRecord("anonymous", "127.0.0.50", "anonymous-system-id");
  }
}
