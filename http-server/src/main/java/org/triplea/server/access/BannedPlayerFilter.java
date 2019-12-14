package org.triplea.server.access;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.time.Clock;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.lobby.server.db.dao.user.ban.BanLookupRecord;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;

@Provider
@PreMatching
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class BannedPlayerFilter implements ContainerRequestFilter {

  private final UserBanDao userBanDao;
  private final Clock clock;

  @Context private HttpServletRequest request;

  public static BannedPlayerFilter newBannedPlayerFilter(final Jdbi jdbi) {
    return new BannedPlayerFilter(jdbi.onDemand(UserBanDao.class), Clock.systemUTC());
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (Strings.emptyToNull(request.getHeader(SystemIdHeader.SYSTEM_ID_HEADER)) == null) {
      // missing system id header, abort the request
      requestContext.abortWith(
          Response.status(Status.UNAUTHORIZED).entity("Invalid request").build());

    } else {
      // check if user is banned, if so abort the request
      userBanDao
          .lookupBan(request.getRemoteAddr(), request.getHeader(SystemIdHeader.SYSTEM_ID_HEADER))
          .map(this::formatBanMessage)
          .ifPresent(
              banMessage ->
                  requestContext.abortWith(
                      Response.status(Status.UNAUTHORIZED).entity(banMessage).build()));
    }
  }

  private String formatBanMessage(final BanLookupRecord banLookupRecord) {
    final long banMinutes =
        Duration.between(clock.instant(), banLookupRecord.getBanExpiry()).toMinutes();
    final String banDuration = BanDurationFormatter.formatBanMinutes(banMinutes);

    return String.format("Banned %s, (ID: %s)", banDuration, banLookupRecord.getPublicBanId());
  }
}
