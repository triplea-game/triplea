package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Tuple;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Named;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ProductionRuleList;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;

@ExtendWith(MockitoExtension.class)
class TriggerAttachmentTest {

  @Nested
  class TriggerChangeTest {

    @Mock
    private IDelegateBridge bridge;
    @Mock
    private IDelegateHistoryWriter historyWriter;

    @BeforeEach
    void setUp() {
      final GameData gameData = new GameData();

      when(bridge.getData()).thenReturn(gameData);
      when(bridge.getHistoryWriter()).thenReturn(historyWriter);
    }

    @Test
    void testTriggerProductionFrontierEditChange() {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getProductionRule())
          .thenReturn(Arrays.asList("frontier:rule1", "frontier:-rule2", "frontier:rule3"));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final ProductionRuleList productionRuleList = gameData.getProductionRuleList();
      productionRuleList.addProductionRule(new ProductionRule("rule1", gameData));
      final ProductionRule productionRule2 = new ProductionRule("rule2", gameData);
      productionRuleList.addProductionRule(productionRule2);
      productionRuleList.addProductionRule(new ProductionRule("rule3", gameData));

      final ProductionFrontierList productionFrontierList = gameData.getProductionFrontierList();
      productionFrontierList
          .addProductionFrontier(
              new ProductionFrontier("frontier", gameData, Collections.singletonList(productionRule2)));

      TriggerAttachment.triggerProductionFrontierEditChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
      final ArgumentCaptor<String> ruleAddArgument = ArgumentCaptor.forClass(String.class);
      verify(historyWriter, times(3)).startEvent(ruleAddArgument.capture());
      final List<String> allValues = ruleAddArgument.getAllValues();
      assertEquals(3, allValues.size());
      assertTrue(allValues.stream().anyMatch(s -> s.contains("rule1") && s.contains("added")));
      assertTrue(allValues.stream().anyMatch(s -> s.contains("rule2") && s.contains("removed")));
      assertTrue(allValues.stream().anyMatch(s -> s.contains("rule3") && s.contains("added")));
      assertTrue(allValues.stream().allMatch(s -> s.contains("frontier")));
    }

