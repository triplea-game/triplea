package org.triplea.spitfire.server.controllers.lobby;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.GameHostingApiKeyDaoWrapper;
import org.triplea.domain.data.ApiKey;
import org.triplea.dropwizard.common.IpAddressExtractor;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingClient;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingResponse;
import org.triplea.spitfire.server.HttpController;

/**
 * Provides an endpoint where an independent connection can be established, provides an API key to
 * unauthenticated users that can then be used to post a game. Banning rules are verified to ensure
 * banned users cannot post games.
 */
@Builder
public class GameHostingController extends HttpController {

  @Nonnull private final Function<InetAddress, ApiKey> apiKeySupplier;

  public static GameHostingController build(final Jdbi jdbi) {
    final var gameHostingApiKeyDaoWrapper = GameHostingApiKeyDaoWrapper.build(jdbi);
    return GameHostingController.builder() //
        .apiKeySupplier(gameHostingApiKeyDaoWrapper::newGameHostKey)
        .build();
  }

  @POST
  @Path(GameHostingClient.GAME_HOSTING_REQUEST_PATH)
  public GameHostingResponse hostingRequest(@Context final HttpServletRequest request) {
    String remoteIp = IpAddressExtractor.extractIpAddress(request);
    try {
      return GameHostingResponse.builder()
          .apiKey(apiKeySupplier.apply(InetAddress.getByName(remoteIp)).getValue())
          .publicVisibleIp(remoteIp)
          .build();
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IP address in request: " + remoteIp, e);
    }
  }
}
