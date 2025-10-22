package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

public class TechTrackerTest {
  private final GameData data = mock(GameData.class);
  private final TechAbilityAttachment attachment =
      spy(new TechAbilityAttachment("", new NamedAttachable("test", data), data));
  private final UnitType dummyUnitType = mock(UnitType.class);
  private Collection<TechAdvance> techAdvances;

  @BeforeEach
  void setUp() {
    final TechAdvance advance = mock(TechAdvance.class);
    techAdvances = List.of(advance, advance, advance, advance);
    when(advance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME))
        .thenReturn(attachment, null, attachment, attachment);
  }

  @Test
  void sumIntegerMap() {
    @SuppressWarnings("unchecked")
    final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper = mock(Function.class);
    doReturn(
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, -1)),
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, 20)),
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, 300)))
        .when(mapper)
        .apply(attachment);
    final int result = TechTracker.sumIntegerMap(mapper, dummyUnitType, techAdvances);
    assertEquals(319, result);
  }

  @Test
  void sumNumbers() {
    final AtomicInteger counter = new AtomicInteger(1);
    final int result =
        TechTracker.sumNumbers(
            a -> {
              assertEquals(attachment, a);
              return counter.getAndUpdate(i -> i * -10);
            },
            "NamedAttachable{name=test}",
            techAdvances);
    assertThat(result, is(101));
  }

  @Test
  void clearCache() {
    // Test that updating techs updates the TechTracker's cache.
    GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
    TechTracker techTracker = gameData.getTechTracker();
    GamePlayer player = americans(gameData);
    IDelegateBridge bridge = newDelegateBridge(player);
    TechnologyFrontier technologyFrontier = gameData.getTechnologyFrontier();

    TechAdvance heavyBomber =
        technologyFrontier.getAdvanceByPropertyOrName("Heavy Bomber").orElse(null);
    assertThat(heavyBomber, is(notNullValue()));

    // Check that modifying tech via tech tracker (via Change objects), invalidates the cache.
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));
    TechTracker.addAdvance(player, bridge, heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(1));
    TechTracker.removeAdvance(player, bridge, heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));

    // Check that modifying the tech frontier also invalidates the cache.
    TechTracker.addAdvance(player, bridge, heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(1));
    technologyFrontier.removeAdvance(heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));
    technologyFrontier.addAdvance(heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(1));
    technologyFrontier.removeAdvance(heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));
    technologyFrontier.addAdvance(List.of(heavyBomber));
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(1));

    // Check that modifying relevant attachments also invalidates the cache.
    TechAbilityAttachment taa = TechAbilityAttachment.get(heavyBomber);
    Change change = ChangeFactory.attachmentPropertyChange(taa, "2:bomber", "attackRollsBonus");
    gameData.performChange(change);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(2));

    TechAttachment ta = player.getTechAttachment();
    change = ChangeFactory.attachmentPropertyChange(ta, "false", "heavyBomber");
    gameData.performChange(change);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));
    change = ChangeFactory.attachmentPropertyChange(ta, "true", "heavyBomber");
    gameData.performChange(change);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(2));
  }

  @Test
  void getFullyResearchedPlayerTechCategories() {
    GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
    GamePlayer player = americans(gameData);
    IDelegateBridge bridge = newDelegateBridge(player);
    assertThat(TechTracker.getFullyResearchedPlayerTechCategories(player), is(empty()));

    // Add one advance, there should still be nothing fully researched.
    final List<TechnologyFrontier> allAdvances = TechAdvance.getPlayerTechCategories(player);
    TechTracker.addAdvance(player, bridge, allAdvances.get(0).getTechs().get(0));
    assertThat(TechTracker.getFullyResearchedPlayerTechCategories(player), is(empty()));

    // Add the remaining advances from that category.
    for (int i = 1; i < allAdvances.get(0).getTechs().size(); i++) {
      TechTracker.addAdvance(player, bridge, allAdvances.get(0).getTechs().get(i));
    }
    assertThat(
        TechTracker.getFullyResearchedPlayerTechCategories(player),
        containsInAnyOrder(allAdvances.get(0)));
  }
}
