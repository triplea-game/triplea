package org.triplea.db.dao.moderator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;
import org.triplea.db.dao.user.role.UserRole;

@DataSet(cleanBefore = true, value = "moderators/select.yml")
class ModeratorsDaoTest extends DaoTest {

  private static final int NOT_MODERATOR_ID = 100000;
  private static final int MODERATOR_ID = 900000;
  private static final int SUPER_MODERATOR_ID = 900001;

  private final ModeratorsDao moderatorsDao = newDao(ModeratorsDao.class);

  @Test
  void getModerators() {
    final List<ModeratorUserDaoData> moderators = moderatorsDao.getModerators();

    assertThat(
        "User dataset contains three players: an admin, moderator, and a player. We "
            + "expect the two non-player users to be returned.",
        moderators,
        hasSize(2));

    assertThat(moderators.get(0).getUsername(), is("moderator"));
    assertThat(moderators.get(0).getLastLogin(), is(Instant.parse("2001-01-01T23:59:20Z")));

    assertThat(moderators.get(1).getUsername(), is("Super! moderator"));
    assertThat(moderators.get(1).getLastLogin(), nullValue());
  }

  @Test
  @ExpectedDataSet("moderators/select_post_update_roles.yml")
  void updateRoles() {
    assertThat(moderatorsDao.setRole(NOT_MODERATOR_ID, UserRole.MODERATOR), is(1));
    assertThat(moderatorsDao.setRole(MODERATOR_ID, UserRole.ADMIN), is(1));
    assertThat(moderatorsDao.setRole(SUPER_MODERATOR_ID, UserRole.PLAYER), is(1));
  }
}
