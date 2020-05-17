package org.triplea.modules.moderation.player.info;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.chat.Chatters;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RolesAllowed(UserRole.ANONYMOUS)
public class PlayerInfoController extends HttpController {

  private final BiFunction<AuthenticatedUser, PlayerChatId, PlayerSummary> fetchPlayerInfoAction;

  public static PlayerInfoController build(final Jdbi jdbi, final Chatters chatters) {
    return new PlayerInfoController(FetchPlayerInfoModule.build(jdbi, chatters));
  }

  @POST
  @Path(PlayerLobbyActionsClient.FETCH_PLAYER_INFORMATION)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 15, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public PlayerSummary fetchPlayerInfo(
      @Auth final AuthenticatedUser authenticatedUser, final String playerId) {
    Preconditions.checkNotNull(playerId);
    return fetchPlayerInfoAction.apply(authenticatedUser, PlayerChatId.of(playerId));
  }
}
