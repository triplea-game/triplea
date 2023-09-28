package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RulesAttachmentTest {

  @Mock private PlayerList playerList;

  private final GameData gameData = mock(GameData.class);

  private final RulesAttachment attachment = new RulesAttachment("Test attachment", null, gameData);

  @Mock private GamePlayer player1;
  @Mock private GamePlayer player2;
  @Mock private GamePlayer player3;
  @Mock private GamePlayer player4;

  @BeforeEach
  void setUp() {
    when(gameData.getPlayerList()).thenReturn(playerList);
  }

  /* Testing stored getIsAI values with setIsAI */
  @Test
  void isAITest() {
    attachment.setIsAI("true");
    assertTrue(attachment.getIsAI());
    attachment.setIsAI("false");
    assertFalse(attachment.getIsAI());
  }

  /* Testing returned values for checkGetIsAI */
  @Test
  void checkGetIsAITest() {
    lenient().when(player1.isAi()).thenReturn(false);
    lenient().when(player2.isAi()).thenReturn(true);
    lenient().when(player3.isAi()).thenReturn(true);
    lenient().when(player4.isAi()).thenReturn(false);

    /* Testing with 1 non AI player */
    final List<GamePlayer> players1 = List.of(player1);
    attachment.setIsAI("true");
    assertFalse(attachment.checkIsAI(players1));
    attachment.setIsAI("false");
    assertTrue(attachment.checkIsAI(players1));
    /* Testing with 1 AI player */
    final List<GamePlayer> players2 = List.of(player2);
    attachment.setIsAI("true");
    assertTrue(attachment.checkIsAI(players2));
    attachment.setIsAI("false");
    assertFalse(attachment.checkIsAI(players2));
    /* Testing with 1 non AI player and 1 AI player */
    final List<GamePlayer> players12 = List.of(player1, player2);
    attachment.setIsAI("true");
    assertFalse(attachment.checkIsAI(players12));
    attachment.setIsAI("false");
    assertFalse(attachment.checkIsAI(players12));
    /* Testing with 2 AI players */
    final List<GamePlayer> players23 = List.of(player2, player3);
    attachment.setIsAI("true");
    assertTrue(attachment.checkIsAI(players23));
    attachment.setIsAI("false");
    assertFalse(attachment.checkIsAI(players23));
    /* Testing with 2 non AI players */
    final List<GamePlayer> players14 = List.of(player1, player4);
    attachment.setIsAI("true");
    assertFalse(attachment.checkIsAI(players14));
    attachment.setIsAI("false");
    assertTrue(attachment.checkIsAI(players14));
  }
}
