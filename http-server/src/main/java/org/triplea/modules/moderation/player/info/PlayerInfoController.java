package org.triplea.modules.moderation.player.info;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RolesAllowed(UserRole.MODERATOR)
public class PlayerInfoController extends HttpController {

  private final Function<PlayerChatId, PlayerSummaryForModerator> fetchPlayerInfoAction;

  public static PlayerInfoController build(final Jdbi jdbi) {
    return new PlayerInfoController(FetchPlayerInfoModule.build(jdbi));
  }

  @POST
  @Path(ModeratorChatClient.FETCH_PLAYER_INFORMATION)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 15, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public PlayerSummaryForModerator fetchPlayerInfo(
      @Auth final AuthenticatedUser authenticatedUser, final String playerId) {
    Preconditions.checkNotNull(playerId);
    return fetchPlayerInfoAction.apply(PlayerChatId.of(playerId));
  }
}
