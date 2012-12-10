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
package games.strategy.sound;

import games.strategy.engine.data.properties.IEditableProperty;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Contains the sound file names and the directory of all sound files.
 * 
 * @author Frigoref
 * 
 */
public class SoundPath
{
	// the sounds directory is based on the resource loader, because it could change based on the map or skin
	
	public static enum SoundType
	{
		GENERAL, TRIPLEA
	}
	
	// MAKE SURE TO ADD NEW SOUNDS TO THE getAllSoundOptions() METHOD! (or else the user's preference will not be saved)
	
	// standard sounds (files can be found in corresponding data/... folder to this package)
	public static final String CLIP_CHAT_MESSAGE = "chat_message";
	public static final String CLIP_CHAT_SLAP = "chat_slap";
	public static final String CLIP_CLICK_BUTTON = "click_button"; // TODO
	public static final String CLIP_CLICK_PLOT = "click_plot"; // TODO
	public static final String CLIP_GAME_START = "game_start";
	public static final String CLIP_GAME_WON = "game_won";
	public static final String CLIP_REQUIRED_ACTION = "required_action"; // TODO
	public static final String CLIP_REQUIRED_YOUR_TURN_SERIES = "required_your_turn_series";
	
	// TripleA sounds
	public static final String CLIP_BATTLE_AA_HIT = "battle_aa_hit";
	public static final String CLIP_BATTLE_AA_MISS = "battle_aa_miss";
	public static final String CLIP_BATTLE_AIR = "battle_air";
	public static final String CLIP_BATTLE_AIR_SUCCESSFUL = "battle_air_successful";
	public static final String CLIP_BATTLE_BOMBARD = "battle_bombard";
	public static final String CLIP_BATTLE_FAILURE = "battle_failure";
	public static final String CLIP_BATTLE_LAND = "battle_land";
	public static final String CLIP_BATTLE_RETREAT_AIR = "battle_retreat_air";
	public static final String CLIP_BATTLE_RETREAT_LAND = "battle_retreat_land";
	public static final String CLIP_BATTLE_RETREAT_SEA = "battle_retreat_sea";
	public static final String CLIP_BATTLE_RETREAT_SUBMERGE = "battle_retreat_submerge";
	public static final String CLIP_BATTLE_SEA_NORMAL = "battle_sea_normal";
	public static final String CLIP_BATTLE_SEA_SUBS = "battle_sea_subs";
	public static final String CLIP_BATTLE_SEA_SUCCESSFUL = "battle_sea_successful";
	public static final String CLIP_BOMBING_ROCKET = "bombing_rocket";
	public static final String CLIP_BOMBING_STRATEGIC = "bombing_strategic";
	public static final String CLIP_PHASE_BATTLE = "phase_battle";
	public static final String CLIP_PHASE_END_TURN = "phase_end_turn";
	public static final String CLIP_PHASE_MOVE_COMBAT = "phase_move_combat";
	public static final String CLIP_PHASE_MOVE_NONCOMBAT = "phase_move_noncombat";
	public static final String CLIP_PHASE_PLACEMENT = "phase_placement";
	public static final String CLIP_PHASE_POLITICS = "phase_politics";
	public static final String CLIP_PHASE_PURCHASE = "phase_purchase";
	public static final String CLIP_PHASE_TECHNOLOGY = "phase_technology";
	public static final String CLIP_PLACED_AIR = "placed_air";
	public static final String CLIP_PLACED_INFRASTRUCTURE = "placed_infrastructure";
	public static final String CLIP_PLACED_LAND = "placed_land";
	public static final String CLIP_PLACED_SEA = "placed_sea";
	public static final String CLIP_POLITICAL_ACTION_FAILURE = "political_action_failure";
	public static final String CLIP_POLITICAL_ACTION_SUCCESSFUL = "political_action_successful";
	public static final String CLIP_TECHNOLOGY_FAILURE = "technology_failure";
	public static final String CLIP_TECHNOLOGY_SUCCESSFUL = "technology_successful";
	public static final String CLIP_TERRITORY_CAPTURE_BLITZ = "territory_capture_blitz";
	public static final String CLIP_TERRITORY_CAPTURE_CAPITAL = "territory_capture_capital";
	public static final String CLIP_TERRITORY_CAPTURE_LAND = "territory_capture_land";
	public static final String CLIP_TERRITORY_CAPTURE_SEA = "territory_capture_sea";
	
