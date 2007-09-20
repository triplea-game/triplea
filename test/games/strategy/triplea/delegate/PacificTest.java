/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.ui.display.DummyDisplay;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class PacificTest extends TestCase
{

    GameData m_data;

    UnitType infantry;

    UnitType armor;

    UnitType artillery;

    UnitType marine;

    UnitType fighter;

    UnitType bomber;

    UnitType sub;

    UnitType destroyer;

    UnitType carrier;

    UnitType battleship;

    UnitType transport;

    // Define defending players
    PlayerID americans;

    PlayerID chinese;

    PlayerID british;

    Territory queensland;

    ITestDelegateBridge bridge;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File pacific = new File(gamesFolder, "pacific_incomplete.xml");

        if (!pacific.exists())
            throw new IllegalStateException("pacific game does not exist");

        InputStream input = new BufferedInputStream(new FileInputStream(pacific));

        try
        {
            m_data = (new GameParser()).parse(input);
        } finally
        {
            input.close();
        }

        infantry = m_data.getUnitTypeList().getUnitType("infantry");
        armor = m_data.getUnitTypeList().getUnitType("armour");
        artillery = m_data.getUnitTypeList().getUnitType("artillery");
        marine = m_data.getUnitTypeList().getUnitType("marine");
        fighter = m_data.getUnitTypeList().getUnitType("fighter");
        bomber = m_data.getUnitTypeList().getUnitType("bomber");
        sub = m_data.getUnitTypeList().getUnitType("submarine");
        destroyer = m_data.getUnitTypeList().getUnitType("destroyer");
        carrier = m_data.getUnitTypeList().getUnitType("carrier");
        battleship = m_data.getUnitTypeList().getUnitType("battleship");
        transport = m_data.getUnitTypeList().getUnitType("transport");

        // Define defending players
        americans = m_data.getPlayerList().getPlayerID("Americans");
        chinese = m_data.getPlayerList().getPlayerID("Chinese");
        british = m_data.getPlayerList().getPlayerID("British");

        queensland = m_data.getMap().getTerritory("Queensland");

        bridge = getDelegateBridge(americans);

    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }

    protected ITestDelegateBridge getDelegateBridge(PlayerID player)
    {
        ITestDelegateBridge bridge1 = new TestDelegateBridge(m_data, player, (IDisplay) new DummyDisplay());
        TestTripleADelegateBridge bridge2 = new TestTripleADelegateBridge(bridge1, m_data);
        return bridge2;
    }

    public void testNonJapanAttack()
    {
        bridge.setStepName("NotJapanAttack");

        // Defending US infantry hit on a 2 (0 base)
        List<Unit> infantryUS = infantry.create(1, americans);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        DiceRoll roll = DiceRoll.rollDice(infantryUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

        // Defending US marines hit on a 2 (0 base)
        List<Unit> marineUS = marine.create(1, americans);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        roll = DiceRoll.rollDice(marineUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

        // Chinese units
        // Defending Chinese infantry hit on a 2 (0 base)
        List<Unit> infantryChina = infantry.create(1, chinese);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

    }

    public void testJapanAttackFirstRound()
    {

        bridge.setStepName("japaneseBattle");

        // >>> After patch normal to-hits will miss <<<

        // Defending US infantry miss on a 2 (0 base)
        List<Unit> infantryUS = infantry.create(1, americans);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        DiceRoll roll = DiceRoll.rollDice(infantryUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(0, roll.getHits());

        // Defending US marines miss on a 2 (0 base)
        List<Unit> marineUS = marine.create(1, americans);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        roll = DiceRoll.rollDice(marineUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(0, roll.getHits());

        //      

        // Chinese units
        // Defending Chinese infantry still hit on a 2 (0 base)
        List<Unit> infantryChina = infantry.create(1, chinese);
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

        // Defending US infantry hit on a 1 (0 base)
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 0 }));
        roll = DiceRoll.rollDice(infantryUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

        // Defending US marines hit on a 1 (0 base)
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 0 }));
        roll = DiceRoll.rollDice(marineUS, true, americans, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

        // Chinese units
        // Defending Chinese infantry still hit on a 2 (0 base)
        bridge.setRandomSource(new ScriptedRandomSource(new int[]
        { 1 }));
        roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, m_data, new MockBattle(queensland), "");
        assertEquals(1, roll.getHits());

    }

}
