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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.prefs.Preferences;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility for loading and playing sound clips.
 * 
 * Stores a preference in the user preferences for being silent.
 * The property will persist and be reloaded after the virtual machine
 * has been stopped and restarted.
 */
public class ClipPlayer
{
	private static ClipPlayer s_clipPlayer;
	private boolean m_beSilent = true; // true until we get better sounds
	private final HashSet<String> m_mutedClips = new HashSet<String>();
	private final HashMap<String, Clip> m_sounds = new HashMap<String, Clip>();
	
	// standard settings
	private static final String SOUND_PREFERENCE = "beSilent2";
	
	public static synchronized ClipPlayer getInstance()
	{
		if (s_clipPlayer == null)
		{
			s_clipPlayer = new ClipPlayer();
			SoundPath.preLoadSounds(SoundPath.SoundType.GENERAL);
		}
		return s_clipPlayer;
	}
	
	/**
	 * Singleton.
	 */
	private ClipPlayer()
	{
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		m_beSilent = prefs.getBoolean(SOUND_PREFERENCE, true); // true until we get better sounds
	}
	
	/*
	 * If set to true, no sounds will play.
	 * 
	 * This property is persisted using the java.util.prefs API, and will
	 * persist after the vm has stopped.
	 * 
	 * @param aBool
	 *            new value for m_beSilent
	public void setBeSilent(final boolean aBool)
	{
		m_beSilent = aBool;
		
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.putBoolean(SOUND_PREFERENCE, m_beSilent);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	 */

	/*
	 * @param aBool
	 *            whether to mute or unmute all clips
	private void muteAllClips(boolean aBool)
	{
		if (aBool)
			m_mutedClips.addAll(m_sounds.keySet());
		else
			m_mutedClips.clear();
	}
	 */

	public ArrayList<IEditableProperty> getSoundOptions(final SoundPath.SoundType sounds)
	{
		return SoundPath.getSoundOptions(sounds);
	}
	
	public boolean isMuted(final String clipName)
	{
		return m_mutedClips.contains(clipName);
	}
	
	public void setMute(final String clipName, final boolean value)
	{
		if (value == true)
			m_mutedClips.add(clipName);
		else
			m_mutedClips.remove(clipName);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 */
	static public void play(final String clipName)
	{
		getInstance().playClip(clipName);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 */
	private void playClip(final String clipName)
	{
		final Clip clip = loadClip(clipName);
		if (clip != null)
		{
			clip.setFramePosition(0);
			clip.loop(0);
		}
	}
	
	/**
	 * To reduce the delay when the clip is first played, we can preload clips here.
	 * 
	 * @param clipName
	 *            name of the clip
	 */
	public void preLoadClip(final String clipName)
	{
		loadClip(clipName);
	}
	
	private Clip loadClip(final String clipName)
	{
		if (m_beSilent || isMuted(clipName))
			return null;
		Clip clip;
		if (m_sounds.containsKey(clipName))
		{
			clip = m_sounds.get(clipName);
		}
		else
		{
			clip = loadClip(new File(SoundPath.SOUNDS_DIRECTORY.getAbsolutePath().concat(File.separator + clipName)));
			m_sounds.put(clipName, clip);
		}
		return clip;
	}
	
	private synchronized Clip loadClip(final File clipFile)
	{
		try
		{
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
			final AudioFormat format = audioInputStream.getFormat();
			final DataLine.Info info = new DataLine.Info(Clip.class, format);
			final Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(audioInputStream);
			return clip;
		}
		// these can happen if the sound isnt configured, its not that bad.
		catch (final LineUnavailableException e)
		{
			e.printStackTrace(System.out);
		} catch (final IOException e)
		{
			e.printStackTrace(System.out);
		} catch (final UnsupportedAudioFileException e)
		{
			e.printStackTrace(System.out);
		} catch (final RuntimeException re)
		{
			re.printStackTrace(System.out);
		}
		return null;
	}
}
