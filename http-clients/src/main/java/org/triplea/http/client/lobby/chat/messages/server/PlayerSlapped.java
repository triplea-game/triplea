package org.triplea.http.client.lobby.chat.messages.server;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.domain.data.PlayerName;

/** Payload indicating someone slapped another other player (that was not the local player). */
@Builder
@Value
public class PlayerSlapped {
  @Nonnull private final PlayerName slapper;
  @Nonnull private final PlayerName slapped;
}