	public static void preLoadSounds(final SoundType sounds)
	{
		final ClipPlayer clipPlayer = ClipPlayer.getInstance();
		switch (sounds)
		{
			case GENERAL:
				clipPlayer.preLoadClip(CLIP_CHAT_MESSAGE);
				clipPlayer.preLoadClip(CLIP_CHAT_SLAP);
				clipPlayer.preLoadClip(CLIP_CLICK_BUTTON);
				clipPlayer.preLoadClip(CLIP_CLICK_PLOT);
				clipPlayer.preLoadClip(CLIP_GAME_START);
				clipPlayer.preLoadClip(CLIP_GAME_WON);
				clipPlayer.preLoadClip(CLIP_REQUIRED_ACTION);
				clipPlayer.preLoadClip(CLIP_REQUIRED_YOUR_TURN_SERIES);
				break;
			case TRIPLEA:
				clipPlayer.preLoadClip(CLIP_BATTLE_AA_HIT);
				clipPlayer.preLoadClip(CLIP_BATTLE_AA_MISS);
				clipPlayer.preLoadClip(CLIP_BATTLE_AIR);
				clipPlayer.preLoadClip(CLIP_BATTLE_AIR_SUCCESSFUL);
				clipPlayer.preLoadClip(CLIP_BATTLE_BOMBARD);
				clipPlayer.preLoadClip(CLIP_BATTLE_FAILURE);
				clipPlayer.preLoadClip(CLIP_BATTLE_LAND);
				clipPlayer.preLoadClip(CLIP_BATTLE_RETREAT_AIR);
				clipPlayer.preLoadClip(CLIP_BATTLE_RETREAT_LAND);
				clipPlayer.preLoadClip(CLIP_BATTLE_RETREAT_SEA);
				clipPlayer.preLoadClip(CLIP_BATTLE_RETREAT_SUBMERGE);
				clipPlayer.preLoadClip(CLIP_BATTLE_SEA_NORMAL);
				clipPlayer.preLoadClip(CLIP_BATTLE_SEA_SUBS);
				clipPlayer.preLoadClip(CLIP_BATTLE_SEA_SUCCESSFUL);
				clipPlayer.preLoadClip(CLIP_BOMBING_ROCKET);
				clipPlayer.preLoadClip(CLIP_BOMBING_STRATEGIC);
				clipPlayer.preLoadClip(CLIP_PHASE_BATTLE);
				clipPlayer.preLoadClip(CLIP_PHASE_END_TURN);
				clipPlayer.preLoadClip(CLIP_PHASE_MOVE_COMBAT);
				clipPlayer.preLoadClip(CLIP_PHASE_MOVE_NONCOMBAT);
				clipPlayer.preLoadClip(CLIP_PHASE_PLACEMENT);
				clipPlayer.preLoadClip(CLIP_PHASE_POLITICS);
				clipPlayer.preLoadClip(CLIP_PHASE_PURCHASE);
				clipPlayer.preLoadClip(CLIP_PHASE_TECHNOLOGY);
				clipPlayer.preLoadClip(CLIP_PLACED_AIR);
				clipPlayer.preLoadClip(CLIP_PLACED_INFRASTRUCTURE);
				clipPlayer.preLoadClip(CLIP_PLACED_LAND);
				clipPlayer.preLoadClip(CLIP_PLACED_SEA);
				clipPlayer.preLoadClip(CLIP_POLITICAL_ACTION_FAILURE);
				clipPlayer.preLoadClip(CLIP_POLITICAL_ACTION_SUCCESSFUL);
				clipPlayer.preLoadClip(CLIP_TECHNOLOGY_FAILURE);
				clipPlayer.preLoadClip(CLIP_TECHNOLOGY_SUCCESSFUL);
				clipPlayer.preLoadClip(CLIP_TERRITORY_CAPTURE_BLITZ);
				clipPlayer.preLoadClip(CLIP_TERRITORY_CAPTURE_CAPITAL);
				clipPlayer.preLoadClip(CLIP_TERRITORY_CAPTURE_LAND);
				clipPlayer.preLoadClip(CLIP_TERRITORY_CAPTURE_SEA);
				break;
		}
	}
	
