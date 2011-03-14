/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI;

import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.Dynamix_AI.Code.DoCombatMove;
import games.strategy.triplea.Dynamix_AI.Code.DoNonCombatMove;
import games.strategy.triplea.Dynamix_AI.Code.Place;
import games.strategy.triplea.Dynamix_AI.Code.Purchase;
import games.strategy.triplea.Dynamix_AI.Code.SelectCasualties;
import games.strategy.triplea.Dynamix_AI.Code.Tech;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.FactoryCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.KnowledgeCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ReconsiderSignalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.StatusCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.TacticalCenter;
import games.strategy.triplea.Dynamix_AI.CommandCenter.ThreatInvalidationCenter;
import games.strategy.triplea.Dynamix_AI.Others.Battle_RetreatTerCalculator;
import games.strategy.triplea.Dynamix_AI.Others.PhaseType;
import games.strategy.triplea.Dynamix_AI.UI.UI;
import games.strategy.triplea.baseAI.AbstractAI;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stephen (Wisconsin)
 *         2010-2011
 */
public class Dynamix_AI extends AbstractAI implements IGamePlayer, ITripleaPlayer
{
    private final static Logger s_logger = Logger.getLogger(Dynamix_AI.class.getName());
    /**
     * Some notes on using the Dynamix logger:
     *
     * First, to make the logs easily readable even when there are hundreds of lines, I want every considerable step down in the call stack to mean more log message indentation.
     * For example, these base logs have no indentation before them, but the base logs in the DoCombatMove class will have two spaces inserted at the start, and the level below that, four spaces.
     * In this way, when you're reading the log, you can skip over unimportant areas with speed because of the indentation.
     *
     * Just keep these things in mind while adding new logging code.
     * (P.S. For multiple reasons, it is strongly suggested that you use DUtils.Log instead of writing directly to the logger returned by this method.)
     */
    public static Logger GetStaticLogger()
    {
        return s_logger;
    }

    public Dynamix_AI(String name)
    {
        super(name);
    }

    //These static dynamix AI instances are going to be used by the settings window to let the player change AI goals, aggresiveness, etc.
    private static List<Dynamix_AI> s_dAIInstances = new ArrayList<Dynamix_AI>();
    public static void ClearAIInstancesMemory()
    {
        DUtils.Log(Level.FINE, "Clearing static Dynamix_AI instances.");
        s_dAIInstances.clear();
    }

    public static void AddDynamixAIIntoAIInstancesMemory(Dynamix_AI ai)
    {
        DUtils.Log(Level.FINE, "Adding Dynamix_AI named {0} to static instances.", ai.getName());
        s_dAIInstances.add(ai);
    }

    public static List<Dynamix_AI> GetDynamixAIInstancesMemory()
    {
        return s_dAIInstances;
    }

    public static void Initialize(TripleAFrame frame)
    {
        UI.Initialize(frame); //Must be done first
        DUtils.Log(Level.FINE, "Initializing Dynamix_AI...");
        GlobalCenter.Initialize(CachedInstanceCenter.CachedGameData);
        FactoryCenter.ClearStaticInstances();
        TacticalCenter.ClearStaticInstances();
        KnowledgeCenter.ClearStaticInstances();
        StatusCenter.ClearStaticInstances();
        ThreatInvalidationCenter.ClearStaticInstances();
        ReconsiderSignalCenter.ClearStaticInstances();
        CachedInstanceCenter.CachedBattleTracker = DelegateFinder.battleDelegate(CachedInstanceCenter.CachedGameData).getBattleTracker();
        UnitGroup.ClearBufferedMoves();
    }

    public static void ShowSettingsWindow()
    {
        DUtils.Log(Level.FINE, "Showing Dynamix_AI settings window.");
        UI.ShowSettingsWindow();
    }

    /**
     * Please call this right before an action is displayed to the user.
     */
    @Override
    public void pause()
    {
        Pause();
    }

