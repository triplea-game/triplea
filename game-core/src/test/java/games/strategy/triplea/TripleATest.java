package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TripleATest {

  private static final String DELEGATE_NAME_EDIT = "edit";

  @Mock private LaunchAction launchAction;
  @Mock private Chat chat;
  @Mock private ServerGame serverGame;
  @Mock private GameData gameData;

  @Test
  void testStartGameWhenServerGameStartedWithNewPlayersWithoutEditDelegate() {

    final TripleA tripleA = new TripleA();
    final Map<String, PlayerType> playerNames = new HashMap<>();
    playerNames.put("first", PlayerType.HUMAN_PLAYER);
    playerNames.put("second", PlayerType.WEAK_AI);
    playerNames.put("third", PlayerType.PRO_AI);

    when(serverGame.getData()).thenReturn(gameData);
    when(gameData.getDelegate(DELEGATE_NAME_EDIT)).thenReturn(null);

    final Set<Player> players = tripleA.newPlayers(playerNames);
    assertThat(players, hasSize(3));

    tripleA.startGame(serverGame, players, launchAction, chat);
    verify(gameData).addDelegate(any());
    verify(serverGame).addDelegateMessenger(any());
    verify(serverGame).setDisplay(any());
    verify(serverGame).setSoundChannel(any());

    tripleA.shutDown();
    verify(serverGame, times(2)).setDisplay(any());
    verify(serverGame, times(2)).setSoundChannel(any());
  }
}
