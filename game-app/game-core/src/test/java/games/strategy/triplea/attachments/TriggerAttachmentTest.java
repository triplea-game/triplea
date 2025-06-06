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

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Named;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ProductionRuleList;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.ui.NotificationMessages;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Tuple;

@ExtendWith(MockitoExtension.class)
class TriggerAttachmentTest {

  private static final FireTriggerParams defaultFireTriggerParams =
      new FireTriggerParams(
          "beforeOrAfter",
          "stepName",
          false, // useUses
          false, // testUses
          false, // testChance
          false); // testWhen

  private static BattleDelegate mockAddBattleDelegate(final GameData gameData) {
    final BattleDelegate battleDelegate = mock(BattleDelegate.class);
    when(battleDelegate.getName()).thenReturn("battle");
    gameData.addDelegate(battleDelegate);
    return battleDelegate;
  }

  @Nested
  class TriggerFireTest {

    @Mock private IDelegateBridge bridge;
    @Mock private IDelegateHistoryWriter historyWriter;

    @BeforeEach
    void setUp() {
      final GameData gameData = new GameData();

      when(bridge.getData()).thenReturn(gameData);
      when(bridge.getHistoryWriter()).thenReturn(historyWriter);
    }

    // Assumes 'gameData.getMap()' has the relevant parts set up.
    private Territory addTerritoryPlayer(final String territoryName, final GamePlayer player) {

      final GameData gameData = bridge.getData();
      final GameMap gameMap = gameData.getMap();

      final Territory territory = new Territory(territoryName, gameData);
      territory.addAttachment(
          Constants.TERRITORY_ATTACHMENT_NAME, new TerritoryAttachment(null, null, null));
      if (player != null) {
        territory.setOwner(player);
      }
      gameMap.addTerritory(territory);
      return territory;
    }

    // Assumes 'gameData.getMap()' has the relevant parts set up.
    private Territory addTerritory(final String territoryName) {
      return addTerritoryPlayer(territoryName, null);
    }

    @Test
    void testTriggerNotifications() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment(
              "triggerAttachment", new GamePlayer("somePlayerName", gameData), gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final String notificationMessageKey = "BlackIce";
      final String notificationMessage =
          "<body><h2>The Land of Black Ice</h2>Whether out of duty, ..."
              + "<br>never heard from again.</body>";

      final IDisplay display = mock(IDisplay.class);
      when(bridge.getDisplayChannelBroadcaster()).thenReturn(display);

      final NotificationMessages notificationMessages = mock(NotificationMessages.class);
      when(notificationMessages.getMessage(notificationMessageKey)).thenReturn(notificationMessage);

      setPropertyOrThrow(triggerAttachment, "notification", notificationMessageKey);

      TriggerAttachment.triggerNotifications(
          satisfiedTriggers, bridge, defaultFireTriggerParams, notificationMessages);
      verify(display)
          .reportMessageToPlayers(
              not(argThat(Collection::isEmpty)), // Players.
              any(),
              argThat(htmlMessage -> htmlMessage.contains(notificationMessage)),
              any());
    }

    @Test
    void testTriggerProductionFrontierEditChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final ProductionRuleList productionRuleList = gameData.getProductionRuleList();
      productionRuleList.addProductionRule(new ProductionRule("rule1", gameData));
      final ProductionRule productionRule2 = new ProductionRule("rule2", gameData);
      productionRuleList.addProductionRule(productionRule2);
      productionRuleList.addProductionRule(new ProductionRule("rule3", gameData));

      final ProductionFrontierList productionFrontierList = gameData.getProductionFrontierList();
      productionFrontierList.addProductionFrontier(
          new ProductionFrontier("frontier", gameData, List.of(productionRule2)));

      final MutableProperty<?> productionRuleProperty =
          triggerAttachment.getPropertyOrThrow("productionRule");
      productionRuleProperty.setValue("frontier:rule1");
      productionRuleProperty.setValue("frontier:-rule2");
      productionRuleProperty.setValue("frontier:rule3");

      TriggerAttachment.triggerProductionFrontierEditChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
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
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);
      gamePlayer.addAttachment("rulesAttachment", new RulesAttachment(null, null, gameData));
      gameData.getPlayerList().addPlayerId(gamePlayer);

