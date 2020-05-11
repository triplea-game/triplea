package org.triplea.http.client.lobby.moderator.toolbox.log;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/**
 * Bean class for transport between server and client. Represents one row of the moderator audit
 * history table.
 */
@Builder
@Value
public class ModeratorEvent {
  @Nonnull private final Long date;
  @Nonnull private final String moderatorName;
  @Nonnull private final String moderatorAction;
  @Nonnull private final String actionTarget;
}
