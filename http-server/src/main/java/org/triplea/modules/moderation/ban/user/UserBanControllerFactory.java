package org.triplea.modules.moderation.ban.user;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.modules.moderation.remote.actions.RemoteActionsEventQueue;

/** Factory class, instantiates {@code BannedUsersController}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserBanControllerFactory {

  public static UserBanController buildController(
      final Jdbi jdbi,
      final Chatters chatters,
      final RemoteActionsEventQueue remoteActionsEventQueue) {
    return UserBanController.builder()
        .bannedUsersService(UserBanService.build(jdbi, chatters, remoteActionsEventQueue))
        .build();
  }
}
