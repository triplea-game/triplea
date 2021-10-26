package org.triplea.spitfire.server.controllers.latest.version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.latest.version.LatestVersionClient;
import org.triplea.http.client.latest.version.LatestVersionResponse;
import org.triplea.modules.latest.version.LatestVersionModule;
import org.triplea.spitfire.server.HttpController;

@Builder
public class LatestVersionController extends HttpController {
  private final LatestVersionModule latestVersionModule;

  public static LatestVersionController build(final LatestVersionModule latestVersionModule) {
    return new LatestVersionController(latestVersionModule);
  }

  @GET
  @Path(LatestVersionClient.LATEST_VERSION_PATH)
  public Response getLatestEngineVersion() {
    return latestVersionModule
        .getLatestVersion()
        .map(
            latest ->
                Response.ok(
                        LatestVersionResponse.builder() //
                            .latestEngineVersion(latest)
                            .releaseNotesUrl("https://triplea-game.org/release_notes/")
                            .downloadUrl("https://triplea-game.org/download/")
                            .build())
                    .build())
        .orElseGet(() -> Response.status(503, "Unable to fetch latest version").build());
  }
}
