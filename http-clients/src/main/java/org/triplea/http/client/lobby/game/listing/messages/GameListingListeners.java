package org.triplea.http.client.lobby.game.listing.messages;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;

/**
 * Register an instance of this class to a {@code GameListingClient} to receive callbacks when the
 * server receives game listing events. Members of this class are the callback implementations that
 * are notified on game listing events.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
public class GameListingListeners {
  @Nonnull private final Consumer<String> gameRemoved;
  @Nonnull private final Consumer<LobbyGameListing> gameUpdated;
}
