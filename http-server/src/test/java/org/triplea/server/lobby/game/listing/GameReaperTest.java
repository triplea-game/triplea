package org.triplea.server.lobby.game.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameReaperTest {

  private static final String ID0 = "id0";
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";

  private static final Collection<GameListing.GameId> ids =
      Arrays.asList(newGameId(ID0), newGameId(ID1), newGameId(ID2));

  @Mock private Cache<String, Boolean> cache;

  private GameReaper gameReaper;

  private static GameListing.GameId newGameId(final String id) {
    return GameListing.GameId.builder().id(id).apiKey("key").build();
  }

  @BeforeEach
  void setup() {
    gameReaper = new GameReaper(cache);
  }

  @Nested
  class FindDeadGames {
    @Test
    void nothingInCache() {
      when(cache.getIfPresent(ID0)).thenReturn(null);
      when(cache.getIfPresent(ID1)).thenReturn(null);
      when(cache.getIfPresent(ID2)).thenReturn(null);

      final Collection<GameListing.GameId> results = gameReaper.findDeadGames(ids);

      assertThat(results, hasSize(3));
    }

    @Test
    void oneElementInCache() {
      when(cache.getIfPresent(ID0)).thenReturn(null);
      when(cache.getIfPresent(ID1)).thenReturn(true);
      when(cache.getIfPresent(ID2)).thenReturn(null);

      final Collection<GameListing.GameId> results = gameReaper.findDeadGames(ids);

      assertThat(results, hasSize(2));
      assertThat(results, hasItem(newGameId(ID0)));
      assertThat(results, hasItem(newGameId(ID2)));
    }

    @Test
    void allElementsInCache() {
      when(cache.getIfPresent(ID0)).thenReturn(true);
      when(cache.getIfPresent(ID1)).thenReturn(true);
      when(cache.getIfPresent(ID2)).thenReturn(true);

      final Collection<GameListing.GameId> results = gameReaper.findDeadGames(ids);

      assertThat(results, hasSize(0));
    }
  }

  @Test
  void registerKeepAlive() {
    gameReaper.registerKeepAlive(ID0);

    verify(cache).put(ID0, true);
  }
}
