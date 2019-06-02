package org.triplea.lobby.server.db;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Interface for adding new moderator audit records to database. These records
 * keep track of which actions moderators have taken, who the target was and
 * which moderator took the action.
 */
public interface ModeratorAuditHistoryDao {

  void addAuditRecord(AuditArgs auditArgs);

  /**
   * Parameters needed when adding an audit record.
   */
  @Getter
  @Builder
  @ToString
  final class AuditArgs {
    @Nonnull
    private final String moderatorName;
    @Nonnull
    private final AuditAction actionName;
    @Nonnull
    private final String actionTarget;
  }


  /**
   * The set of moderator actions.
   */
  enum AuditAction {
    BAN_MAC, BAN_USERNAME, BOOT_USER_FROM_BOT, BOOT_USER_FROM_LOBBY, BAN_PLAYER_FROM_BOT,
  }
}
