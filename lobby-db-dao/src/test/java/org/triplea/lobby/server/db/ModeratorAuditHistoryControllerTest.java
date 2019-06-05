package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
class ModeratorAuditHistoryControllerTest {

  // test constants that exist in 'pre-insert.yml'
  private static final String MODERATOR_NAME = "moderator";
  private static final String ACTION_TARGET = "ACTION_TARGET";

  private final ModeratorAuditHistoryDao dao = JdbiDatabase.newConnection().onDemand(ModeratorAuditHistoryDao.class);

  @Test
  @DataSet("moderator_audit/pre-insert.yml")
  void addAuditRecordThrowsIfModeratorNameNotFound() {
    assertThrows(
        IllegalStateException.class,
        () -> dao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorName("moderator-name-that-does-not-exist")
            .actionTarget("any-value")
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
            .build()));
  }

  @Test
  @DataSet("moderator_audit/pre-insert.yml")
  @ExpectedDataSet("moderator_audit/post-insert.yml")
  void addAuditRecord() {
    dao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
        .moderatorName(MODERATOR_NAME)
        .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
        .actionTarget(ACTION_TARGET)
        .build());
  }
}
