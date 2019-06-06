package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter(AccessLevel.PACKAGE)
public class LookupModeratorEventsArgs {
  @Nonnull
  private final String apiKey;
  @Nonnull
  private final Integer rowStart;
  @Nonnull
  private final Integer rowCount;
}
