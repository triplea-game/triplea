package org.triplea.http.client.moderator.toolbox;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
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
