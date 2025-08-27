package games.strategy.triplea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.triplea.delegate.EditDelegate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
public class TripleATest {

  private static TripleA tripleA;

  @Mock private LaunchAction launchAction;
  @Mock private Chat chat;
  @Mock private ServerGame serverGame;
  @Mock private GameData gameData;
  @Mock private Set<Player> playerSet;
  @Mock private IDisplay display;
  @Mock private ISound sound;

  @BeforeAll
  public static void init() {
    tripleA = new TripleA();
  }

  @Test
  void testNewPlayersAreRetrievedFromGivenPlayerNames() {
    final Map<String, PlayerTypes.Type> playerNames = new HashMap<>();
    playerNames.put("first", PlayerTypes.FAST_AI);
    playerNames.put("second", PlayerTypes.WEAK_AI);
    playerNames.put("third", PlayerTypes.PRO_AI);
    final Set<Player> players = tripleA.newPlayers(playerNames);
    assertThat(players, hasSize(playerNames.size()));
  }

  @Test
  void testStartGameAndShutDownWhenServerGameStartedWithoutEditDelegate() {

    when(serverGame.getData()).thenReturn(gameData);
    doAnswer(
            invocation -> {
              IGame game = invocation.getArgument(1);
              game.setDisplay(display);
              game.setSoundChannel(sound);
              return null;
            })
        .when(launchAction)
        .startGame(any(LocalPlayers.class), any(IGame.class), anySet(), any(Chat.class));

    tripleA.startGame(serverGame, playerSet, launchAction, chat);
    verify(gameData).addDelegate(isA(EditDelegate.class));
    verify(serverGame).addDelegateMessenger(isA(EditDelegate.class));
    verify(serverGame).setDisplay(display);
    verify(serverGame).setSoundChannel(sound);

    tripleA.shutDown();
    verify(serverGame).setDisplay(isNull());
    verify(serverGame).setSoundChannel(isNull());
  }
}
