package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Parameter class for the parameters needed when querying moderator audit history events.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
@EqualsAndHashCode
@ToString
public class LookupModeratorEventsArgs {
  @Nonnull
  private final String apiKey;
  @Nonnull
  private final Integer rowStart;
  @Nonnull
  private final Integer rowCount;
}
