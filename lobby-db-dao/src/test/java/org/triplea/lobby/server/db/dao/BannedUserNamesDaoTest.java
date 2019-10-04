package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.data.UsernameBanDaoData;
import org.triplea.test.common.Integration;

@ExtendWith(DBUnitExtension.class)
@Integration
class BannedUserNamesDaoTest {

  private static final UsernameBanDao bannedUserNamesDao =
      JdbiDatabase.newConnection().onDemand(UsernameBanDao.class);

  @DataSet(cleanBefore = true, value = "banned_names/two_rows.yml")
  @Test
  void getBannedUserNames() {
    final List<UsernameBanDaoData> data = bannedUserNamesDao.getBannedUserNames();

    assertThat(data, hasSize(2));

    assertThat(data, hasSize(2));
    assertThat(data.get(0).getUsername(), is("aaa"));
    assertThat(data.get(0).getDateCreated(), is(Instant.parse("2015-01-01T23:59:20.0Z")));

    assertThat(data.get(1).getUsername(), is("zzz"));
    assertThat(data.get(1).getDateCreated(), is(Instant.parse("2010-01-01T23:59:20.0Z")));
  }

  @DataSet(cleanBefore = true, value = "banned_names/one_row.yml")
  @ExpectedDataSet(value = "banned_names/two_rows.yml", ignoreCols = "date_created")
  @Test
  void addBannedUserName() {
    bannedUserNamesDao.addBannedUserName("aaa");
  }

  @DataSet(cleanBefore = true, value = "banned_names/two_rows.yml")
  @ExpectedDataSet("banned_names/one_row.yml")
  @Test
  void removeBannedUserName() {
    bannedUserNamesDao.removeBannedUserName("aaa");
  }
}
