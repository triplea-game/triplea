package org.triplea.modules.game;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.triplea.db.data.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;

/**
 * Provides an endpoint that will attempt a 'reverse' connection back to a potential game host. This
 * is to verify that the potential host has a publicly available network address.
 */
@Builder
public class ConnectivityController extends HttpController {
  @Nonnull private final Predicate<InetSocketAddress> connectivityCheck;

  @POST
  @Path(ConnectivityCheckClient.CONNECTIVITY_CHECK_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.MINUTES)})
  @RolesAllowed(UserRole.HOST)
  public boolean checkConnectivity(@Context final HttpServletRequest request, final Integer port) {
    Preconditions.checkArgument(port > 0, "Port must be a positive number, was: " + port);
    Preconditions.checkArgument(
        port < Math.pow(2, 16), "Port must be less than max value (2^16), was: " + port);

    return connectivityCheck.test(new InetSocketAddress(request.getRemoteAddr(), port));
  }
}
