package org.triplea.db.dao.username.ban;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class UsernameBanDaoTest {

  private final UsernameBanDao usernameBanDao;

  @Test
  @DisplayName("Verify retrieving username bans")
  @DataSet(value = "username_ban/get_banned_usernames.yml", useSequenceFiltering = false)
  void getBannedUserNames() {
    final List<UsernameBanRecord> result = usernameBanDao.getBannedUserNames();
    assertThat(result, hasSize(2));

    assertThat(result.get(0).getUsername(), is("USERNAME1"));
    assertThat(result.get(0).getDateCreated(), isInstant(2001, 1, 1, 23, 59, 59));

    assertThat(result.get(1).getUsername(), is("USERNAME2"));
    assertThat(result.get(1).getDateCreated(), isInstant(2000, 1, 1, 23, 59, 59));
  }

  @Test
  @DisplayName("Verify name matching")
  @DataSet(value = "username_ban/get_banned_usernames.yml", useSequenceFiltering = false)
  void nameIsBanned() {
    assertThat(
        "Exact match should return true",
        usernameBanDao.nameIsBanned("username1"), //
        is(true));

    assertThat(
        "Case insensitive match should return true",
        usernameBanDao.nameIsBanned("username1".toUpperCase()),
        is(true));

    assertThat(
        "Non-exact match should return false",
        usernameBanDao.nameIsBanned("username1_"), //
        is(false));
  }

  @Test
  @DisplayName("Verify adding a username ban")
  @DataSet(value = "username_ban/add_banned_username_before.yml", useSequenceFiltering = false)
  @ExpectedDataSet("username_ban/add_banned_username_after.yml")
  void addBannedUserName() {
    usernameBanDao.addBannedUserName("username");
  }

  @Test
  @DisplayName("Verify removing a username ban")
  @DataSet(value = "username_ban/remove_banned_username_before.yml", useSequenceFiltering = false)
  @ExpectedDataSet("username_ban/remove_banned_username_after.yml")
  void removeBannedUserName() {
    final int result = usernameBanDao.removeBannedUserName("username");

    assertThat(result, is(1));
  }

  @Test
  @DisplayName("Verify when removing a username that DNE, that nothing changes")
  @DataSet(value = "username_ban/remove_banned_username_before.yml", useSequenceFiltering = false)
  @ExpectedDataSet("username_ban/remove_banned_username_before.yml")
  void removeBannedUserNameNameDoesNotExist() {
    final int result = usernameBanDao.removeBannedUserName("DNE");

    assertThat(result, is(0));
  }
}
