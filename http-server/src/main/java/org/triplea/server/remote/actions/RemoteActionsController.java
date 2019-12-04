package org.triplea.server.remote.actions;

import com.google.common.base.Preconditions;
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
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.remote.actions.RemoteActionsClient;
import org.triplea.java.ArgChecker;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;
import org.triplea.server.http.HttpController;

/**
 * Endpoints for moderators to use to issue remote action commands that affect game-hosts, eg:
 * requesting a server to shutdown.
 */
@Builder
public class RemoteActionsController extends HttpController {
  @Nonnull private final RemoteActionsModule remoteActionsModule;

  @POST
  @Path(RemoteActionsClient.SEND_SHUTDOWN_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response sendShutdownSignal(
      @Auth final AuthenticatedUser authenticatedUser, final String ip) {
    Preconditions.checkArgument(IpAddressParser.isValid(ip));

    remoteActionsModule.addIpForShutdown(
        authenticatedUser.getUserIdOrThrow(), IpAddressParser.fromString(ip));
    return Response.ok().build();
  }

  @POST
  @Path(RemoteActionsClient.IS_PLAYER_BANNED_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 60, duration = 1, timeUnit = TimeUnit.MINUTES)})
  @RolesAllowed(UserRole.HOST)
  public Response isUserBanned(final String ipAddress) {
    ArgChecker.checkNotEmpty(ipAddress);
    Preconditions.checkArgument(IpAddressParser.isValid(ipAddress));

    final boolean result = remoteActionsModule.isUserBanned(IpAddressParser.fromString(ipAddress));

    return Response.ok().entity(result).build();
  }
}
