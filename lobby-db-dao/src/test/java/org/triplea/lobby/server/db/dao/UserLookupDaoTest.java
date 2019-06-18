package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import com.github.npathai.hamcrestopt.OptionalMatchers;

@Integration
@ExtendWith(DBUnitExtension.class)
@DataSet("user_lookup/select.yml")
class UserLookupDaoTest {

  private final UserLookupDao userLookupDao =
      JdbiDatabase.newConnection().onDemand(UserLookupDao.class);

  @Test
  void lookupUserIdByName() {
    assertThat(userLookupDao.lookupUserIdByName("DNE"), isEmpty());
    assertThat(userLookupDao.lookupUserIdByName("user"), OptionalMatchers.isPresentAndIs(900000));
  }
}
