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
package games.strategy.triplea.ai.Dynamix_AI;

import games.strategy.triplea.ai.Dynamix_AI.Others.BattleCalculationType;
import games.strategy.triplea.ai.Dynamix_AI.Others.ThreatInvalidationType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 
 * @author Stephen
 */
public class DSettings implements Serializable
{
	private static final long serialVersionUID = 2696071717784800413L;
	public int PurchaseWait_AL = 750;
	public int CombatMoveWait_AL = 750;
	public int NonCombatMoveWait_AL = 750;
	public int PlacementWait_AL = 750;
	public boolean AllowCalcingDecrease = true;
	public int CalcingDecreaseToPercentage = 75;
	public boolean AIC_disableAllUnitPurchasesAndPlacements = false;
	public boolean AIC_disableAllUnitMovements = false;
	public int CA_Purchase_determinesUnitThatWouldHelpTargetInvasionMost = 50;
	public int CA_CM_determinesIfTaskCreationsWorthwhileBasedOnTakeoverChance = 250;
	public int CA_CMNCM_sortsPossibleTaskRecruitsForOptimalAttackDefense = 25;
	public int CA_CMNCM_determinesIfTasksRequirementsAreMetEnoughForRecruitingStop = 250;
	public int CA_CM_determinesIfTradeTasksRequirementsAreMetEnoughForRecruitingStop = 750;
	public int CA_CM_determinesAttackResultsToSeeIfTaskWorthwhile = 500;
	public int CA_CMNCM_determinesResponseResultsToSeeIfTaskWorthwhile = 500;
	public int CA_CMNCM_determinesIfTaskEndangersCap = 250;
	public int CA_CMNCM_determinesSurvivalChanceAfterTaskToSeeIfToInvalidateAttackers = 250;
	public int CA_NCM_determinesSurvivalChanceOfFromTerAfterMoveToSeeIfToCancelMove = 100;
	public int CA_Retreat_determinesIfAIShouldRetreat = 2000;
	public boolean LimitLogHistory = true;
	public int LimitLogHistoryTo = 5;
	public BattleCalculationType BattleCalculationMethod = BattleCalculationType.BattleCalculator;
	public boolean EnableResourceCollectionMultiplier = false;
	public int ResourceCollectionMultiplyPercent = 100;
	public boolean EnableResourceCollectionIncreaser = false;
	public int ResourceCollectionIncreaseAmount = 0;
	public boolean EnableUnitPlacementMultiplier = false;
	public int UnitPlacementMultiplyPercent = 100;
	public boolean TR_enableAttackOffensive = true;
	public int TR_attackOffensive_takeoverChanceRequired = 70;
	public int TR_attackOffensive_counterAttackSurvivalChanceRequired = 55;
	public int TR_attackOffensive_counterAttackTradeScoreRequiredToBypassSurvivalRequirement = 20;
	public int TR_attackOffensive_Neutrals_takeoverChanceRequired = 90;
	public int TR_attackOffensive_Neutrals_counterAttackSurvivalChanceRequired = 60;
	public int TR_attackOffensive_Neutrals_counterAttackTradeScoreRequiredToBypassSurvivalRequirement = 20;
	public int TR_attackOffensive_Capitals_takeoverChanceRequired = 55;
	public int TR_attackOffensive_Capitals_counterAttackSurvivalChanceRequired = 0;
	public int TR_attackOffensive_Capitals_counterAttackTradeScoreRequiredToBypassSurvivalRequirement = 20;
	public boolean TR_enableAttackStabalize = true;
	public int TR_attackStabalize_takeoverChanceRequired = 55;
	public int TR_attackStabalize_counterAttackSurvivalChanceRequired = 25;
	public int TR_attackStabalize_counterAttackTradeScoreRequiredToBypassSurvivalRequirement = 20;
	public boolean TR_enableAttackTrade = true;
	public int TR_attackTrade_totalTradeScoreRequired = 10;
	public int TR_attackTrade_certaintyOfReachingDesiredNumberOfLeftoverLandUnitsRequired = 50;
	public boolean TR_enableAttackLandGrab = true;
	public boolean TR_attackLandGrab_onlyGrabLandIfWeCanBlitzIt = false;
	public boolean TR_enableReinforceFrontLine = true;
	public int TR_reinforceFrontLine_enemyAttackSurvivalChanceRequired = 50;
	public int TR_reinforceFrontline_enemyAttackTradeScoreRequiredToBypassRequirements = 20;
	public boolean TR_enableReinforceStabalize = true;
	public int TR_reinforceStabalize_enemyAttackSurvivalChanceRequired = 50;
	public boolean TR_enableReinforceBlock = true;
	public boolean CR_enableCallForLandGrab = true;
	public boolean CR_enableCallForDefensiveFront = true;
	public boolean CR_enableCallForCapitalDefense = true;
	public boolean EnableAILogging = false;
	public Level AILoggingDepth = Level.FINEST;
	public int AA_resourcePercentageThatMustExistForFactoryBuy = 50;
	public int AA_maxUnitTypesForPurchaseMix = 5;
	public ThreatInvalidationType AA_threatInvalidationType = ThreatInvalidationType.AroundHotspot;
	public int AA_threatInvalidationAroundHotspotRadius = 1;
	public int AA_percentageOfResistedThreatThatTasksInvalidate = 50;
	public boolean AA_ignoreAlliedUnitsAsDefenses = false;
	public int AA_survivalChanceOfLandingTerRequiredForPlaneRecruit = 75;
	private static DSettings s_lastSettings = null;
	private static String PROGRAM_SETTINGS = "Program Settings";
	
	public static DSettings LoadSettings()
	{
		if (s_lastSettings == null)
		{
			DSettings result = new DSettings();
			try
			{
				final byte[] pool = Preferences.userNodeForPackage(Dynamix_AI.class).getByteArray(PROGRAM_SETTINGS, null);
				if (pool != null)
				{
					result = (DSettings) new ObjectInputStream(new ByteArrayInputStream(pool)).readObject();
				}
			} catch (final Exception ex)
			{
			}
			if (result == null)
			{
				result = new DSettings();
			}
			s_lastSettings = result;
			return result;
		}
		else
			return s_lastSettings;
	}
	
	public static void SaveSettings(final DSettings settings)
	{
		s_lastSettings = settings;
		ObjectOutputStream outputStream = null;
		try
		{
			final ByteArrayOutputStream pool = new ByteArrayOutputStream(10000);
			outputStream = new ObjectOutputStream(pool);
			outputStream.writeObject(settings);
			final Preferences prefs = Preferences.userNodeForPackage(Dynamix_AI.class);
			prefs.putByteArray(PROGRAM_SETTINGS, pool.toByteArray());
			try
			{
				prefs.flush();
			} catch (final BackingStoreException ex)
			{
				ex.printStackTrace();
			}
		} catch (final Exception ex)
		{
			ex.printStackTrace();
		} finally
		{
			try
			{
				if (outputStream != null)
					outputStream.close();
			} catch (final Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
