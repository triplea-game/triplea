package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.proAI.logging.LogUI;
import games.strategy.triplea.ai.proAI.logging.LogUtils;
import games.strategy.triplea.ai.strongAI.StrongAI;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.oddsCalculator.ta.ConcurrentOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.ui.TripleAFrame;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pro AI.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProAI extends StrongAI
{
	private final static Logger s_logger = Logger.getLogger(ProAI.class.getName());
	
	private static final IOddsCalculator s_battleCalculator = new ConcurrentOddsCalculator("ProAI"); // if non-static, then only need 1 for the entire AI instance and must be shutdown when AI is gc'ed.
	
	// Utilities
	private final ProBattleUtils proBattleUtils;
	
	// Phases
	private final ProCombatMoveAI proCombatMoveAI;
	private final ProRetreatAI proRetreatAI;
	
	public ProAI(final String name, final String type)
	{
		super(name, type);
		proBattleUtils = new ProBattleUtils(this);
		proCombatMoveAI = new ProCombatMoveAI(proBattleUtils);
		proRetreatAI = new ProRetreatAI(this, proBattleUtils);
	}
	
	public static void Initialize(final TripleAFrame frame)
	{
		LogUI.initialize(frame); // Must be done first
		LogUtils.log(Level.FINE, "Initialized Hard AI");
	}
	
	public static void ShowSettingsWindow()
	{
		LogUtils.log(Level.FINE, "Showing Hard AI settings window");
		LogUI.showSettingsWindow();
	}
	
	public static Logger getLogger()
	{
		return s_logger;
	}
	
	public static void clearCache()
	{
		s_battleCalculator.setGameData(null); // is static, set to null so that we don't keep the data around after a game is exited.
		LogUI.clearCachedInstances();
	}
	
	public IOddsCalculator getCalc()
	{
		return s_battleCalculator;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.ai.strongAI.StrongAI#move(boolean, games.strategy.triplea.delegate.remote.IMoveDelegate, games.strategy.engine.data.GameData, games.strategy.engine.data.PlayerID)
	 */
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		s_battleCalculator.setGameData(data);
		if (nonCombat)
		{
			doNonCombatMove(moveDel, player);
		}
		else
		{
			LogUI.notifyStartOfRound(data.getSequence().getRound(), player.getName());
			proCombatMoveAI.move(moveDel, data, player);
		}
		pause();
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.ai.strongAI.StrongAI#retreatQuery(games.strategy.net.GUID, boolean, games.strategy.engine.data.Territory, java.util.Collection, java.lang.String)
	 */
	@Override
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		s_battleCalculator.setGameData(getGameData());
		return proRetreatAI.retreatQuery(battleID, submerge, battleTerritory, possibleTerritories, message);
	}
	
}
