package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.username.ban.UsernameBanDao;
import org.triplea.lobby.server.db.dao.username.ban.UsernameBanRecord;

class BannedUserNamesDaoTest extends DaoTest {
  private final UsernameBanDao bannedUserNamesDao = DaoTest.newDao(UsernameBanDao.class);

  @DataSet(cleanBefore = true, value = "banned_names/two_rows.yml")
  @Test
  void getBannedUserNames() {
    final List<UsernameBanRecord> data = bannedUserNamesDao.getBannedUserNames();

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
