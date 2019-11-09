package org.triplea.lobby.server.db.dao.user.ban;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.DaoTest;

class UserBanDaoTest extends DaoTest {
  private final UserBanDao userBanDao = DaoTest.newDao(UserBanDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "user_ban/lookup_bans.yml")
  void lookupBans() {
    final List<UserBanRecord> result = userBanDao.lookupBans();

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getBanExpiry(), is(Instant.parse("2100-01-01T23:59:59.0Z")));
    assertThat(result.get(0).getDateCreated(), is(Instant.parse("2020-01-01T23:59:59.0Z")));
    assertThat(result.get(0).getIp(), is("127.0.0.2"));
    assertThat(result.get(0).getPublicBanId(), is("public-id2"));
    assertThat(result.get(0).getSystemId(), is("system-id2"));
    assertThat(result.get(0).getUsername(), is("username2"));

    assertThat(result.get(1).getBanExpiry(), is(Instant.parse("2021-01-01T23:59:59.0Z")));
    assertThat(result.get(1).getDateCreated(), is(Instant.parse("2010-01-01T23:59:59.0Z")));
    assertThat(result.get(1).getIp(), is("127.0.0.1"));
    assertThat(result.get(1).getPublicBanId(), is("public-id1"));
    assertThat(result.get(1).getSystemId(), is("system-id1"));
    assertThat(result.get(1).getUsername(), is("username1"));
  }

  @Nested
  @DataSet(cleanBefore = true, value = "user_ban/lookup_username_by_ban_id.yml")
  class LookupUsernameByBanId {
    @Test
    void banIdFound() {
      assertThat(userBanDao.lookupUsernameByBanId("public-id"), isPresentAndIs("username"));
    }

    @Test
    void banIdNotFound() {
      assertThat(userBanDao.lookupUsernameByBanId("DNE"), isEmpty());
    }
  }

  @Test
  @DataSet(cleanBefore = true, value = "user_ban/remove_ban_before.yml")
  @ExpectedDataSet("user_ban/remove_ban_after.yml")
  void removeBan() {
    userBanDao.removeBan("public-id");
  }

  @Test
  @DataSet(cleanBefore = true, value = "user_ban/add_ban_before.yml")
  @ExpectedDataSet("user_ban/add_ban_after.yml")
  void addBan() {
    userBanDao.addBan("public-id", "username", "system-id", "127.0.0.3", 5);
  }
}
