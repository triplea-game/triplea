package org.triplea.modules.moderation.moderators;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.java.Log;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.db.dao.moderator.ModeratorsDao;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.client.lobby.moderator.toolbox.management.ModeratorInfo;

@Builder
@Log
class ModeratorsService {
  @Nonnull private final ModeratorsDao moderatorsDao;
  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  public static ModeratorsService build(final Jdbi jdbi) {
    return ModeratorsService.builder()
        .moderatorsDao(jdbi.onDemand(ModeratorsDao.class))
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .build();
  }

  /** Returns a list of all users that are moderators. */
  List<ModeratorInfo> fetchModerators() {
    return moderatorsDao.getModerators().stream()
        .map(
            userInfo ->
                ModeratorInfo.builder()
                    .name(userInfo.getUsername())
                    .lastLogin(userInfo.getLastLogin())
                    .build())
        .collect(Collectors.toList());
  }

  /** Promotes a user to moderator. Can only be done by super-moderators. */
  void addModerator(final int moderatorIdRequesting, final String username) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(username)
            .orElseThrow(
                () -> new IllegalArgumentException("Unable to find username: " + username));

    Preconditions.checkState(moderatorsDao.setRole(userId, UserRole.MODERATOR) == 1);
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_MODERATOR)
            .actionTarget(username)
            .build());
    log.info(username + " was promoted to moderator");
  }

  /** Removes moderator status from a user. Can only be done by super moderators. */
  void removeMod(final int moderatorIdRequesting, final String moderatorNameToRemove) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(moderatorNameToRemove)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Failed to find moderator by user name: " + moderatorNameToRemove));

    Preconditions.checkState(
        moderatorsDao.setRole(userId, UserRole.PLAYER) == 1,
        "Failed to remove moderator status for: " + moderatorNameToRemove);

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_MODERATOR)
            .actionTarget(moderatorNameToRemove)
            .build());
    log.info(moderatorNameToRemove + " was removed from moderators");
  }

  /** Promotes a user to super-moderator. Can only be done by super moderators. */
  void addSuperMod(final int moderatorIdRequesting, final String username) {
    final int userId =
        userJdbiDao
            .lookupUserIdByName(username)
            .orElseThrow(
                () -> new IllegalArgumentException("Failed to find user by name: " + username));

    Preconditions.checkState(
        moderatorsDao.setRole(userId, UserRole.ADMIN) == 1,
        "Failed to add super moderator status for: " + username);
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorIdRequesting)
            .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_SUPER_MOD)
            .actionTarget(username)
            .build());
    log.info(username + " was promoted to super mod");
  }

  /** Checks if any user exists in DB by the given name. */
  boolean userExistsByName(final String username) {
    return userJdbiDao.lookupUserIdByName(username).isPresent();
  }
}