    @Test
    void testTriggerPlayerPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getPropertyMap()).thenCallRealMethod();
      triggerAttachment.getPropertyMap().get("playerAttachmentName").setValue("rulesAttachment:RulesAttachment");
      when(triggerAttachment.getPlayerProperty())
          .thenReturn(Collections.singletonList(
              Tuple.of("productionPerXTerritories", "someNewValue")));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final PlayerId playerId = mock(PlayerId.class);
      when(playerId.getAttachment("rulesAttachment"))
          .thenReturn(new RulesAttachment(null, null, gameData));
      when(triggerAttachment.getPlayers()).thenReturn(Collections.singletonList(playerId));

      TriggerAttachment.triggerPlayerPropertyChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerRelationshipTypePropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getPropertyMap()).thenCallRealMethod();
      triggerAttachment.getPropertyMap().get("relationshipTypeAttachmentName")
          .setValue("relationshipTypeAttachment:RelationshipTypeAttachment");
      when(triggerAttachment.getRelationshipTypeProperty())
          .thenReturn(Collections.singletonList(
              Tuple.of("canMoveLandUnitsOverOwnedLand", "true")));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final RelationshipType relationshipType = mock(RelationshipType.class);
      when(relationshipType.getAttachment("relationshipTypeAttachment"))
          .thenReturn(new RelationshipTypeAttachment(null, null, gameData));
      when(triggerAttachment.getRelationshipTypes()).thenReturn(Collections.singletonList(relationshipType));

      TriggerAttachment.triggerRelationshipTypePropertyChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTerritoryPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getPropertyMap()).thenCallRealMethod();
      triggerAttachment.getPropertyMap().get("territoryAttachmentName")
          .setValue("territoryAttachment:TerritoryAttachment");
      when(triggerAttachment.getTerritoryProperty())
          .thenReturn(Collections.singletonList(
              Tuple.of("kamikazeZone", "true")));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final Territory territory = mock(Territory.class);
      when(territory.getAttachment("territoryAttachment"))
          .thenReturn(new TerritoryAttachment(null, null, gameData));
      when(triggerAttachment.getTerritories()).thenReturn(Collections.singletonList(territory));

      TriggerAttachment.triggerTerritoryPropertyChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTerritoryEffectPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getPropertyMap()).thenCallRealMethod();
      triggerAttachment.getPropertyMap().get("territoryEffectAttachmentName")
          .setValue("territoryEffectAttachment:TerritoryEffectAttachment");
      when(triggerAttachment.getTerritoryEffectProperty())
          .thenReturn(Collections.singletonList(
              Tuple.of("unitsNotAllowed", "conscript:veteran:champion")));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final TerritoryEffect territoryEffect = mock(TerritoryEffect.class);
      when(territoryEffect.getAttachment("territoryEffectAttachment"))
          .thenReturn(new TerritoryEffectAttachment(null, null, gameData));
      when(triggerAttachment.getTerritoryEffects()).thenReturn(Collections.singletonList(territoryEffect));

      TriggerAttachment.triggerTerritoryEffectPropertyChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerUnitPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getPropertyMap()).thenCallRealMethod();
      triggerAttachment.getPropertyMap().get("unitAttachmentName")
          .setValue("unitAttachment:UnitAttachment");
      when(triggerAttachment.getUnitProperty())
          .thenReturn(Collections.singletonList(
              Tuple.of("movement", "4")));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final UnitType unitType = mock(UnitType.class);
      when(unitType.getAttachment("unitAttachment"))
          .thenReturn(new UnitAttachment(null, null, gameData));
      when(triggerAttachment.getUnitType()).thenReturn(Collections.singletonList(unitType));

      TriggerAttachment.triggerUnitPropertyChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }
  }

  @Nested
  class ClearFirstNewValueTest {

    @Test
    void testClear() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("-clear-");
      assertTrue(r.getFirst());
      assertTrue(r.getSecond().isEmpty());
    }

    @Test
    void testReset() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("-reset-");
      assertTrue(r.getFirst());
      assertTrue(r.getSecond().isEmpty());
    }

    @Test
    void testClearAndValue() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("-clear-4:conscript");
      assertTrue(r.getFirst());
      assertEquals(r.getSecond(), "4:conscript");
    }

    @Test
    void testNoClear() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("clearValueWithoutDash");
      assertFalse(r.getFirst());
      assertEquals(r.getSecond(), "clearValueWithoutDash");
    }

    @Test
    void testEmpty() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("");
      assertFalse(r.getFirst());
      assertEquals(r.getSecond(), "");
    }

    @Test
    void testInnerClear() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("clearValue-clear-");
      assertFalse(r.getFirst());
      assertEquals(r.getSecond(), "clearValue-clear-");
    }

    @Test
    void testInnerReset() {
      final Tuple<Boolean, String> r = TriggerAttachment.getClearFirstNewValue("clearValue-reset-");
      assertFalse(r.getFirst());
      assertEquals(r.getSecond(), "clearValue-reset-");
    }
  }

  @Nested
  class GetPropertyChangeHistoryStartEventTest {

    private Optional<Tuple<Change, String>> applyWithOldNewValue(
        final String startValue, final String newValue) {

      final TriggerAttachment triggerAttachment = new TriggerAttachment("aTriggerAName", null, null);
      final TestAttachment propertyAttachment = new TestAttachment("aTestAName", null, null);
      propertyAttachment.setValue(startValue);
      final Named attachedTo = mock(Named.class);

      return TriggerAttachment.getPropertyChangeHistoryStartEvent(
          triggerAttachment, propertyAttachment,
          "value", // Property name in 'TestAttachment'. Authentic name: "productionPerXTerritories".
          Tuple.of(true, newValue),
          "rulesAttachment", attachedTo);
    }

    @Test
    void testNewValue() {
      final Optional<Tuple<Change, String>> r = applyWithOldNewValue("2:conscript", "4:conscript");

      assertTrue(r.isPresent());
      assertFalse(r.get().getFirst().isEmpty());
      assertFalse(r.get().getSecond().isEmpty());
    }

    @Test
    void testEmptyToNew() {
      final Optional<Tuple<Change, String>> r = applyWithOldNewValue("", "4:conscript");

      assertTrue(r.isPresent());
      assertFalse(r.get().getFirst().isEmpty());
      assertFalse(r.get().getSecond().isEmpty());
    }

    @Test
    void testSameValue() {
      final Optional<Tuple<Change, String>> r = applyWithOldNewValue("4:conscript", "4:conscript");

      assertFalse(r.isPresent());
    }

    @Test
    void testOldToEmpty() {
      final Optional<Tuple<Change, String>> r = applyWithOldNewValue("4:conscript", "");

      assertTrue(r.isPresent());
      assertFalse(r.get().getFirst().isEmpty());
      assertFalse(r.get().getSecond().isEmpty());
    }

    @Test
    void testEmptyToEmpty() {
      final Optional<Tuple<Change, String>> r = applyWithOldNewValue("", "");

      assertFalse(r.isPresent());
    }
  }
}
