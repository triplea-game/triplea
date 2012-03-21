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
import games.strategy.engine.framework.GameRunner;

import java.io.File;
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
	public static final File SOUNDS_DIRECTORY = new File(GameRunner.getRootFolder(), "/sounds");
	
	
	public static enum SoundType
	{
		GENERAL, TRIPLEA
	}
	
	// MAKE SURE TO ADD NEW SOUNDS TO THE getAllSoundOptions() METHOD! (or else the user's preference will not be saved)
	// standard sounds (files can be found in corresponding data/... folder to this package)
	public static final String CLIP_MESSAGE = "message.wav";
	public static final String CLIP_SLAP = "slap.wav";
	public static final String CLIP_START_GAME = "start_game.wav";
	public static final String CLIP_START_YOUR_TURN = "start_your_turn.wav";
	// not used so far but in the folder
	public static final String CLIP_ACTION_REQUIRED = "action_required.wav";
	public static final String CLIP_BUTTON_CLICK = "button_click.wav";
	public static final String CLIP_PLOT_CLICK = "plot_click.wav";
	
	// TripleA sounds
	public static final String CLIP_BOMB = "strat_bomb.wav";
	// not used so far but in the folder
	public static final String CLIP_ROCKET = "rocket.wav";
	public static final String CLIP_TECH = "tech.wav";
	public static final String CLIP_NAVAL_BATTLE = "naval_battle.wav";
	public static final String CLIP_LAND_BATTLE = "terrain_battle.wav";
	public static final String CLIP_CAPTURE = "capture.wav";
	
	public static void preLoadSounds(final SoundType sounds)
	{
		final ClipPlayer clipPlayer = ClipPlayer.getInstance();
		switch (sounds)
		{
			case GENERAL:
				clipPlayer.preLoadClip(SoundPath.CLIP_START_GAME);
				clipPlayer.preLoadClip(SoundPath.CLIP_MESSAGE);
				clipPlayer.preLoadClip(SoundPath.CLIP_SLAP);
				clipPlayer.preLoadClip(SoundPath.CLIP_START_YOUR_TURN);
				break;
			case TRIPLEA:
				clipPlayer.preLoadClip(CLIP_BOMB);
				break;
		}
	}
	
	public static HashSet<String> getAllSoundOptions()
	{
		final HashSet<String> rVal = new HashSet<String>();
		rVal.add(CLIP_START_GAME);
		rVal.add(CLIP_MESSAGE);
		rVal.add(CLIP_SLAP);
		rVal.add(CLIP_START_YOUR_TURN);
		// rVal.add(CLIP_ACTION_REQUIRED);
		// rVal.add(CLIP_BUTTON_CLICK);
		// rVal.add(CLIP_PLOT_CLICK);
		rVal.add(CLIP_BOMB);
		// rVal.add(CLIP_ROCKET);
		// rVal.add(CLIP_TECH);
		// rVal.add(CLIP_NAVAL_BATTLE);
		// rVal.add(CLIP_LAND_BATTLE);
		// rVal.add(CLIP_CAPTURE);
		return rVal;
	}
	
	public static ArrayList<IEditableProperty> getSoundOptions(final SoundType sounds)
	{
		final ArrayList<IEditableProperty> rVal = new ArrayList<IEditableProperty>();
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_MESSAGE, "Messaging"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_SLAP, "Slapping"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_START_GAME, "Game Start"));
		rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_START_YOUR_TURN, "Start Your Turn"));
		switch (sounds)
		{
			case TRIPLEA:
				rVal.add(new SoundOptionCheckBox(SoundPath.CLIP_BOMB, "Strategic Bombing"));
				break;
		}
		return rVal;
	}
	
}
