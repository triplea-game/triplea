package org.triplea.db.dao.moderator;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Interface for adding new moderator audit records to database. These records keep track of which
 * actions moderators have taken, who the target was and which moderator took the action.
 */
public interface ModeratorAuditHistoryDao {
  /** The set of moderator actions. */
  enum AuditAction {
    BAN_MAC,
    BAN_USERNAME,
    REMOVE_USERNAME_BAN,
    BOOT_GAME,
    BOOT_USER_FROM_BOT,
    BOOT_USER_FROM_LOBBY,
    BAN_PLAYER_FROM_BOT,
    ADD_BAD_WORD,
    REMOVE_BAD_WORD,
    BAN_USER,
    REMOVE_USER_BAN,
    ADD_MODERATOR,
    REMOVE_MODERATOR,
    ADD_SUPER_MOD,
    DISCONNECT_USER,
    DISCONNECT_GAME,
    REMOTE_SHUTDOWN,
  }

  /** Parameters needed when adding an audit record. */
  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  final class AuditArgs {
    @Nonnull private final Integer moderatorUserId;
    @Nonnull private final AuditAction actionName;
    @Nonnull private final String actionTarget;
  }

  default void addAuditRecord(AuditArgs auditArgs) {
    final int rowsInserted =
        insertAuditRecord(
            auditArgs.moderatorUserId, auditArgs.actionName.toString(), auditArgs.actionTarget);
    Preconditions.checkState(rowsInserted == 1);
  }

  @SqlUpdate(
      "insert into moderator_action_history "
          + "  (lobby_user_id, action_name, action_target) "
          + "values (:moderatorId, :actionName, :actionTarget)")
  int insertAuditRecord(
      @Bind("moderatorId") int moderatorId,
      @Bind("actionName") String actionName,
      @Bind("actionTarget") String actionTarget);

  @SqlQuery(
      "select\n"
          + "  h.date_created "
          + ModeratorAuditHistoryRecord.DATE_CREATED_COLUMN
          + ",\n"
          + "  u.username "
          + ModeratorAuditHistoryRecord.USER_NAME_COLUMN
          + ",\n"
          + "  h.action_name "
          + ModeratorAuditHistoryRecord.ACTION_NAME_COLUMN
          + ",\n"
          + "  h.action_target "
          + ModeratorAuditHistoryRecord.ACTION_TARGET_COLUMN
          + "\n"
          + "from moderator_action_history h \n"
          + "join lobby_user u on u.id = h.lobby_user_id\n"
          + "order by h.date_created desc\n"
          + "offset :rowOffset rows\n"
          + "fetch next :rowCount rows only")
  List<ModeratorAuditHistoryRecord> lookupHistoryItems(
      @Bind("rowOffset") int rowOffset, @Bind("rowCount") int rowCount);
}
