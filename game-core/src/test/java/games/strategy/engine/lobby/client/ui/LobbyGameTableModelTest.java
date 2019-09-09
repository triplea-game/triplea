package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import games.strategy.net.GUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.common.GameDescription;

// TODO: rename to GameListModelTest
@ExtendWith(MockitoExtension.class)
final class LobbyGameTableModelTest {
  private GameListModel gameListModel = new GameListModel();

  private final GUID gameGuid = new GUID();
  private final GameDescription gameDescription = GameDescription.builder().build();
  private final GameDescription newDescription =
      GameDescription.builder().comment("comment").build();

  @BeforeEach
  void setUp() {
    gameListModel.add(gameGuid, gameDescription);
    assertThat("games are loaded on init", gameListModel.size(), is(1));
  }

  @Test
  void updateGame() {
    assertThat(gameListModel.getGameDescriptionByRow(0).getComment(), nullValue());

    gameListModel.update(gameGuid, newDescription);

    assertThat(gameListModel.size(), is(1));
    assertThat(
        gameListModel.getGameDescriptionByRow(0).getComment(), is(newDescription.getComment()));
  }

  @Test
  void containsGame() {
    assertThat(gameListModel.containsGame(new GUID()), is(false));
    assertThat(gameListModel.containsGame(gameGuid), is(true));
  }

  @Test
  void addGame() {
    gameListModel.add(new GUID(), GameDescription.builder().build());
    assertThat(gameListModel.size(), is(2));
  }

  @Test
  void updateGameWithNullGuidIsIgnored() {
    gameListModel.update(null, GameDescription.builder().build());
    assertThat(
        "expect row count to remain 1, null guid is bogus data", gameListModel.size(), is(1));
  }

  @Test
  void removeGame() {
    gameListModel.removeGame(gameGuid);
    assertThat(gameListModel.size(), is(0));
  }

  @Test
  void removeGameThatDoesNotExistIsIgnored() {
    gameListModel.removeGame(new GUID());
    assertThat(gameListModel.size(), is(1));
  }
}
