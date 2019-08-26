package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(DBUnitExtension.class)
@DataSet("temp_password_history/sample.yml")
class TempPasswordHistoryDaoTest {

  private static final String USERNAME = "username";

  private final TempPasswordHistoryDao tempPasswordDao =
      JdbiDatabase.newConnection().onDemand(TempPasswordHistoryDao.class);

  @Test
  void verifyCountAndInsert() {

    final String localhost = "127.0.0.1";
    assertThat(tempPasswordDao.countRequestsFromAddress(localhost), is(0));

    tempPasswordDao.recordTempPasswordRequest(localhost, USERNAME);
    assertThat(tempPasswordDao.countRequestsFromAddress(localhost), is(1));

    tempPasswordDao.recordTempPasswordRequest(localhost, USERNAME);
    assertThat(tempPasswordDao.countRequestsFromAddress(localhost), is(2));

    tempPasswordDao.recordTempPasswordRequest(localhost, "other-user");
    assertThat(tempPasswordDao.countRequestsFromAddress(localhost), is(3));

    final String otherAddress = "127.0.0.2";
    assertThat(tempPasswordDao.countRequestsFromAddress(otherAddress), is(0));
  }
}
