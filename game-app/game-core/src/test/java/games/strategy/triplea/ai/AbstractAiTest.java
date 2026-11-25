package games.strategy.triplea.ai;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMultimap;
import games.strategy.engine.data.AllianceTracker;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AbstractAiTest {

  @Test
  void proposalFromNonAllyIsNotAlwaysAcceptedJustBecauseOfClassicNationNames() {
    final GameData data = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final var britain = british(data);
    final var russia = russians(data);
    final var war = data.getRelationshipTypeList().getRelationshipType("War");
    data.performChange(
        ChangeFactory.relationshipChange(
            britain,
            russia,
            data.getRelationshipTracker().getRelationshipType(britain, russia),
            war));
    final var gameDataWithSplitAlliances = mock(GameData.class);
    when(gameDataWithSplitAlliances.getAllianceTracker())
        .thenReturn(
            new AllianceTracker(ImmutableMultimap.of(britain, "Western", russia, "Eastern")));
    final var bridge = mock(PlayerBridge.class);
    when(bridge.getGameData()).thenReturn(gameDataWithSplitAlliances);
    final var ai = new WeakAi("British");
    ai.initialize(bridge, britain);

    final boolean alwaysAccepted =
        IntStream.range(0, 200).allMatch(i -> ai.acceptAction(russia, "peace?", true));

    assertThat(alwaysAccepted).isFalse();
  }
}