      setPropertyOrThrow(
          triggerAttachment, "playerAttachmentName", "rulesAttachment:RulesAttachment");
      // NOTE: The 'count' part is prepended in the game parser.
      setPropertyOrThrow(
          triggerAttachment, "playerProperty", "someNewValue:productionPerXTerritories");
      setPropertyOrThrow(triggerAttachment, "players", "somePlayer");

      TriggerAttachment.triggerPlayerPropertyChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerRelationshipTypePropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final RelationshipType relationshipType =
          new RelationshipType("someRelationshipType", gameData);
      relationshipType.addAttachment(
          "relationshipTypeAttachment", new RelationshipTypeAttachment(null, null, gameData));
      gameData.getRelationshipTypeList().addRelationshipType(relationshipType);

      setPropertyOrThrow(
          triggerAttachment,
          "relationshipTypeAttachmentName",
          "relationshipTypeAttachment:RelationshipTypeAttachment");
      // NOTE: The 'count' part is prepended in the game parser.
      setPropertyOrThrow(
          triggerAttachment, "relationshipTypeProperty", "true:canMoveLandUnitsOverOwnedLand");
      setPropertyOrThrow(triggerAttachment, "relationshipTypes", "someRelationshipType");

      TriggerAttachment.triggerRelationshipTypePropertyChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTerritoryPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final String territoryName = "Sea Zone 9";
      final Territory territory = new Territory(territoryName, gameData);
      territory.addAttachment("territoryAttachment", new TerritoryAttachment(null, null, gameData));
      gameData.getMap().addTerritory(territory);

      setPropertyOrThrow(
          triggerAttachment, "territoryAttachmentName", "territoryAttachment:TerritoryAttachment");
      // NOTE: The 'count' part is prepended in the game parser.
      setPropertyOrThrow(triggerAttachment, "territoryProperty", "true:kamikazeZone");
      setPropertyOrThrow(triggerAttachment, "territories", territoryName);

      TriggerAttachment.triggerTerritoryPropertyChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTerritoryEffectPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final String territoryEffectName = "someTerritoryEffect";
      final TerritoryEffect territoryEffect = new TerritoryEffect(territoryEffectName, gameData);
      territoryEffect.addAttachment(
          "territoryEffectAttachment", new TerritoryEffectAttachment(null, null, gameData));
      gameData.getTerritoryEffectList().put(territoryEffectName, territoryEffect);

      setPropertyOrThrow(
          triggerAttachment,
          "territoryEffectAttachmentName",
          "territoryEffectAttachment:TerritoryEffectAttachment");
      // NOTE: The 'count' part is prepended in the game parser.
      setPropertyOrThrow(
          triggerAttachment,
          "territoryEffectProperty",
          "conscript:veteran:champion:unitsNotAllowed");
      setPropertyOrThrow(triggerAttachment, "territoryEffects", "someTerritoryEffect");

      TriggerAttachment.triggerTerritoryEffectPropertyChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerUnitPropertyChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final UnitType unitType = new UnitType("someUnit", gameData);
      gameData.getUnitTypeList().addUnitType(unitType);
      unitType.addAttachment("unitAttachment", new UnitAttachment(null, null, gameData));

      setPropertyOrThrow(triggerAttachment, "unitAttachmentName", "unitAttachment:UnitAttachment");
      // NOTE: The 'count' part is prepended in the game parser.
      setPropertyOrThrow(triggerAttachment, "unitProperty", "4:movement");
      setPropertyOrThrow(triggerAttachment, "unitType", "someUnit");

