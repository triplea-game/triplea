package org.triplea.db.dao.user.ban;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class UserBanDaoTest {
  private final UserBanDao userBanDao;

  @Nested
  @DataSet(value = "user_ban/banned_by_ip.yml", useSequenceFiltering = false)
  class IsBannedByIp {
    @Test
    void isBannedByIpPositiveCase() {
      assertThat(userBanDao.isBannedByIp("127.0.0.1"), is(true));
    }

    @Test
    void notBannedWhenIpNotPresent() {
      assertThat(userBanDao.isBannedByIp("1.1.1.1"), is(false));
    }

    @Test
    void notBannedWhenBanIsExpired() {
      assertThat(userBanDao.isBannedByIp("127.0.0.2"), is(false));
    }
  }

  @Nested
  @DataSet(value = "user_ban/lookup_bans.yml", useSequenceFiltering = false)
  class BanLookups {
    @Test
    @DisplayName("Verify retrieval of all current bans")
    void lookupBans() {
      final List<UserBanRecord> result = userBanDao.lookupBans();

      assertThat(result, hasSize(2));
      assertThat(result.get(0).getBanExpiry(), isInstant(2100, 1, 1, 23, 59, 59));
      assertThat(result.get(0).getDateCreated(), isInstant(2020, 1, 1, 23, 59, 59));
      assertThat(result.get(0).getIp(), is("127.0.0.2"));
      assertThat(result.get(0).getPublicBanId(), is("public-id2"));
      assertThat(result.get(0).getSystemId(), is("system-id2"));
      assertThat(result.get(0).getUsername(), is("username2"));

      assertThat(result.get(1).getBanExpiry(), isInstant(2200, 1, 1, 23, 59, 59));
      assertThat(result.get(1).getDateCreated(), isInstant(2010, 1, 1, 23, 59, 59));
      assertThat(result.get(1).getIp(), is("127.0.0.1"));
      assertThat(result.get(1).getPublicBanId(), is("public-id1"));
      assertThat(result.get(1).getSystemId(), is("system-id1"));
      assertThat(result.get(1).getUsername(), is("username1"));
    }

    @Test
    @DisplayName("Verify ban lookup case with no results found")
    void lookupBanEmptyCase() {
      final Optional<BanLookupRecord> result = userBanDao.lookupBan("99.99.99.99", "system-id-DNE");

      assertThat(result, isEmpty());
    }

    @Test
    @DisplayName("Verify ban lookup by IP address")
    void lookupBanRecordByIp() {
      final Optional<BanLookupRecord> result = userBanDao.lookupBan("127.0.0.2", "any-system-id");
      assertThat(
          result,
          isPresentAndIs(
              BanLookupRecord.builder()
                  .banExpiry(Instant.parse("2100-01-01T23:59:59.0Z"))
                  .publicBanId("public-id2")
                  .build()));
    }

    @Test
    @DisplayName("Verify we can lookup bans by system-id and will choose the row with max(expiry)")
    void lookupBanRecordBySystemId() {
      final Optional<BanLookupRecord> result = userBanDao.lookupBan("99.99.99.99", "system-id2");
      assertThat(
          result,
          isPresentAndIs(
              BanLookupRecord.builder()
                  .banExpiry(Instant.parse("2100-01-01T23:59:59.0Z"))
                  .publicBanId("public-id2")
                  .build()));
    }
  }

  @Nested
  @DataSet(value = "user_ban/lookup_username_by_ban_id.yml", useSequenceFiltering = false)
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

  @Nested
  class AddAndRemoveBan {
    @Test
    @DataSet(value = "user_ban/remove_ban_before.yml", useSequenceFiltering = false)
    @ExpectedDataSet("user_ban/remove_ban_after.yml")
    void removeBan() {
      userBanDao.removeBan("public-id");
    }

    @Test
    @DataSet(value = "user_ban/add_ban_before.yml", useSequenceFiltering = false)
    @ExpectedDataSet("user_ban/add_ban_after.yml")
    void addBan() {
      userBanDao.addBan("public-id", "username", "system-id", "127.0.0.3", 5);
    }
  }
}
