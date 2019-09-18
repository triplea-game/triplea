package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.common.GameDescription;

@ExtendWith(MockitoExtension.class)
final class GameListModelTest {
  private GameListModel gameListModel = new GameListModel();

  private final UUID gameId = UUID.randomUUID();
  private final GameDescription gameDescription = GameDescription.builder().build();
  private final GameDescription newDescription =
      GameDescription.builder().comment("comment").build();

  @BeforeEach
  void setUp() {
    gameListModel.add(gameId, gameDescription);
    assertThat("games are loaded on init", gameListModel.size(), is(1));
  }

  @Test
  void updateGame() {
    assertThat(gameListModel.getGameDescriptionByRow(0).getComment(), nullValue());

    gameListModel.update(gameId, newDescription);

    assertThat(gameListModel.size(), is(1));
    assertThat(
        gameListModel.getGameDescriptionByRow(0).getComment(), is(newDescription.getComment()));
  }

  @Test
  void containsGame() {
    assertThat(gameListModel.containsGame(UUID.randomUUID()), is(false));
    assertThat(gameListModel.containsGame(gameId), is(true));
  }

  @Test
  void addGame() {
    gameListModel.add(UUID.randomUUID(), GameDescription.builder().build());
    assertThat(gameListModel.size(), is(2));
  }

  @Test
  void removeGame() {
    gameListModel.removeGame(gameId);
    assertThat(gameListModel.size(), is(0));
  }

  @Test
  void removeGameThatDoesNotExistIsIgnored() {
    gameListModel.removeGame(UUID.randomUUID());
    assertThat(gameListModel.size(), is(1));
  }
}
