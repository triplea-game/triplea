package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Parameter class for removing an entry from the bad word table.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
public class UpdateBadWordsArg {
  @Nonnull
  private final String apiKey;
  @Nonnull
  private final String badWord;
}
