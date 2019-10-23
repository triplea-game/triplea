package org.triplea.lobby.server.db.dao.api.key;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.dao.DaoTest;

/**
 * Make sure data transforms successfully from wrapper to database. Simply make valid requests and
 * ensure we do not get any exceptions.
 */
@DataSet(cleanBefore = true, value = "api_key/initial.yml")
class ApiKeyDaoWrapperIntegrationTest extends DaoTest {

  private final ApiKeyDaoWrapper wrapper = new ApiKeyDaoWrapper(JdbiDatabase.newConnection());

  @Test
  void lookupApiKey() {
    wrapper.lookupByApiKey(ApiKey.of("api-key"));
  }

  @Test
  void storeKeyHostRole() throws Exception {
    wrapper.newKey(InetAddress.getLocalHost());
  }

  @Test
  void storeKeyAnonymousRole() throws Exception {
    wrapper.newKey(PlayerName.of("not-registered"), InetAddress.getLocalHost());
  }

  @Test
  void storeKeyPlayerRole() throws Exception {
    // this player name *is* in database
    wrapper.newKey(PlayerName.of("registered-user"), InetAddress.getLocalHost());
  }
}
