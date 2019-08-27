package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.data.ApiKeyDaoData;
import org.triplea.test.common.Integration;

@ExtendWith(DBUnitExtension.class)
@Integration
@DataSet(cleanBefore = true, value = "moderator_api_key/select.yml")
class ModeratorApiKeyDaoTest {

  private static final String SECRET_KEY =
      "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String SECOND_KEY =
      "aaa9f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String THIRD_KEY =
      "ccc9f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String FOURTH_KEY =
      "ddd9f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final int MODERATOR_ID = 900000;
  private static final int SUPER_MODERATOR_ID = 900001;

  private static final ModeratorApiKeyDao moderatorApiKeyDao =
      JdbiDatabase.newConnection().onDemand(ModeratorApiKeyDao.class);

  @Test
  void getKeysByUserId() {
    assertThat(moderatorApiKeyDao.getKeysByUserId(1), hasSize(0));
    assertThat(moderatorApiKeyDao.getKeysByUserId(900001), hasSize(1));

    final List<ApiKeyDaoData> apiKeys = moderatorApiKeyDao.getKeysByUserId(MODERATOR_ID);
    assertThat(apiKeys, hasSize(2));

    assertThat(apiKeys.get(0).getLastUsed(), is(Instant.parse("2010-01-01T23:59:20.0Z")));
    assertThat(apiKeys.get(0).getLastUsedByHostAddress(), is("99.99.99.99"));
    assertThat(apiKeys.get(0).getPublicId(), is("90.1"));

    assertThat(apiKeys.get(1).getLastUsed(), nullValue());
    assertThat(apiKeys.get(1).getLastUsedByHostAddress(), is("99.99.99.11"));
    assertThat(apiKeys.get(1).getPublicId(), is("90.2"));
  }

  @Test
  @ExpectedDataSet(
      value = "moderator_api_key/select_post_insert.yml",
      orderBy = "public_id",
      ignoreCols = {"id", "date_created"})
  void insertNewKey() {
    moderatorApiKeyDao.insertNewApiKey("90.4", MODERATOR_ID, "11.11.11.11", FOURTH_KEY);
  }

  @Test
  @ExpectedDataSet("moderator_api_key/select_post_delete.yml")
  void deleteKey() {
    assertThat(moderatorApiKeyDao.deleteKey("AA.A"), is(0));
    assertThat(moderatorApiKeyDao.deleteKey("90.1"), is(1));
    assertThat(moderatorApiKeyDao.deleteKey("90.2"), is(1));
    assertThat(moderatorApiKeyDao.deleteKey("90.3"), is(1));
  }

  @Test
  @ExpectedDataSet("moderator_api_key/select_post_delete.yml")
  void deleteUserKeys() {
    assertThat(moderatorApiKeyDao.deleteKeysByUserId(MODERATOR_ID), is(2));
    assertThat(moderatorApiKeyDao.deleteKeysByUserId(SUPER_MODERATOR_ID), is(1));
    assertThat(moderatorApiKeyDao.deleteKeysByUserId(1111), is(0));
  }

  @Test
  void lookupModeratorIdByApiKey() {
    assertThat(moderatorApiKeyDao.lookupModeratorIdByApiKey("DNE"), isEmpty());
    assertThat(
        moderatorApiKeyDao.lookupModeratorIdByApiKey(SECRET_KEY), isPresentAndIs(MODERATOR_ID));
  }

  @Test
  void isSuperModeratorKey() {
    assertThat(moderatorApiKeyDao.lookupModeratorIdByApiKey("DNE"), isEmpty());
    assertThat(
        moderatorApiKeyDao.lookupModeratorIdByApiKey(SECRET_KEY), isPresentAndIs(MODERATOR_ID));
  }

  @Test
  void lookupSuperModeratorIdByApiKey() {
    assertThat(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey("DNE"), isEmpty());
    assertThat(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(SECRET_KEY), isEmpty());
    assertThat(moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(SECOND_KEY), isEmpty());
    assertThat(
        moderatorApiKeyDao.lookupSuperModeratorIdByApiKey(THIRD_KEY),
        isPresentAndIs(SUPER_MODERATOR_ID));
  }

  @Test
  void recordKeyUsage() {
    assertThat(moderatorApiKeyDao.recordKeyUsage("DNE", "1.1.1.1"), is(0));
    assertThat(moderatorApiKeyDao.recordKeyUsage(SECRET_KEY, "1.1.1.1"), is(1));
    // TODO: see if we can verify that the last-used-date and last-used-by-ip columns are updated
  }
}