      TriggerAttachment.triggerUnitPropertyChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerRelationshipChange() throws Exception {
      final GameData gameData = bridge.getData();
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final GamePlayer playerKeoland = new GamePlayer("Keoland", gameData);
      final GamePlayer playerFuryondy = new GamePlayer("Furyondy", gameData);
      gameData.getPlayerList().addPlayerId(playerKeoland);
      gameData.getPlayerList().addPlayerId(playerFuryondy);

      final RelationshipType existingRelationshipType =
          new RelationshipType(Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL, gameData);
      final RelationshipType newRelationshipType =
          new RelationshipType(Constants.RELATIONSHIP_ARCHETYPE_ALLIED, gameData);
      final RelationshipTypeList relationshipTypeList = gameData.getRelationshipTypeList();
      relationshipTypeList.addRelationshipType(existingRelationshipType);
      relationshipTypeList.addRelationshipType(newRelationshipType);

      final RelationshipTracker relationshipTracker = gameData.getRelationshipTracker();
      relationshipTracker.setRelationship(playerKeoland, playerFuryondy, existingRelationshipType);

      final BattleDelegate battleDelegate = mockAddBattleDelegate(gameData);
      final BattleTracker battleTracker = mock(BattleTracker.class);
      when(battleDelegate.getBattleTracker()).thenReturn(battleTracker);

      setPropertyOrThrow(triggerAttachment, "relationshipChange", "Keoland:Furyondy:any:allied");

      TriggerAttachment.triggerRelationshipChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerAvailableTechChange() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);
      gamePlayer
          .getTechnologyFrontierList()
          .addTechnologyFrontier(new TechnologyFrontier("airCategory", gameData));

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", gamePlayer, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final TechnologyFrontier gameTechnologyFrontier = gameData.getTechnologyFrontier();
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("longRangeAir", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("jetPower", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("heavyBomber", gameData));

      setPropertyOrThrow(
          triggerAttachment, "availableTech", "airCategory:longRangeAir:jetPower:heavyBomber");

      TriggerAttachment.triggerAvailableTechChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(3)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerTechChange() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", gamePlayer, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final TechnologyFrontier gameTechnologyFrontier = gameData.getTechnologyFrontier();
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("longRangeAir", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("jetPower", gameData));
      gameTechnologyFrontier.addAdvance(
          TechAdvance.findDefinedAdvanceAndCreateAdvance("heavyBomber", gameData));

      setPropertyOrThrow(triggerAttachment, "tech", "longRangeAir:heavyBomber");

      TriggerAttachment.triggerTechChange(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(2)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerProductionChange() throws Exception {
      final GameData gameData = bridge.getData();

      final ProductionFrontier startingFrontier =
          new ProductionFrontier(ProductionFrontier.PRODUCTION, gameData);
      final ProductionFrontier newFrontier =
          new ProductionFrontier("Americans_Super_Carrier_production", gameData);

      gameData.getProductionFrontierList().addProductionFrontier(startingFrontier);
      gameData.getProductionFrontierList().addProductionFrontier(newFrontier);

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);
      gamePlayer.setProductionFrontier(startingFrontier);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", gamePlayer, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      setPropertyOrThrow(triggerAttachment, "frontier", "Americans_Super_Carrier_production");

      TriggerAttachment.triggerProductionChange(
          satisfiedTriggers, bridge, defaultFireTriggerParams);
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

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", gamePlayer, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      setPropertyOrThrow(triggerAttachment, "support", "supportAttachmentBattlefleet_Support");

      TriggerAttachment.triggerSupportChange(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerChangeOwnership() throws Exception {
      final GameData gameData = bridge.getData();
      mockAddBattleDelegate(gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final GamePlayer playerChina = new GamePlayer("China", gameData);
      final GamePlayer playerRussia = new GamePlayer("Russia", gameData);
      final GamePlayer playerBritain = new GamePlayer("Britain", gameData);
      gameData.getPlayerList().addPlayerId(playerChina);
      gameData.getPlayerList().addPlayerId(playerRussia);
      gameData.getPlayerList().addPlayerId(playerBritain);

      addTerritoryPlayer("Altay", playerChina);
      addTerritoryPlayer("Archangel", playerChina);
      addTerritoryPlayer("Eastern Szechwan", playerRussia);

      // NOTE: Currently not testing "booleanCaptured?" option nor the BattleDelegate part reg.
      // capturing
      // (ie. the boolean part last in "Altay:China:Russia:false" is always set to false in this
      // test).
      final MutableProperty<?> changeOwnership =
          triggerAttachment.getPropertyOrThrow("changeOwnership");
      changeOwnership.setValue("Altay:China:Russia:false");
      changeOwnership.setValue(
          "Archangel:Russia:Britain:false"); // Belonging to non-Russia, so should not match.
      changeOwnership.setValue("Eastern Szechwan:any:China:false");

      TriggerAttachment.triggerChangeOwnership(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(2)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerPurchase() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", gamePlayer, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      gameData.getUnitTypeList().addUnitType(new UnitType("brigantine", gameData));
      gameData.getUnitTypeList().addUnitType(new UnitType("sellsword", gameData));
      gameData.getUnitTypeList().addUnitType(new UnitType("skirmisher", gameData));

      final MutableProperty<?> purchase = triggerAttachment.getPropertyOrThrow("purchase");
      // NOTE: The 'count' part is prepended in the game parser.
      purchase.setValue("1:brigantine");
      purchase.setValue("2:sellsword:skirmisher");

      TriggerAttachment.triggerPurchase(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerUnitRemoval() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer player = new GamePlayer("somePlayer", gameData);
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", player, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final Territory territoryCorusk = addTerritory("Corusk Pass");
      final Territory territoryHraak = addTerritory("Hraak Pass");
      final Territory territorySoull = addTerritory("Soull Pass");

      final UnitType unitTypeConscript = new UnitType("conscript", gameData);
      gameData.getUnitTypeList().addUnitType(unitTypeConscript);
      final UnitType unitTypeSellsword = new UnitType("sellsword", gameData);
      gameData.getUnitTypeList().addUnitType(unitTypeSellsword);

      final BiConsumer<Territory, UnitType> addUnit =
          (territory, unitType) ->
              territory.getUnitCollection().add(new Unit(unitType, player, gameData));

      addUnit.accept(territoryCorusk, unitTypeConscript);
      addUnit.accept(territoryCorusk, unitTypeConscript);

      addUnit.accept(territoryHraak, unitTypeConscript);

      addUnit.accept(territorySoull, unitTypeSellsword);
      addUnit.accept(territorySoull, unitTypeSellsword);
      addUnit.accept(territorySoull, unitTypeConscript);

      final MutableProperty<?> removeUnits = triggerAttachment.getPropertyOrThrow("removeUnits");
      // NOTE: The 'count' part is prepended in the game parser.
      removeUnits.setValue("5:Corusk Pass:all");
      removeUnits.setValue("5:Hraak Pass:conscript");
      removeUnits.setValue("5:all:sellsword");

      TriggerAttachment.triggerUnitRemoval(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(3)).addChange(not(argThat(Change::isEmpty)));
    }

    private void addUnitType(final String unitTypeName) {
      final GameData gameData = bridge.getData();
      final UnitType unitTypeConscript = new UnitType(unitTypeName, gameData);
      gameData.getUnitTypeList().addUnitType(unitTypeConscript);
      unitTypeConscript.addAttachment(
          Constants.UNIT_ATTACHMENT_NAME, new UnitAttachment("somename", null, gameData));
    }

    @Test
    void testTriggerUnitPlacement() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer player = new GamePlayer("somePlayer", gameData);
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", player, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      addUnitType("conscript");
      addUnitType("sellsword");

      addTerritory("Corusk Pass");
      addTerritory("Hraak Pass");
      addTerritory("Soull Pass");

      final MutableProperty<?> placement = triggerAttachment.getPropertyOrThrow("placement");
      placement.setValue("3:Corusk Pass:conscript");
      placement.setValue("2:Hraak Pass:conscript:sellsword");
      placement.setValue("Soull Pass:sellsword");

      TriggerAttachment.triggerUnitPlacement(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(3)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerResourceChange() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer player = new GamePlayer("somePlayer", gameData);
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", player, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      gameData.getResourceList().addResource(new Resource(Constants.PUS, gameData));

      setPropertyOrThrow(triggerAttachment, "resource", Constants.PUS);
      setPropertyOrThrow(triggerAttachment, "resourceCount", "23");

      TriggerAttachment.triggerResourceChange(satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(1)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerActivateTriggerOther() throws Exception {
      final GameData gameData = bridge.getData();

      mockAddBattleDelegate(gameData);

      // The trigger to be fired is for tech: trigger option "tech", method "triggerTechChange()".
      final TriggerAttachment triggerToBeFiredTriggerAttachment;
      {
        final GamePlayer gamePlayer = new GamePlayer("somePlayer", gameData);
        gameData.getPlayerList().addPlayerId(gamePlayer);

        triggerToBeFiredTriggerAttachment =
            new TriggerAttachment("triggerToBeFired", gamePlayer, gameData);
        gamePlayer.addAttachment(
            triggerToBeFiredTriggerAttachment.getName(), triggerToBeFiredTriggerAttachment);

        final TechnologyFrontier gameTechnologyFrontier = gameData.getTechnologyFrontier();
        gameTechnologyFrontier.addAdvance(
            TechAdvance.findDefinedAdvanceAndCreateAdvance("longRangeAir", gameData));
        gameTechnologyFrontier.addAdvance(
            TechAdvance.findDefinedAdvanceAndCreateAdvance("jetPower", gameData));
        gameTechnologyFrontier.addAdvance(
            TechAdvance.findDefinedAdvanceAndCreateAdvance("heavyBomber", gameData));

        setPropertyOrThrow(triggerToBeFiredTriggerAttachment, "tech", "longRangeAir:heavyBomber");
      }

      final TriggerAttachment activateTriggerTriggerAttachment =
          new TriggerAttachment("activateTrigger", null, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(activateTriggerTriggerAttachment);

      setPropertyOrThrow(
          activateTriggerTriggerAttachment,
          "activateTrigger",
          String.format(
              "%s:1:false:false:false:false", triggerToBeFiredTriggerAttachment.getName()));

      TriggerAttachment.triggerActivateTriggerOther(
          Map.of(), satisfiedTriggers, bridge, defaultFireTriggerParams);
      verify(bridge, times(2)).addChange(not(argThat(Change::isEmpty)));
    }

    @Test
    void testTriggerVictory() throws Exception {
      final GameData gameData = bridge.getData();

      final GamePlayer player = new GamePlayer("somePlayer", gameData);
      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("triggerAttachment", player, gameData);
      final Set<TriggerAttachment> satisfiedTriggers = Set.of(triggerAttachment);

      final String notificationMessageKey = "IndomitableCenterVictory";
      final String notificationMessage =
          "<body><h2>Victory!<br>The Indomitable Center Has Conquered!</h2>...</body>";
      setPropertyOrThrow(triggerAttachment, "victory", notificationMessageKey);

      final EndRoundDelegate endRoundDelegate = mock(EndRoundDelegate.class);
      when(endRoundDelegate.getName()).thenReturn("endRound");
      gameData.addDelegate(endRoundDelegate);

      final NotificationMessages notificationMessages = mock(NotificationMessages.class);
      when(notificationMessages.getMessage(notificationMessageKey)).thenReturn(notificationMessage);

      TriggerAttachment.triggerVictory(
          satisfiedTriggers, bridge, defaultFireTriggerParams, notificationMessages);
      verify(endRoundDelegate).signalGameOver(any(), any(), any());
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
      final Tuple<Boolean, String> r =
          TriggerAttachment.getClearFirstNewValue("-clear-4:conscript");
      assertTrue(r.getFirst());
      assertEquals(r.getSecond(), "4:conscript");
    }

    @Test
    void testNoClear() {
      final Tuple<Boolean, String> r =
          TriggerAttachment.getClearFirstNewValue("clearValueWithoutDash");
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

      final TriggerAttachment triggerAttachment =
          new TriggerAttachment("aTriggerAName", null, null);
      final TestAttachment propertyAttachment = new TestAttachment("aTestAName", null, null);
      propertyAttachment.setValue(startValue);
      final Named attachedTo = mock(Named.class);

      return TriggerAttachment.getPropertyChangeHistoryStartEvent(
          triggerAttachment,
          propertyAttachment,
          "value", // Property name in 'TestAttachment'. Authentic name:
          // "productionPerXTerritories".
          Tuple.of(true, newValue),
          "rulesAttachment",
          attachedTo);
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

  void setPropertyOrThrow(TriggerAttachment attachment, String name, String value)
      throws MutableProperty.InvalidValueException {
    attachment.getPropertyOrThrow(name).setValue(value);
  }
}
