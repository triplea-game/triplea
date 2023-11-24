package org.triplea.db.dao.moderator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@DataSet(
    value =
        "moderator_audit/user_role.yml,"
            + "moderator_audit/lobby_user.yml,"
            + "moderator_audit/moderator_action_history.yml",
    useSequenceFiltering = false)
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class ModeratorAuditHistoryDaoTest {

  private static final int MODERATOR_ID = 900000;
  private static final int MODERATOR_ID_DOES_NOT_EXIST = 1111;

  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @Test
  void addAuditRecordThrowsIfModeratorNameNotFound() {
    assertThrows(
        UnableToExecuteStatementException.class,
        () ->
            moderatorAuditHistoryDao.addAuditRecord(
                ModeratorAuditHistoryDao.AuditArgs.builder()
                    .moderatorUserId(MODERATOR_ID_DOES_NOT_EXIST)
                    .actionTarget("any-value")
                    .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
                    .build()));
  }

  @Test
  @DataSet(
      value =
          "moderator_audit/user_role.yml,"
              + "moderator_audit/lobby_user.yml,"
              + "moderator_audit/empty_moderator_action_history.yml",
      useSequenceFiltering = false)
  @ExpectedDataSet("moderator_audit/moderator_action_history_post_insert.yml")
  void addAuditRecord() {
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(MODERATOR_ID)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
            .actionTarget("ACTION_TARGET")
            .build());
  }

  @Test
  void selectHistory() {
    List<ModeratorAuditHistoryRecord> results = moderatorAuditHistoryDao.lookupHistoryItems(0, 3);

    assertThat(results, hasSize(3));

    assertThat(results.get(0).getUsername(), is("moderator2"));
    assertThat(results.get(0).getDateCreated(), isInstant(2016, 1, 5, 23, 59, 20));
    assertThat(results.get(0).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(0).getActionTarget(), is("ACTION_TARGET5"));

    assertThat(results.get(1).getUsername(), is("moderator1"));
    assertThat(results.get(1).getDateCreated(), isInstant(2016, 1, 4, 23, 59, 20));
    assertThat(results.get(1).getActionName(), is("BOOT_PLAYER"));
    assertThat(results.get(1).getActionTarget(), is("ACTION_TARGET4"));

    assertThat(results.get(2).getUsername(), is("moderator2"));
    assertThat(results.get(2).getDateCreated(), isInstant(2016, 1, 3, 23, 59, 20));
    assertThat(results.get(2).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(2).getActionTarget(), is("ACTION_TARGET3"));

    // there are only 5 records total
    results = moderatorAuditHistoryDao.lookupHistoryItems(3, 3);
    assertThat(results, hasSize(2));
    assertThat(results.get(0).getUsername(), is("moderator2"));
    assertThat(results.get(0).getDateCreated(), isInstant(2016, 1, 2, 23, 59, 20));
    assertThat(results.get(0).getActionName(), is("MUTE_USERNAME"));
    assertThat(results.get(0).getActionTarget(), is("ACTION_TARGET2"));

    assertThat(results.get(1).getUsername(), is("moderator1"));
    assertThat(results.get(1).getDateCreated(), isInstant(2016, 1, 1, 23, 59, 20));
    assertThat(results.get(1).getActionName(), is("BAN_USERNAME"));
    assertThat(results.get(1).getActionTarget(), is("ACTION_TARGET1"));

    results = moderatorAuditHistoryDao.lookupHistoryItems(5, 3);
    assertThat(results, is(empty()));
  }
}
