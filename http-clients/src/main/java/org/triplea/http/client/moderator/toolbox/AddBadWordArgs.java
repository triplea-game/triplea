package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Parameter class with parameters needed to add a new bad-word entry.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
public class AddBadWordArgs {
  @Nonnull
  private final String apiKey;
  @Nonnull
  private final String badWord;
}
