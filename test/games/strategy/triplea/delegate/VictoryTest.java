package games.strategy.triplea.delegate;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;
import junit.framework.TestCase;

/**
 * "Victory" map is just a branch/mod of Pact of Steel 2.
 * POS2 is an actual game with good gameplay that we don't want to mess with, so
 * "Victory" is more of an xml purely for testing purposes, and probably should never be played.
 * 
 */
public class VictoryTest extends TestCase
{
	
	private GameData m_data;
	private PlayerID m_italians;
	private ITestDelegateBridge m_bridge;
	
	public VictoryTest(final String name)
	{
		super(name);
	}
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		m_data = LoadGameUtil.loadGame("victory_test", "victory_test.xml");
		
		m_italians = m_data.getPlayerList().getPlayerID("Italians");
		m_bridge = GameDataTestUtil.getDelegateBridge(m_italians, m_data);
		// we need to initialize the original owner
		final InitializationDelegate initDel = (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
		initDel.setDelegateBridgeAndPlayer(m_bridge);
		initDel.start();
		initDel.end();
	}
	
	@Override
	public void tearDown() throws Exception
	{
		m_data = null;
		super.tearDown();
	}
	
	public void testNoBlitzThroughMountain()
	{
		final Territory libya = m_data.getMap().getTerritory("Libya");
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(libya, armour.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		final String error = moveDelegate.move(libya.getUnits().getUnits(), m_data.getMap().getRoute(libya, b_congo));
		moveDelegate.end();
		assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
	}
	
	public void testBlitzNormal()
	{
		final Territory fw_africa = m_data.getMap().getTerritory("French West Africa");
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(fw_africa, armour.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		final String error = moveDelegate.move(fw_africa.getUnits().getUnits(), m_data.getMap().getRoute(fw_africa, b_congo));
		moveDelegate.end();
		assertEquals(error, null);
	}
	
	public void testNoBlitzWithStopThroughMountain()
	{
		final Territory libya = m_data.getMap().getTerritory("Libya");
		final Territory a_egypt = m_data.getMap().getTerritory("Anglo Egypt");
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(libya, armour.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		String error = moveDelegate.move(libya.getUnits().getUnits(), m_data.getMap().getRoute(libya, a_egypt));
		assertEquals(error, null); // first step is legal
		
		// second step isn't legal because we lost blitz even though we took the mountain
		error = moveDelegate.move(a_egypt.getUnits().getUnits(), m_data.getMap().getRoute(a_egypt, b_congo));
		moveDelegate.end();
		assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
	}
	
	public void testBlitzWithStop()
	{
		final Territory fw_africa = m_data.getMap().getTerritory("French West Africa");
		final Territory fe_africa = m_data.getMap().getTerritory("French Equatorial Africa");
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(fw_africa, armour.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		String error = moveDelegate.move(fw_africa.getUnits().getUnits(), m_data.getMap().getRoute(fw_africa, fe_africa));
		assertEquals(error, null);
		error = moveDelegate.move(fe_africa.getUnits().getUnits(), m_data.getMap().getRoute(fe_africa, b_congo));
		moveDelegate.end();
		assertEquals(error, null);
	}
	
	// test if it gives the unit can't blitz error instead of the unit lost blitz error if a non-blitzing unit tries to blitz!
	public void testMotorizedThroughMountain()
	{
		final Territory libya = m_data.getMap().getTerritory("Libya");
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final UnitType motorized = m_data.getUnitTypeList().getUnitType("motorized");
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(libya, motorized.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		final String error = moveDelegate.move(libya.getUnits().getUnits(), m_data.getMap().getRoute(libya, b_congo));
		moveDelegate.end();
		assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
	}
	
	public void testMotorizedNoBlitzBlitzedTerritory()
	{
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final Territory kenya = m_data.getMap().getTerritory("Kenya");
		final Territory fe_africa = m_data.getMap().getTerritory("French Equatorial Africa");
		
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		final UnitType motorized = m_data.getUnitTypeList().getUnitType("motorized");
		
		new ChangePerformer(m_data).perform(ChangeFactory.changeOwner(fe_africa, m_italians));
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(fe_africa, armour.create(1, m_italians)));
		new ChangePerformer(m_data).perform(ChangeFactory.changeOwner(kenya, m_italians));
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(kenya, motorized.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		String error = moveDelegate.move(fe_africa.getUnits().getUnits(), m_data.getMap().getRoute(fe_africa, b_congo));
		assertEquals(null, error);
		
		error = moveDelegate.move(kenya.getUnits().getUnits(), m_data.getMap().getRoute(kenya, b_congo));
		assertEquals(null, error);
		
		error = moveDelegate.move(b_congo.getUnits().getUnits(), m_data.getMap().getRoute(b_congo, fe_africa));
		assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
		moveDelegate.end();
	}
	
	public void testFuelUseMotorized()
	{
		final Territory b_congo = m_data.getMap().getTerritory("Belgian Congo");
		final Territory kenya = m_data.getMap().getTerritory("Kenya");
		final UnitType motorized = m_data.getUnitTypeList().getUnitType("motorized");
		final UnitType armour = m_data.getUnitTypeList().getUnitType("armour");
		
		new ChangePerformer(m_data).perform(ChangeFactory.changeOwner(kenya, m_italians));
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(kenya, motorized.create(1, m_italians)));
		
		final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
		m_bridge.setStepName("CombatMove");
		moveDelegate.setDelegateBridgeAndPlayer(m_bridge);
		moveDelegate.start();
		
		final int fuelAmount = m_italians.getResources().getQuantity("Fuel");
		final int puAmount = m_italians.getResources().getQuantity("PUs");
		
		moveDelegate.move(kenya.getUnits().getUnits(), m_data.getMap().getRoute(kenya, b_congo));
		assertEquals(fuelAmount - 1, m_italians.getResources().getQuantity("Fuel"));
		assertEquals(puAmount - 1, m_italians.getResources().getQuantity("PUs"));
		
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(kenya, armour.create(1, m_italians)));
		moveDelegate.move(kenya.getUnits().getUnits(), m_data.getMap().getRoute(kenya, b_congo));
		assertEquals(fuelAmount - 1, m_italians.getResources().getQuantity("Fuel"));
		assertEquals(puAmount - 1, m_italians.getResources().getQuantity("PUs"));
		
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(kenya, motorized.create(5, m_italians)));
		moveDelegate.move(kenya.getUnits().getUnits(), m_data.getMap().getRoute(kenya, b_congo));
		assertEquals(fuelAmount - 6, m_italians.getResources().getQuantity("Fuel"));
		assertEquals(puAmount - 6, m_italians.getResources().getQuantity("PUs"));
		
		new ChangePerformer(m_data).perform(ChangeFactory.addUnits(kenya, motorized.create(50, m_italians)));
		final String error = moveDelegate.move(kenya.getUnits().getUnits(), m_data.getMap().getRoute(kenya, b_congo));
		assertTrue(error.startsWith("Not enough resources to perform this move"));
		
		moveDelegate.end();
	}
	
	public void testUseNoFuelWhileTransported()
	{
		// FIXME this is a known bug.. when you fix this first update
	}
	
	public void testMultipleResourcesToPurchase()
	{
		
		final IntegerMap<Resource> italianResources = m_italians.getResources().getResourcesCopy();
		final PurchaseDelegate purchaseDelegate = (PurchaseDelegate) m_data.getDelegateList().getDelegate("purchase");
		m_bridge.setStepName("italianPurchase");
		purchaseDelegate.setDelegateBridgeAndPlayer(m_bridge);
		purchaseDelegate.start();
		final IntegerMap<ProductionRule> purchaseList = new IntegerMap<ProductionRule>();
		final ProductionRule armourtest = m_data.getProductionRuleList().getProductionRule("buyArmourtest");
		assertNotNull(armourtest);
		italianResources.subtract(armourtest.getCosts());
		purchaseList.add(armourtest, 1);
		final String error = purchaseDelegate.purchase(purchaseList);
		assertEquals(null, error);
		assertEquals(italianResources, m_italians.getResources().getResourcesCopy());
		
	}
	
	public void testNotEnoughMultipleResourcesToPurchase()
	{
		
		final IntegerMap<Resource> italianResources = m_italians.getResources().getResourcesCopy();
		final PurchaseDelegate purchaseDelegate = (PurchaseDelegate) m_data.getDelegateList().getDelegate("purchase");
		m_bridge.setStepName("italianPurchase");
		purchaseDelegate.setDelegateBridgeAndPlayer(m_bridge);
		purchaseDelegate.start();
		final IntegerMap<ProductionRule> purchaseList = new IntegerMap<ProductionRule>();
		final ProductionRule armourtest = m_data.getProductionRuleList().getProductionRule("buyArmourtest2");
		assertNotNull(armourtest);
		italianResources.subtract(armourtest.getCosts());
		purchaseList.add(armourtest, 1);
		final String error = purchaseDelegate.purchase(purchaseList);
		assertEquals(PurchaseDelegate.NOT_ENOUGH_RESOURCES, error);
		
	}
	
	public void testPUOnlyResourcesToPurchase()
	{
		
		final IntegerMap<Resource> italianResources = m_italians.getResources().getResourcesCopy();
		final PurchaseDelegate purchaseDelegate = (PurchaseDelegate) m_data.getDelegateList().getDelegate("purchase");
		m_bridge.setStepName("italianPurchase");
		purchaseDelegate.setDelegateBridgeAndPlayer(m_bridge);
		purchaseDelegate.start();
		final IntegerMap<ProductionRule> purchaseList = new IntegerMap<ProductionRule>();
		final ProductionRule buyArmour = m_data.getProductionRuleList().getProductionRule("buyArmour");
		assertNotNull(buyArmour);
		italianResources.subtract(buyArmour.getCosts());
		purchaseList.add(buyArmour, 1);
		final String error = purchaseDelegate.purchase(purchaseList);
		assertEquals(null, error);
		assertEquals(italianResources, m_italians.getResources().getResourcesCopy());
	}
	
	public void testNoPUResourcesToPurchase()
	{
		
		final IntegerMap<Resource> italianResources = m_italians.getResources().getResourcesCopy();
		final PurchaseDelegate purchaseDelegate = (PurchaseDelegate) m_data.getDelegateList().getDelegate("purchase");
		m_bridge.setStepName("italianPurchase");
		purchaseDelegate.setDelegateBridgeAndPlayer(m_bridge);
		purchaseDelegate.start();
		final IntegerMap<ProductionRule> purchaseList = new IntegerMap<ProductionRule>();
		final ProductionRule buyArmour = m_data.getProductionRuleList().getProductionRule("buyArmourtest3");
		assertNotNull(buyArmour);
		italianResources.subtract(buyArmour.getCosts());
		purchaseList.add(buyArmour, 1);
		final String error = purchaseDelegate.purchase(purchaseList);
		assertEquals(null, error);
		assertEquals(italianResources, m_italians.getResources().getResourcesCopy());
		
	}
	
	public void testTerritoryEffectsOnCombat()
	{
		// TODO implement test
	}
	
	public void testCanOnlyInvadeFrom()
	{
		// TODO implement test
	}
}
