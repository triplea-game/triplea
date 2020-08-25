package org.triplea.db.dao.username.ban;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.modules.http.LobbyServerTest;

@RequiredArgsConstructor
class UsernameBanDaoTest extends LobbyServerTest {

  private final UsernameBanDao usernameBanDao;

  @Test
  @DisplayName("Verify retrieving username bans")
  @DataSet(cleanBefore = true, value = "username_ban/get_banned_usernames.yml")
  void getBannedUserNames() {
    final List<UsernameBanRecord> result = usernameBanDao.getBannedUserNames();
    assertThat(result, hasSize(2));

    assertThat(result.get(0).getUsername(), is("username1"));
    assertThat(result.get(0).getDateCreated(), isInstant(2001, 1, 1, 23, 59, 59));

    assertThat(result.get(1).getUsername(), is("username2"));
    assertThat(result.get(1).getDateCreated(), isInstant(2000, 1, 1, 23, 59, 59));
  }

  @Test
  @DisplayName("Verify name matching")
  @DataSet(cleanBefore = true, value = "username_ban/get_banned_usernames.yml")
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
  @DataSet(cleanBefore = true, value = "username_ban/add_banned_username_before.yml")
  @ExpectedDataSet("username_ban/add_banned_username_after.yml")
  void addBannedUserName() {
    usernameBanDao.addBannedUserName("username");
  }

  @Test
  @DisplayName("Verify removing a username ban")
  @DataSet(cleanBefore = true, value = "username_ban/remove_banned_username_before.yml")
  @ExpectedDataSet("username_ban/remove_banned_username_after.yml")
  void removeBannedUserName() {
    final int result = usernameBanDao.removeBannedUserName("username");

    assertThat(result, is(1));
  }

  @Test
  @DisplayName("Verify when removing a username that DNE, that nothing changes")
  @DataSet(cleanBefore = true, value = "username_ban/remove_banned_username_before.yml")
  @ExpectedDataSet("username_ban/remove_banned_username_before.yml")
  void removeBannedUserNameNameDoesNotExist() {
    final int result = usernameBanDao.removeBannedUserName("DNE");

    assertThat(result, is(0));
  }
}
