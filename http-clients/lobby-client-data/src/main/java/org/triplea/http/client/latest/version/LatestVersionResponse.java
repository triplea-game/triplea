package org.triplea.http.client.latest.version;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LatestVersionResponse {

  /**
   * String value of RecommendedAction enum indicating whether the client is up to date, or if an
   * update is recommended. or required
   */
  @Nonnull private final String recommendedUpdateAction;

  /** HTML message to show to the user when prompting them to upgrade */
  @Nonnull private final String upgradeMessageHtml;

  /** URL of the page to open where the latest version can be downloaded */
  @Nonnull private final String downloadPageUrl;

  public enum RecommendedAction {
    NO_UPDATE,
    UPDATE_RECOMMENDED,
  }
}