    private static long s_lastActionDisplayTime = new Date().getTime();
    /**
     * Please call this right before an action is displayed to the user.
     */
    public static void Pause()
    {
        try
        {
            if (DSettings.LoadSettings().UseActionLengthGoals)
            {
                long pauseTime = GetTimeTillNextScheduledActionDisplay();
                if(pauseTime == -1)
                    return;
                Thread.sleep(pauseTime);
                s_lastActionDisplayTime = new Date().getTime();
            }
            else
            {
                switch (GlobalCenter.CurrentPhaseType)
                {
                    case Purchase:
                        Thread.sleep(DSettings.LoadSettings().PurchaseWait_AW);
                    case Combat_Move:
                        Thread.sleep(DSettings.LoadSettings().CombatMoveWait_AW);
                    case Non_Combat_Move:
                        Thread.sleep(DSettings.LoadSettings().NonCombatMoveWait_AW);
                    case Place:
                        Thread.sleep(DSettings.LoadSettings().PlacementWait_AW);
                }
            }
        }
        catch (InterruptedException ex)
        {
            DUtils.Log(Level.SEVERE, "InterruptedException occured while trying to perform AI pausing. Exception: {0}", ex);
        }
    }

    public static long GetTimeTillNextScheduledActionDisplay()
    {
        if(!DSettings.LoadSettings().UseActionLengthGoals)
            return -1;
        //If we're not in a phase that has pausing enabled
        if (!DUtils.ToList(DUtils.ToArray(PhaseType.Purchase, PhaseType.Combat_Move, PhaseType.Non_Combat_Move, PhaseType.Place)).contains(GlobalCenter.CurrentPhaseType))
            return -1;
        long timeSince = new Date().getTime() - s_lastActionDisplayTime;
        long wantedActionLength = 0;
        switch (GlobalCenter.CurrentPhaseType)
        {
            case Purchase:
                wantedActionLength = DSettings.LoadSettings().PurchaseWait_AL;
                break;
            case Combat_Move:
                wantedActionLength = DSettings.LoadSettings().CombatMoveWait_AL;
                break;
            case Non_Combat_Move:
                wantedActionLength = DSettings.LoadSettings().NonCombatMoveWait_AL;
                break;
            case Place:
                wantedActionLength = DSettings.LoadSettings().PlacementWait_AL;
                break;
        }
        int timeTill = (int) (wantedActionLength - timeSince);
        timeTill = Math.max(timeTill, 0);
        return timeTill;
    }

    private void NotifyGameRound(GameData data)
    {
        if (GlobalCenter.GameRound != data.getSequence().getRound())
        {
            GlobalCenter.GameRound = data.getSequence().getRound();

            UI.NotifyStartOfRound(GlobalCenter.GameRound);
            DUtils.Log(Level.FINE, "-----Start of game round notification sent out. Round {0}-----", GlobalCenter.GameRound);
            TacticalCenter.NotifyStartOfRound();
            FactoryCenter.NotifyStartOfRound();
            KnowledgeCenter.NotifyStartOfRound();
            StatusCenter.NotifyStartOfRound();
            ThreatInvalidationCenter.NotifyStartOfRound();
            ReconsiderSignalCenter.NotifyStartOfRound();

            GlobalCenter.CurrentPlayer = PlayerID.NULL_PLAYERID; //Reset current player to re-enable trigger in NotifyPlayer method
        }
    }
    private void NotifyPlayer(PlayerID player)
    {
        if(GlobalCenter.FirstDynamixPlayer == null)
            GlobalCenter.FirstDynamixPlayer = player;
        if (GlobalCenter.CurrentPlayer != player)
        {
            GlobalCenter.CurrentPlayer = player;

            DUtils.Log(Level.FINE, "-----Start of player's turn notification sent out. Player: {0}-----", player.getName());
            DOddsCalculator.SetGameData(getGameData()); //Refresh DOddsCalculator game data each time the player changes, to keep it up to date

            GlobalCenter.CurrentPhaseType = PhaseType.Unknown; //Reset current phase type to re-enable trigger in NotifyPhaseType method
        }
    }
    private void NotifyPhaseType(PhaseType phaseType)
    {
        if (GlobalCenter.FirstDynamixPhase == null)
            GlobalCenter.FirstDynamixPhase = phaseType;
        if (GlobalCenter.CurrentPhaseType != phaseType)
        {
            GlobalCenter.CurrentPhaseType = phaseType;

            DUtils.Log(Level.FINE, "-----Start of phase notification sent out. Phase: {0}-----", phaseType);
        }
    }

    protected void place(boolean bid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player)
    {
        NotifyGameRound(data);
        NotifyPlayer(player);
        NotifyPhaseType(PhaseType.Place);
        Place.place(this, bid, placeDelegate, data, player);

        if(bid) //We don't want info from the bid phase spilling over into first real round
            FactoryCenter.NotifyStartOfRound(); //So clear data
    }

