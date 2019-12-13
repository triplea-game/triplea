package org.triplea.lobby.server.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.dao.DaoTest;

class GameHostingApiKeyDaoTest extends DaoTest {

  private final GameHostingApiKeyDao gameHostApiKeyDao = DaoTest.newDao(GameHostingApiKeyDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "game_hosting_api_key/key_exists.yml")
  void keyExists() {
    assertThat(gameHostApiKeyDao.keyExists("game-hosting-key"), is(true));
  }

  @Test
  @DataSet(cleanBefore = true, value = "game_hosting_api_key/key_exists.yml")
  void keyDoesNotExist() {
    assertThat(gameHostApiKeyDao.keyExists("DNE"), is(false));
  }

  @Test
  @DataSet(cleanBefore = true, value = "game_hosting_api_key/insert_key_before.yml")
  @ExpectedDataSet("game_hosting_api_key/insert_key_after.yml")
  void insertKey() {
    gameHostApiKeyDao.insertKey("game-hosting-api-key", "127.0.0.2");
  }
}
