package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Named;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ProductionRuleList;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.ui.NotificationMessages;
import games.strategy.triplea.ui.display.ITripleADisplay;

@ExtendWith(MockitoExtension.class)
class TriggerAttachmentTest {

  @Nested
  class TriggerFireTest {

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
    void testTriggerNotifications() {
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      final String notificationMessageKey = "BlackIce";
      final String notificationMessage =
          "<body><h2>The Land of Black Ice</h2>Whether out of duty, ...<br>never heard from again.</body>";

      when(triggerAttachment.getNotification()).thenReturn(notificationMessageKey);
      final PlayerId playerId = mock(PlayerId.class);
      when(playerId.getName()).thenReturn("somePlayerName");
      when(triggerAttachment.getPlayers()).thenReturn(Collections.singletonList(playerId));

      final ITripleADisplay display = mock(ITripleADisplay.class);
      when(bridge.getDisplayChannelBroadcaster()).thenReturn(display);

      final NotificationMessages notificationMessages = mock(NotificationMessages.class);
      when(notificationMessages.getMessage(notificationMessageKey)).thenReturn(notificationMessage);

      TriggerAttachment.triggerNotifications(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false, // testWhen
          notificationMessages);
      verify(display).reportMessageToPlayers(
          not(argThat(Collection::isEmpty)), // Players.
          any(),
          argThat(htmlMessage -> htmlMessage.contains(notificationMessage)),
          any());
    }

    // TODO: Should this be moved down so as to fit with the order of definition of methods in TriggerAttachment?
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

