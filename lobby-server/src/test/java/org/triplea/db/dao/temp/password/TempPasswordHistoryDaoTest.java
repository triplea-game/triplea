package org.triplea.db.dao.temp.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.modules.http.LobbyServerTest;

@DataSet(cleanBefore = true, value = "temp_password_history/sample.yml")
@RequiredArgsConstructor
class TempPasswordHistoryDaoTest extends LobbyServerTest {

  private static final String USERNAME = "username";

  private final TempPasswordHistoryDao tempPasswordHistoryDao;

  @Test
  void verifyCountAndInsert() {

    final String localhost = "127.0.0.1";
    assertThat(tempPasswordHistoryDao.countRequestsFromAddress(localhost), is(0));

    tempPasswordHistoryDao.recordTempPasswordRequest(localhost, USERNAME);
    assertThat(tempPasswordHistoryDao.countRequestsFromAddress(localhost), is(1));

    tempPasswordHistoryDao.recordTempPasswordRequest(localhost, USERNAME);
    assertThat(tempPasswordHistoryDao.countRequestsFromAddress(localhost), is(2));

    tempPasswordHistoryDao.recordTempPasswordRequest(localhost, "other-user");
    assertThat(tempPasswordHistoryDao.countRequestsFromAddress(localhost), is(3));

    final String otherAddress = "127.0.0.2";
    assertThat(tempPasswordHistoryDao.countRequestsFromAddress(otherAddress), is(0));
  }
}
