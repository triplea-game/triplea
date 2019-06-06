package org.triplea.http.client.moderator.toolbox;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Bean class for transport between server and client. Represents one row of the moderator audit history table.
 */
@Builder
@Getter
@EqualsAndHashCode
@ToString
public class ModeratorEvent {
  @Nonnull
  private final Instant date;
  @Nonnull
  private final String moderatorName;
  @Nonnull
  private final String moderatorAction;
  @Nonnull
  private final String actionTarget;
}