	public static HashSet<String> getAllSoundOptions()
	{
		final HashSet<String> rVal = new HashSet<String>();
		rVal.add(CLIP_CHAT_MESSAGE);
		rVal.add(CLIP_CHAT_SLAP);
		rVal.add(CLIP_CLICK_BUTTON);
		rVal.add(CLIP_CLICK_PLOT);
		rVal.add(CLIP_GAME_START);
		rVal.add(CLIP_GAME_WON);
		rVal.add(CLIP_REQUIRED_ACTION);
		rVal.add(CLIP_REQUIRED_YOUR_TURN_SERIES);
		
		rVal.add(CLIP_BATTLE_AA_HIT);
		rVal.add(CLIP_BATTLE_AA_MISS);
		rVal.add(CLIP_BATTLE_AIR);
		rVal.add(CLIP_BATTLE_AIR_SUCCESSFUL);
		rVal.add(CLIP_BATTLE_BOMBARD);
		rVal.add(CLIP_BATTLE_FAILURE);
		rVal.add(CLIP_BATTLE_LAND);
		rVal.add(CLIP_BATTLE_RETREAT_AIR);
		rVal.add(CLIP_BATTLE_RETREAT_LAND);
		rVal.add(CLIP_BATTLE_RETREAT_SEA);
		rVal.add(CLIP_BATTLE_RETREAT_SUBMERGE);
		rVal.add(CLIP_BATTLE_SEA_NORMAL);
		rVal.add(CLIP_BATTLE_SEA_SUBS);
		rVal.add(CLIP_BATTLE_SEA_SUCCESSFUL);
		rVal.add(CLIP_BOMBING_ROCKET);
		rVal.add(CLIP_BOMBING_STRATEGIC);
		rVal.add(CLIP_PHASE_BATTLE);
		rVal.add(CLIP_PHASE_END_TURN);
		rVal.add(CLIP_PHASE_MOVE_COMBAT);
		rVal.add(CLIP_PHASE_MOVE_NONCOMBAT);
		rVal.add(CLIP_PHASE_PLACEMENT);
		rVal.add(CLIP_PHASE_POLITICS);
		rVal.add(CLIP_PHASE_PURCHASE);
		rVal.add(CLIP_PHASE_TECHNOLOGY);
		rVal.add(CLIP_PLACED_AIR);
		rVal.add(CLIP_PLACED_INFRASTRUCTURE);
		rVal.add(CLIP_PLACED_LAND);
		rVal.add(CLIP_PLACED_SEA);
		rVal.add(CLIP_POLITICAL_ACTION_FAILURE);
		rVal.add(CLIP_POLITICAL_ACTION_SUCCESSFUL);
		rVal.add(CLIP_TECHNOLOGY_FAILURE);
		rVal.add(CLIP_TECHNOLOGY_SUCCESSFUL);
		rVal.add(CLIP_TERRITORY_CAPTURE_BLITZ);
		rVal.add(CLIP_TERRITORY_CAPTURE_CAPITAL);
		rVal.add(CLIP_TERRITORY_CAPTURE_LAND);
		rVal.add(CLIP_TERRITORY_CAPTURE_SEA);
		return rVal;
	}
	
	public static ArrayList<IEditableProperty> getSoundOptions(final SoundType sounds)
	{
		final ArrayList<IEditableProperty> rVal = new ArrayList<IEditableProperty>();
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_CHAT_MESSAGE, "Chat Messaging"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_CHAT_SLAP, "Chat Slapping"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_CLICK_BUTTON, "Click Button"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_CLICK_PLOT, "Click Plot"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_GAME_START, "Game Start"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_GAME_WON, "Game Won"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_REQUIRED_ACTION, "Required Action"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, "Start of Your Turn Control"));
		switch (sounds)
		{
			case TRIPLEA:
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_AA_HIT, "AA Hit"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_AA_MISS, "AA Miss"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_AIR, "Air Battle"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, "Air Battle Won"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_BOMBARD, "Bombardment"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_FAILURE, "Battle Lost"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_LAND, "Land Battle"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_RETREAT_AIR, "Air Retreat"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_RETREAT_LAND, "Land Retreat"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_RETREAT_SEA, "Sea Retreat"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, "Sub Submerge"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_SEA_NORMAL, "Naval Battle"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_SEA_SUBS, "Submarine Battle"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL, "Sea Battle Won"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BOMBING_ROCKET, "Rocket Attack"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BOMBING_STRATEGIC, "Strategic Bombing"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_BATTLE, "Phase: Battle"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_END_TURN, "Phase: End Turn"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_MOVE_COMBAT, "Phase: Combat Movement"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_MOVE_NONCOMBAT, "Phase: NonCombat Movement"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_PLACEMENT, "Phase: Placement"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_POLITICS, "Phase: Politics"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_PURCHASE, "Phase: Purchase Phase"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PHASE_TECHNOLOGY, "Phase: Technology"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PLACED_AIR, "Place Air Units"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PLACED_INFRASTRUCTURE, "Place Infrastructure"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PLACED_LAND, "Place Land Units"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_PLACED_SEA, "Place Sea Units"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_POLITICAL_ACTION_FAILURE, "Political Action Failed"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_POLITICAL_ACTION_SUCCESSFUL, "Political Action Successful"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TECHNOLOGY_FAILURE, "Technology Failed"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TECHNOLOGY_SUCCESSFUL, "Technology Researched"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, "Captured By Blitzing"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, "Captured Capital"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, "Captured Land Territory"));
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, "Captured Sea Zone"));
				break;
		}
		return rVal;
	}
}