    @Test
    void testTriggerRelationshipChange() {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      when(triggerAttachment.getRelationshipChange())
          .thenReturn(Collections.singletonList("Keoland:Furyondy:any:allied"));
      when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");

      final PlayerId playerKeoland = new PlayerId("Keoland", gameData);
      final PlayerId playerFuryondy = new PlayerId("Furyondy", gameData);
      gameData.getPlayerList().addPlayerId(playerKeoland);
      gameData.getPlayerList().addPlayerId(playerFuryondy);

      final RelationshipType existingRelationshipType =
          new RelationshipType(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL, gameData);
      final RelationshipType newRelationshipType =
          new RelationshipType(Constants.RELATIONSHIP_ARCHETYPE_ALLIED, gameData);
      gameData.getRelationshipTypeList().addRelationshipType(existingRelationshipType);
      gameData.getRelationshipTypeList().addRelationshipType(newRelationshipType);

      final RelationshipTracker relationshipTracker = gameData.getRelationshipTracker();
      relationshipTracker.setRelationship(playerKeoland, playerFuryondy, existingRelationshipType);

      final BattleDelegate battleDelegate = mock(BattleDelegate.class);
      when(battleDelegate.getName()).thenReturn("battle");
      gameData.addDelegate(battleDelegate);
      final BattleTracker battleTracker = mock(BattleTracker.class);
      when(battleDelegate.getBattleTracker()).thenReturn(battleTracker);

      TriggerAttachment.triggerRelationshipChange(
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
    void testTriggerAvailableTechChange() throws Exception {
      final GameData gameData = bridge.getData();

      final PlayerId playerId = new PlayerId("somePlayer", gameData);
      playerId.getTechnologyFrontierList().addTechnologyFrontier(new TechnologyFrontier("airCategory", gameData));

      final TriggerAttachment triggerAttachment = new TriggerAttachment("triggerAttachment", playerId, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      final TechnologyFrontier gameTechnologyFrontier = gameData.getTechnologyFrontier();
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("longRangeAir", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("jetPower", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("heavyBomber", gameData));

      triggerAttachment.getPropertyMap().get("availableTech")
          .setValue("airCategory:longRangeAir:jetPower:heavyBomber");

      TriggerAttachment.triggerAvailableTechChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge, times(3)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTechChange() throws Exception {
      final GameData gameData = bridge.getData();

      final PlayerId playerId = new PlayerId("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", playerId, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      final TechnologyFrontier gameTechnologyFrontier = gameData.getTechnologyFrontier();
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("longRangeAir", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("jetPower", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("heavyBomber", gameData));

      triggerAttachment.getPropertyMap().get("tech").setValue("longRangeAir:heavyBomber");

      TriggerAttachment.triggerTechChange(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge, times(2)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerProductionChange() throws Exception {
      final GameData gameData = bridge.getData();

      final ProductionFrontier startingFrontier = new ProductionFrontier("production", gameData);
      final ProductionFrontier newFrontier =
          new ProductionFrontier("Americans_Super_Carrier_production", gameData);

      gameData.getProductionFrontierList().addProductionFrontier(startingFrontier);
      gameData.getProductionFrontierList().addProductionFrontier(newFrontier);

      final PlayerId playerId = new PlayerId("somePlayer", gameData);
      playerId.setProductionFrontier(startingFrontier);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", playerId, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      triggerAttachment.getPropertyMap().get("frontier").setValue("Americans_Super_Carrier_production");

      TriggerAttachment.triggerProductionChange(
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
    void testTriggerSupportChange() throws Exception {
      final GameData gameData = bridge.getData();

      final UnitType battleshipUnitType = new UnitType("battleship", gameData);
      battleshipUnitType.addAttachment(
          "something",
          new UnitSupportAttachment("supportAttachmentBattlefleet_Support", null, gameData));
      gameData.getUnitTypeList().addUnitType(battleshipUnitType);

      final PlayerId playerId = new PlayerId("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", playerId, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      triggerAttachment.getPropertyMap().get("support")
          .setValue("supportAttachmentBattlefleet_Support");

      TriggerAttachment.triggerSupportChange(
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

    // TODO: Too little/much usage of mocking in the various added unit tests? Poor usage of mocking?

    @Test
    void testTriggerChangeOwnership() throws Exception {
      final GameData gameData = bridge.getData();
      final BattleDelegate battleDelegate = mock(BattleDelegate.class);
      when(battleDelegate.getName()).thenReturn("battle");
      gameData.addDelegate(battleDelegate);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      final PlayerId playerChina = new PlayerId("China", gameData);
      final PlayerId playerRussia = new PlayerId("Russia", gameData);
      final PlayerId playerBritain = new PlayerId("Britain", gameData);
      gameData.getPlayerList().addPlayerId(playerChina);
      gameData.getPlayerList().addPlayerId(playerRussia);
      gameData.getPlayerList().addPlayerId(playerBritain);

      final GameMap gameMap = gameData.getMap();
      final BiConsumer<PlayerId, String> addTerritory = (player, territoryName) -> {
        final Territory territory = new Territory(territoryName, gameData);
        territory.addAttachment(
            Constants.TERRITORY_ATTACHMENT_NAME,
            new TerritoryAttachment(null, null, null));
        territory.setOwner(player);
        gameMap.addTerritory(territory);
      };
      addTerritory.accept(playerChina, "Altay");
      addTerritory.accept(playerChina, "Archangel");
      addTerritory.accept(playerRussia, "Eastern Szechwan");

      // NOTE: Currently not testing "booleanCaptured?" option nor the BattleDelegate part reg. capturing
      // (ie. the boolean part last in "Altay:China:Russia:false" is always set to false in this test).
      final MutableProperty<?> changeOwnership = triggerAttachment.getPropertyMap().get("changeOwnership");
      changeOwnership.setValue("Altay:China:Russia:false");
      changeOwnership.setValue("Archangel:Russia:Britain:false"); // Belonging to non-Russia, so should not match.
      changeOwnership.setValue("Eastern Szechwan:any:China:false");

      TriggerAttachment.triggerChangeOwnership(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge, times(2)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerPurchase() throws Exception {
      final GameData gameData = bridge.getData();

      final PlayerId playerId = new PlayerId("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", playerId, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      gameData.getUnitTypeList().addUnitType(new UnitType("brigantine", gameData));
      gameData.getUnitTypeList().addUnitType(new UnitType("sellsword", gameData));
      gameData.getUnitTypeList().addUnitType(new UnitType("skirmisher", gameData));

      final MutableProperty<?> purchase = triggerAttachment.getPropertyMap().get("purchase");
      // NOTE: The 'count' part is prepended in the game parser.
      purchase.setValue("1:brigantine");
      purchase.setValue("2:sellsword:skirmisher");

      TriggerAttachment.triggerPurchase(
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
    void testTriggerUnitRemoval() throws Exception {
      final GameData gameData = bridge.getData();

      final PlayerId player = new PlayerId("somePlayer", gameData);
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", player, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Collections.singleton(triggerAttachment);

      final GameMap gameMap = gameData.getMap();
      final Function<String, Territory> addTerritory = (territoryName) -> {
        final Territory territory = new Territory(territoryName, gameData);
        territory.addAttachment(
            Constants.TERRITORY_ATTACHMENT_NAME,
            new TerritoryAttachment(null, null, null));
        gameMap.addTerritory(territory);
        return territory;
      };
      final Territory territoryCorusk = addTerritory.apply("Corusk Pass");
      final Territory territoryHraak = addTerritory.apply("Hraak Pass");
      final Territory territorySoull = addTerritory.apply("Soull Pass");

      final UnitType unitTypeConscript = new UnitType("conscript", gameData);
      gameData.getUnitTypeList().addUnitType(unitTypeConscript);
      final UnitType unitTypeSellsword = new UnitType("sellsword", gameData);
      gameData.getUnitTypeList().addUnitType(unitTypeSellsword);

      final BiConsumer<Territory, UnitType> addUnit =
          (territory, unitType) -> territory.getUnitCollection().add(new Unit(unitType, player, gameData));

      addUnit.accept(territoryCorusk, unitTypeConscript);
      addUnit.accept(territoryCorusk, unitTypeConscript);

      addUnit.accept(territoryHraak, unitTypeConscript);

      addUnit.accept(territorySoull, unitTypeSellsword);
      addUnit.accept(territorySoull, unitTypeSellsword);
      addUnit.accept(territorySoull, unitTypeConscript);

      final MutableProperty<?> removeUnits = triggerAttachment.getPropertyMap().get("removeUnits");
      // NOTE: The 'count' part is prepended in the game parser.
      removeUnits.setValue("5:Corusk Pass:all");
      removeUnits.setValue("5:Hraak Pass:conscript");
      removeUnits.setValue("5:all:sellsword");

      TriggerAttachment.triggerUnitRemoval(
          satisfiedTriggers,
          bridge,
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen
      verify(bridge, times(3)).addChange(not(argThat(Change::isEmpty)));
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
