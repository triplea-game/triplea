package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.gameparser.GameParseException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RulesAttachmentTest {

  private final GameData gameData = mock(GameData.class);

  private final RulesAttachment attachment = new RulesAttachment("Test attachment", null, gameData);

  @Mock private PlayerList playerList;

  @Mock private GamePlayer player1;
  @Mock private GamePlayer player2;
  @Mock private GamePlayer player3;
  @Mock private GamePlayer player4;

  @BeforeEach
  void setUp() {
    when(gameData.getPlayerList()).thenReturn(playerList);
  }

  @Nested
  class hasResource {

    @Mock private ResourceList resourceList;

    private final String player1String = "Player1";
    private final String resource1String = "Resource1";

    @Mock private Resource  resource1;

    @BeforeEach
    void setUp() {
      when(gameData.getResourceList()).thenReturn(resourceList);
      lenient().when(playerList.getPlayerId(player1String)).thenReturn(player1);
      lenient().when(resourceList.getResource(resource1String)).thenReturn(resource1);
    }

    /* Testing setHaveResources with invalid arguments */
    @Test
    void setHasResourceInvalidArgs() {
      assertThrows(
              IllegalArgumentException.class,
              () -> attachment.setHasResource("NOT A NUMBER:resource1String"));
      verify(resourceList, times(0)).getResource(resource1String);
      assertThrows(
              GameParseException.class,
              () -> attachment.setHasResource("0:resource1String"));
      verify(resourceList, times(0)).getResource(resource1String);
      assertThrows(
              GameParseException.class,
              () -> attachment.setHasResource("1:NOT A RESOURCE"));
      verify(resourceList).getResource("NOT A RESOURCE");
      assertThrows(
              IllegalArgumentException.class, () -> attachment.setHasResource("q:w"));
    }

    private String concatWithColon(final String... args) {
      return String.join(":", args);
    }

  }

  @Nested
  class isAI {

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
}
