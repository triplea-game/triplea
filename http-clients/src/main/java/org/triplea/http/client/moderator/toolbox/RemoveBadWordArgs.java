package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter(AccessLevel.PACKAGE)
public class RemoveBadWordArgs {
  @Nonnull
  private final String apiKey;
  @Nonnull
  private final String badWord;
}
