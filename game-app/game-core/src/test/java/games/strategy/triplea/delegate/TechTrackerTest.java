package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
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
import games.strategy.engine.data.UnitTypeList;
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
  private final TechTracker techTracker = new TechTracker(data);
  private final TechAbilityAttachment attachment =
      spy(new TechAbilityAttachment("", new NamedAttachable("test", data), data));
  private final UnitTypeList list = mock(UnitTypeList.class);
  private final UnitType dummyUnitType = mock(UnitType.class);
  private final String customToString = "CustomToString";
  private final String testUnitType = "someExistentKey";
  private Collection<TechAdvance> techAdvances;

  @BeforeEach
  void setUp() {
    when(attachment.toString()).thenReturn(customToString);
    when(data.getUnitTypeList()).thenReturn(list);
    when(list.getUnitType(testUnitType)).thenReturn(dummyUnitType);
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
    final int result = techTracker.sumIntegerMap(mapper, dummyUnitType, techAdvances);
    assertEquals(319, result);
  }

  @Test
  void sumNumbers() {
    final AtomicInteger counter = new AtomicInteger(1);
    final int result =
        techTracker.sumNumbers(
            a -> {
              assertEquals(attachment, a);
              return counter.getAndUpdate(i -> i * -10);
            },
            "NamedAttachable{name=test}",
            techAdvances);
    assertThat(result, is(101));
  }

  @Test
  void invalidateCache() {
    // Test that updating techs updates the TechTracker's cache.
    GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
    TechTracker techTracker = gameData.getTechTracker();
    IDelegateBridge bridge = newDelegateBridge(americans(gameData));
    GamePlayer player = americans(gameData);
    TechnologyFrontier technologyFrontier = gameData.getTechnologyFrontier();

    TechAdvance heavyBomber = technologyFrontier.getAdvanceByName("Heavy Bomber");
    assertThat(heavyBomber, is(notNullValue()));

    // Check that modifying tech via tech tracker (via Change objects), invalidates the cache.
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));
    techTracker.addAdvance(player, bridge, heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(1));
    techTracker.removeAdvance(player, bridge, heavyBomber);
    assertThat(techTracker.getAttackRollsBonus(player, bomber(gameData)), is(0));

    // Check that modifying the tech frontier also invalidates the cache.
    techTracker.addAdvance(player, bridge, heavyBomber);
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
}
