package org.triplea.lobby.server.db.dao;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(DBUnitExtension.class)
@DataSet("moderator_single_use_key/select.yml")
class ModeratorSingleUseKeyDaoTest {

  private static final int MODERATOR_ID = 900000;

  private static final String SECRET_KEY =
      "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String USED_KEY =
      "aaa9f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private static final String EXPIRED_KEY =
      "bbb9f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5"
          + "976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";

  private final ModeratorSingleUseKeyDao moderatorSingleUseKeyDao =
      JdbiDatabase.newConnection().onDemand(ModeratorSingleUseKeyDao.class);

  @Test
  void lookupModeratorBySingleUseKey() {
    assertThat(
        moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(SECRET_KEY),
        isPresentAndIs(MODERATOR_ID));
    assertThat(moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey("DNE"), isEmpty());
    assertThat(moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(USED_KEY), isEmpty());
    assertThat(moderatorSingleUseKeyDao.lookupModeratorBySingleUseKey(EXPIRED_KEY), isEmpty());
  }

  @Test
  @DataSet("moderator_single_use_key/empty.yml")
  @ExpectedDataSet("moderator_single_use_key/empty_post_insert.yml")
  void insertSingleUseKey() {
    moderatorSingleUseKeyDao.insertSingleUseKey(MODERATOR_ID, SECRET_KEY);
  }

  @Test
  void invalidateSingleUseKey() {
    assertThat(moderatorSingleUseKeyDao.invalidateSingleUseKey(SECRET_KEY), is(1));
    assertThat(moderatorSingleUseKeyDao.invalidateSingleUseKey(SECRET_KEY), is(0));

    assertThat(moderatorSingleUseKeyDao.invalidateSingleUseKey("DNE"), is(0));
    assertThat(moderatorSingleUseKeyDao.invalidateSingleUseKey(USED_KEY), is(0));
    assertThat(moderatorSingleUseKeyDao.invalidateSingleUseKey(EXPIRED_KEY), is(0));
  }

  @Test
  @ExpectedDataSet("moderator_single_use_key/select_post_delete.yml")
  void deleteSingleUseKeysByUserId() {
    assertThat(moderatorSingleUseKeyDao.deleteKeysByUserId(MODERATOR_ID), is(3));
    assertThat(moderatorSingleUseKeyDao.deleteKeysByUserId(MODERATOR_ID), is(0));
    assertThat(moderatorSingleUseKeyDao.deleteKeysByUserId(1000), is(0));
  }
}
