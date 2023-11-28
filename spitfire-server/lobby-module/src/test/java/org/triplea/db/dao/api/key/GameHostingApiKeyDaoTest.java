package org.triplea.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class GameHostingApiKeyDaoTest {

  private final GameHostingApiKeyDao gameHostApiKeyDao;

  @Test
  @DataSet(value = "game_hosting_api_key/key_exists.yml", useSequenceFiltering = false)
  void keyExists() {
    assertThat(gameHostApiKeyDao.keyExists("game-hosting-key"), is(true));
  }

  @Test
  @DataSet(value = "game_hosting_api_key/key_exists.yml", useSequenceFiltering = false)
  void keyDoesNotExist() {
    assertThat(gameHostApiKeyDao.keyExists("DNE"), is(false));
  }

  @Test
  @DataSet(value = "game_hosting_api_key/insert_key_before.yml", useSequenceFiltering = false)
  @ExpectedDataSet("game_hosting_api_key/insert_key_after.yml")
  void insertKey() {
    gameHostApiKeyDao.insertKey("game-hosting-api-key", "127.0.0.2");
  }
}