    int m_moveLastType = -1;
    protected void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player)
    {
        UnitGroup.movesCount = 0; //Dynamix is able to undo it's moves, via UnitGroup, but we need to reset move count each phase for it to work

        if (!nonCombat)
        {
            NotifyGameRound(data);
            NotifyPlayer(player);
            NotifyPhaseType(PhaseType.Combat_Move);
            ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats();
            DoCombatMove.doCombatMove(this, data, moveDel, player);
            TacticalCenter.get(data, player).AllDelegateUnitGroups.clear();
            TacticalCenter.get(data, player).ClearFrozenUnits();
            pause();
        }
        else
        {
            NotifyGameRound(data);
            NotifyPlayer(player);
            NotifyPhaseType(PhaseType.Non_Combat_Move);
            ThreatInvalidationCenter.get(data, player).ClearInvalidatedThreats();
            DoNonCombatMove.doNonCombatMove(this, data, moveDel, player);
            TacticalCenter.get(data, player).AllDelegateUnitGroups.clear();
            TacticalCenter.get(data, player).ClearFrozenUnits();
            pause();
        }

        if(m_moveLastType == -1)
            m_moveLastType = 1;
        else if(m_moveLastType == 0)
            m_moveLastType = 1;
        else //Put finalize code here that should run after combat and non-combat move have both completed
        {
            m_moveLastType = 0;
            TacticalCenter.get(data, player).ClearEnemyListSortedByPriority();
        }
    }

    protected void tech(ITechDelegate techDelegate, GameData data, PlayerID player)
    {
        NotifyGameRound(data);
        NotifyPlayer(player);
        NotifyPhaseType(PhaseType.Tech);
        Tech.tech(this, techDelegate, data, player);
    }

    protected void purchase(boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaser, GameData data, PlayerID player)
    {
        NotifyGameRound(data);
        NotifyPlayer(player);
        NotifyPhaseType(PhaseType.Purchase);
        Purchase.purchase(this, purchaseForBid, PUsToSpend, purchaser, data, player);
    }

    @Override
    protected void battle(IBattleDelegate battleDelegate, GameData data, PlayerID player)
    {
        NotifyGameRound(data);
        NotifyPlayer(player);
        NotifyPhaseType(PhaseType.Battle);
        //Generally all AI's will follow the same logic: loop until all battles are fought
        //Rather than trying to analyze battles to figure out which must be fought before others,
        //as in the case of a naval battle preceding an amphibious attack,
        //keep trying to fight every battle until all battles are resolved
        while (true)
        {
            BattleListing listing = battleDelegate.getBattles();            
            if(listing.getBattles().isEmpty() && listing.getStrategicRaids().isEmpty()) //All fought
                break;

            Iterator<Territory> raidBattles = listing.getStrategicRaids().iterator();
            //Fight strategic bombing raids
            while(raidBattles.hasNext())
            {
                Territory current = raidBattles.next();
                String error = battleDelegate.fightBattle(current, true);
            }

            Iterator<Territory> nonRaidBattles = listing.getBattles().iterator();
            //Fight normal battles
            while(nonRaidBattles.hasNext())
            {
                Territory current = nonRaidBattles.next();
                setBattleInfo(current);
                String error = battleDelegate.fightBattle(current, false);
            }
            setBattleInfo(null);
        }
    }

    private void setBattleInfo(Territory bTerr)
    {
        m_battleTer = bTerr;
    }

    private Territory getBattleTerritory()
    {
        return m_battleTer;
    }

    Territory m_battleTer = null;
    public Collection<Unit> scrambleQuery(GUID battleID, Collection<Territory> possibleTerritories, String message)
    {
        return null;
    }

    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        DUtils.Log(Level.FINE, "Retreat query starting. Battle Ter: {0} Possible retreat locations: {1}", getBattleTerritory(), possibleTerritories);
        final GameData data = getPlayerBridge().getGameData();
        PlayerID player = GlobalCenter.CurrentPlayer;
        Territory battleTerr = getBattleTerritory();
        //BattleTer will be null if we're defending and TripleA calls this method to ask if we want to retreat(submerge) our subs when being attacked
        //PossibleTerritories will be empty if subs move in our sub ter, and our sub is 'attacking'
        if(battleTerr == null || possibleTerritories.isEmpty())
            return null; //Don't submerge
        AggregateResults results = DUtils.GetBattleResults(battleTerr, getWhoAmI(), data, DSettings.LoadSettings().CA_Retreat_determinesIfAIShouldRetreat, true);
        float retreatChance = .6F;
        if(TacticalCenter.get(data, getID()).BattleRetreatChanceAssignments.containsKey(battleTerr))
        {
            DUtils.Log(Level.FINER, "Found specific battle retreat chance assignment for territory '{0}'. Retreat Chance: {1}", battleTerr, TacticalCenter.get(data, player).BattleRetreatChanceAssignments.get(battleTerr));
            retreatChance = TacticalCenter.get(data, getID()).BattleRetreatChanceAssignments.get(battleTerr);
        }
        else //Must be attack_trade type
        {
            List<Unit> attackers = battleTerr.getUnits().getMatches(Matches.unitIsOwnedBy(getID()));
            List<Unit> defenders = battleTerr.getUnits().getMatches(Matches.unitIsEnemyOf(data, getID()));
            List<Unit> responseAttackers = DUtils.GetSPNNEnemyUnitsThatCanReach(data, battleTerr, getID(), Matches.TerritoryIsLand);
            List<Unit> responseDefenders = new ArrayList<Unit>(results.GetAverageAttackingUnitsRemaining());
            AggregateResults responseResults = DUtils.GetBattleResults(responseAttackers, responseDefenders, battleTerr, data, DSettings.LoadSettings().CA_Retreat_determinesIfAIShouldRetreat, true);
            int tradeScore = DUtils.GetTaskTradeScore(data, battleTerr, attackers, defenders, results, responseAttackers, responseDefenders, responseResults);
            DUtils.Log(Level.FINER, "Attack_Trade battle assumed. Score: {0} Required: {1}", tradeScore, DSettings.LoadSettings().TR_attackTrade_totalTradeScoreRequired);
            if(tradeScore < DSettings.LoadSettings().TR_attackTrade_totalTradeScoreRequired)
                retreatChance = 1.0F;
        }
        if(results.getAttackerWinPercent() <= retreatChance)
        {
            //Calculate best retreat ter and retreat to it
            Territory retreatTer = Battle_RetreatTerCalculator.CalculateBestRetreatTer(data, player, new ArrayList<Territory>(possibleTerritories), battleTerr);
            return retreatTer;
        }
        return null;
    }

    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        return false;
    }

    public Territory selectTerritoryForAirToLand(Collection candidates)
    {
       return (Territory) candidates.iterator().next();
    }

    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from)
    {
        List<Unit> result = new ArrayList<Unit>();
        for(Unit fighter : fightersThatCanBeMoved)
        {
            result.add(fighter);
        }
        return result;
    }

    public boolean shouldBomberBomb(Territory territory)
    {
        List<Unit> nonBomberAttackingUnits = Match.getMatches(territory.getUnits().getUnits(), new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(getWhoAmI()), Matches.UnitIsNotStrategicBomber));
    	if(nonBomberAttackingUnits.isEmpty())
            return true;
        else
            return false;
    }

    public Unit whatShouldBomberBomb(Territory territory, Collection<Unit> units)
    {
        List<Unit> factoryUnits = Match.getMatches(units, Matches.UnitIsFactory);
    	if(factoryUnits.isEmpty())
            return null;
        return (Unit) Match.getNMatches(units, 1, Matches.UnitIsFactory);
    }

    public int[] selectFixedDice(int numRolls, int hitAt, boolean hitOnlyIfEquals, String message)
    {
        int[] dice = new int[numRolls];
        for (int i = 0; i < numRolls; i++)
        {
            dice[i] = (int)Math.ceil(Math.random() * 6);
        }
        return dice;
    }

    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        DUtils.Log(Level.FINE, "Select casualties method called. Message from MustFightBattle class: {0}", message);
        return SelectCasualties.selectCasualties(this, getGameData(), selectFrom, dependents, count, message, dice, hit, defaultCasualties, battleID);
    }

    @Override
    public void reportError(String error)
    {
        DUtils.Log(Level.FINE, "Error message reported: {0}", error);
        if (error.equals("Wrong number of casualties selected") || error.equals("Cannot remove enough units of those types"))
        {
            SelectCasualties.NotifyCasualtySelectionError(error);
        }
    }
}
