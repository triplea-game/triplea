package org.triplea.lobby.server.db.dao;

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
import org.triplea.lobby.server.db.data.ModeratorUserDaoData;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(DBUnitExtension.class)
@DataSet("moderators/select.yml")
class ModeratorsDaoTest {

  private static final int NOT_MODERATOR_ID = 100000;
  private static final int MODERATOR_ID = 900000;
  private static final int SUPER_MODERATOR_ID = 900001;

  private final ModeratorsDao moderatorsDao =
      JdbiDatabase.newConnection().onDemand(ModeratorsDao.class);

  @Test
  void getModerators() {
    final List<ModeratorUserDaoData> moderators = moderatorsDao.getModerators();

    assertThat(moderators, hasSize(2));

    assertThat(moderators.get(0).getUsername(), is("moderator"));
    assertThat(moderators.get(0).getLastLogin(), is(Instant.parse("2001-01-01T23:59:20Z")));

    assertThat(moderators.get(1).getUsername(), is("Super! moderator"));
    assertThat(moderators.get(1).getLastLogin(), nullValue());
  }

  @Test
  @ExpectedDataSet("moderators/select_post_remove_mod.yml")
  void removeModerator() {
    assertThat(moderatorsDao.removeMod(NOT_MODERATOR_ID), is(0));
    assertThat(moderatorsDao.removeMod(MODERATOR_ID), is(1));
    assertThat(moderatorsDao.removeMod(SUPER_MODERATOR_ID), is(0));
  }

  @Test
  @ExpectedDataSet("moderators/select_post_add_super_mod.yml")
  void addSuperMod() {
    assertThat(moderatorsDao.addSuperMod(NOT_MODERATOR_ID), is(0));
    assertThat(moderatorsDao.addSuperMod(MODERATOR_ID), is(1));
    assertThat(moderatorsDao.addSuperMod(SUPER_MODERATOR_ID), is(0));
  }

  @Test
  @ExpectedDataSet("moderators/select_post_add_mod.yml")
  void addMod() {
    assertThat(moderatorsDao.addMod(NOT_MODERATOR_ID), is(1));
    assertThat(moderatorsDao.addMod(MODERATOR_ID), is(0));
    assertThat(moderatorsDao.addMod(SUPER_MODERATOR_ID), is(0));
  }
}
