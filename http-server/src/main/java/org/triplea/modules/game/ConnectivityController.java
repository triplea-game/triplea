package org.triplea.modules.game;

import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.eclipse.jetty.http.HttpStatus;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.game.listing.GameListing;

/**
 * Provides an endpoint that will attempt a 'reverse' connection back to a potential game host. This
 * is to verify that the potential host has a publicly available network address.
 */
@Builder
public class ConnectivityController extends HttpController {
  @Nonnull private final ConnectivityCheck connectivityCheck;

  public static ConnectivityController build(final GameListing gameListing) {
    return ConnectivityController.builder() //
        .connectivityCheck(new ConnectivityCheck(gameListing))
        .build();
  }

  @POST
  @Path(ConnectivityCheckClient.CONNECTIVITY_CHECK_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.MINUTES)})
  @RolesAllowed(UserRole.HOST)
  public Response checkConnectivity(
      @Auth final AuthenticatedUser authenticatedUser, final String gameId) {

    final ConnectivityCheck.ReverseConnectionResult result =
        connectivityCheck.canDoReverseConnect(authenticatedUser.getApiKey(), gameId);

    switch (result) {
      case SUCCESS:
        return Response.ok().entity(true).build();
      case FAILED:
        return Response.ok().entity(false).build();
      case GAME_ID_NOT_FOUND:
        return Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422).build();
      default:
        throw new IllegalStateException("Switch case not handled: " + result);
    }
  }
}
