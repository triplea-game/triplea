package org.triplea.db.dao.moderator.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class PlayerInfoForModeratorDaoTest {

  private final PlayerInfoForModeratorDao playerInfoForModeratorDao;

  @Nested
  @DataSet(value = "moderator_player_lookup/access_log.yml", useSequenceFiltering = false)
  class LookupPlayerAliases {
    @Test
    void lookupEmptyCase() {
      final List<PlayerAliasRecord> results =
          playerInfoForModeratorDao.lookupPlayerAliasRecords("system-id-dne", "9.9.9.9");

      assertThat(results, is(empty()));
    }

    @Test
    void lookupByBothSystemIdAndIp() {
      final List<PlayerAliasRecord> results =
          playerInfoForModeratorDao.lookupPlayerAliasRecords("system-id", "1.1.1.1");

      assertThat(
          "There are 6 records in the dataset, "
              + "we expect 4 to match, and 2 of them to be de-duped by name"
              + "with only the most recent de-duped record returned",
          results,
          hasSize(3));

      assertThat(results.get(0).getUsername(), is("name3"));
      assertThat(results.get(0).getIp(), is("2.2.2.2"));
      assertThat(results.get(0).getSystemId(), is("system-id"));
      assertThat(results.get(0).getDate(), isInstant(2154, 1, 1, 23, 59, 20));

      assertThat(results.get(1).getUsername(), is("name2"));
      assertThat(results.get(1).getIp(), is("1.1.1.1"));
      assertThat(results.get(1).getSystemId(), is("system-id2"));
      assertThat(results.get(1).getDate(), isInstant(2152, 1, 1, 23, 59, 20));

      assertThat(results.get(2).getUsername(), is("name1"));
      assertThat(results.get(2).getIp(), is("1.1.1.1"));
      assertThat(results.get(2).getSystemId(), is("system-id"));
      assertThat(results.get(2).getDate(), isInstant(2151, 1, 1, 23, 59, 20));
    }

    @Test
    void lookupWithOnlyIpMatching() {
      final List<PlayerAliasRecord> results =
          playerInfoForModeratorDao.lookupPlayerAliasRecords("system-id-dne", "2.2.2.2");

      assertThat("We expect to only match the one record with IP 2.2.2.2", results, hasSize(1));

      assertThat(results.get(0).getUsername(), is("name3"));
      assertThat(results.get(0).getIp(), is("2.2.2.2"));
      assertThat(results.get(0).getSystemId(), is("system-id"));
      assertThat(results.get(0).getDate(), isInstant(2154, 1, 1, 23, 59, 20));
    }

    @Test
    void lookupWithOnlySystemIdMatching() {
      final List<PlayerAliasRecord> results =
          playerInfoForModeratorDao.lookupPlayerAliasRecords("system-id2", "9.9.9.9");

      assertThat(
          "We expect to only match the 1 record with system 'system-id2'", results, hasSize(1));

      assertThat(results.get(0).getUsername(), is("name2"));
      assertThat(results.get(0).getIp(), is("1.1.1.1"));
      assertThat(results.get(0).getSystemId(), is("system-id2"));
      assertThat(results.get(0).getDate(), isInstant(2152, 1, 1, 23, 59, 20));
    }
  }

  @Nested
  @DataSet(value = "moderator_player_lookup/banned_user.yml", useSequenceFiltering = false)
  class LookupPlayerBans {
    @Test
    void emptyLookupCase() {
      final List<PlayerBanRecord> results =
          playerInfoForModeratorDao.lookupPlayerBanRecords("system-id-dne", "9.9.9.9");

      assertThat(results, is(empty()));
    }

    @Test
    void lookupByBothSystemIdAndIp() {
      final List<PlayerBanRecord> results =
          playerInfoForModeratorDao.lookupPlayerBanRecords("system-id", "1.1.1.1");

      assertThat(results, hasSize(3));

      assertThat(results.get(0).getUsername(), is("name1"));
      assertThat(results.get(0).getIp(), is("1.1.1.1"));
      assertThat(results.get(0).getSystemId(), is("system-id"));
      assertThat(results.get(0).getBanStart(), isInstant(2010, 1, 1, 23, 59, 20));
      assertThat(results.get(0).getBanEnd(), isInstant(2100, 1, 1, 23, 59, 20));

      assertThat(results.get(1).getUsername(), is("name2"));
      assertThat(results.get(1).getIp(), is("1.1.1.1"));
      assertThat(results.get(1).getSystemId(), is("system-id2"));
      assertThat(results.get(1).getBanStart(), isInstant(2000, 1, 1, 23, 59, 20));
      assertThat(results.get(1).getBanEnd(), isInstant(2050, 1, 1, 23, 59, 20));

      assertThat(results.get(2).getUsername(), is("name2"));
      assertThat(results.get(2).getIp(), is("2.2.2.2"));
      assertThat(results.get(2).getSystemId(), is("system-id"));
      assertThat(results.get(2).getBanStart(), isInstant(2000, 1, 1, 23, 59, 20));
      assertThat(results.get(2).getBanEnd(), isInstant(2010, 1, 1, 23, 59, 20));
    }

    @Test
    void lookupWithMatchByIpOnly() {
      final List<PlayerBanRecord> results =
          playerInfoForModeratorDao.lookupPlayerBanRecords("system-id-dne", "2.2.2.2");

      assertThat(results, hasSize(1));

      assertThat(results.get(0).getUsername(), is("name2"));
      assertThat(results.get(0).getIp(), is("2.2.2.2"));
      assertThat(results.get(0).getSystemId(), is("system-id"));
      assertThat(results.get(0).getBanStart(), isInstant(2000, 1, 1, 23, 59, 20));
      assertThat(results.get(0).getBanEnd(), isInstant(2010, 1, 1, 23, 59, 20));
    }

    @Test
    void lookupWithMatchBySystemIdOnly() {
      final List<PlayerBanRecord> results =
          playerInfoForModeratorDao.lookupPlayerBanRecords("system-id2", "9.9.9.9");

      assertThat(results, hasSize(1));

      assertThat(results.get(0).getUsername(), is("name2"));
      assertThat(results.get(0).getIp(), is("1.1.1.1"));
      assertThat(results.get(0).getSystemId(), is("system-id2"));
      assertThat(results.get(0).getBanStart(), isInstant(2000, 1, 1, 23, 59, 20));
      assertThat(results.get(0).getBanEnd(), isInstant(2050, 1, 1, 23, 59, 20));
    }
  }
}
