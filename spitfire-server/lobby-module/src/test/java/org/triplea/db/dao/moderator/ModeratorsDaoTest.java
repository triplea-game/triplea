package org.triplea.db.dao.moderator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.test.common.RequiresDatabase;

@DataSet(
    value = "moderators/user_role.yml, moderators/lobby_user.yml, moderators/access_log.yml",
    useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class ModeratorsDaoTest {

  private static final int NOT_MODERATOR_ID = 100000;
  private static final int MODERATOR_ID = 900000;
  private static final int SUPER_MODERATOR_ID = 900001;

  private final ModeratorsDao moderatorsDao;

  @Test
  void getModerators() {
    final List<ModeratorUserDaoData> moderators = moderatorsDao.getModerators();

    assertThat(
        "User dataset contains three players: an admin, moderator, and a player. We "
            + "expect the two non-player users to be returned.",
        moderators,
        hasSize(2));

    assertThat(moderators.get(0).getUsername(), is("moderator"));
    assertThat(moderators.get(0).getLastLogin(), isInstant(2001, 1, 1, 23, 59, 20));

    assertThat(moderators.get(1).getUsername(), is("Super! moderator"));
    assertThat(moderators.get(1).getLastLogin(), nullValue());
  }

  @Test
  @ExpectedDataSet("moderators/lobby_user_post_update_roles.yml")
  void updateRoles() {
    assertThat(moderatorsDao.setRole(NOT_MODERATOR_ID, UserRole.MODERATOR), is(1));
    assertThat(moderatorsDao.setRole(MODERATOR_ID, UserRole.ADMIN), is(1));
    assertThat(moderatorsDao.setRole(SUPER_MODERATOR_ID, UserRole.PLAYER), is(1));
  }
}
