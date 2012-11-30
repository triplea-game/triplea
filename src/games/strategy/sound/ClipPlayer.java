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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
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
	private boolean m_beSilent = false;
	private final HashSet<String> m_mutedClips = new HashSet<String>();
	private final HashMap<String, List<Clip>> m_sounds = new HashMap<String, List<Clip>>();
	private final ResourceLoader m_resourceLoader;
	private final Set<String> m_subFolders = new HashSet<String>();
	
	// standard settings
	private static final String SOUND_PREFERENCE_GLOBAL_SWITCH = "beSilent2";
	private static final String SOUND_PREFERENCE_PREFIX = "sound_";
	
	public static synchronized ClipPlayer getInstance()
	{
		if (s_clipPlayer == null)
		{
			s_clipPlayer = new ClipPlayer(ResourceLoader.getMapResourceLoader(null));
			SoundPath.preLoadSounds(SoundPath.SoundType.GENERAL);
		}
		return s_clipPlayer;
	}
	
	public static synchronized ClipPlayer getInstance(final ResourceLoader resourceLoader, final GameData data)
	{
		// make a new clip player if we switch resource loaders (ie: if we switch maps)
		if (s_clipPlayer == null || s_clipPlayer.m_resourceLoader != resourceLoader)
		{
			s_clipPlayer = new ClipPlayer(resourceLoader, data);
			SoundPath.preLoadSounds(SoundPath.SoundType.GENERAL);
		}
		return s_clipPlayer;
	}
	
	private ClipPlayer(final ResourceLoader resourceLoader)
	{
		m_resourceLoader = resourceLoader;
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		m_beSilent = prefs.getBoolean(SOUND_PREFERENCE_GLOBAL_SWITCH, false);
		final HashSet<String> choices = SoundPath.getAllSoundOptions();
		// until we get better sounds, all sounds start as muted, except for Slapping
		choices.remove(SoundPath.CLIP_CHAT_SLAP);
		final boolean slapMuted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + SoundPath.CLIP_CHAT_SLAP, false);
		if (slapMuted)
			m_mutedClips.add(SoundPath.CLIP_CHAT_SLAP);
		for (final String sound : choices)
		{
			final boolean muted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + sound, false); // true until we get better sounds
			if (muted)
				m_mutedClips.add(sound);
		}
	}
	
	private ClipPlayer(final ResourceLoader resourceLoader, final GameData data)
	{
		this(resourceLoader);
		for (final PlayerID p : data.getPlayerList().getPlayers())
		{
			m_subFolders.add(p.getName());
		}
	}
	
	/**
	 * If set to true, no sounds will play.
	 * 
	 * This property is persisted using the java.util.prefs API, and will
	 * persist after the vm has stopped.
	 * 
	 * @param aBool
	 *            new value for m_beSilent
	 */
	public static void setBeSilent(final boolean aBool)
	{
		final ClipPlayer clipPlayer = getInstance();
		clipPlayer.m_beSilent = aBool;
		
		final Preferences prefs = Preferences.userNodeForPackage(clipPlayer.getClass());
		prefs.putBoolean(SOUND_PREFERENCE_GLOBAL_SWITCH, clipPlayer.m_beSilent);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static boolean getBeSilent()
	{
		final ClipPlayer clipPlayer = getInstance();
		return clipPlayer.m_beSilent;
	}
	
	// please avoid unnecessary calls of this
	private void putSoundInPreferences(final String clip, final boolean isMuted)
	{
		final ClipPlayer clipPlayer = getInstance();
		final Preferences prefs = Preferences.userNodeForPackage(clipPlayer.getClass());
		prefs.putBoolean(SOUND_PREFERENCE_PREFIX + clip, isMuted);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
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
		// we want to avoid unnecessary calls to preferences
		final boolean isCurrentCorrect = m_mutedClips.contains(clipName) == value;
		if (isCurrentCorrect)
			return;
		if (value == true)
			m_mutedClips.add(clipName);
		else
			m_mutedClips.remove(clipName);
		putSoundInPreferences(clipName, value);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 * @param subFolder
	 *            String - the name of the player, or null
	 */
	static public void play(final String clipName, final String subFolder)
	{
		getInstance().playClip(clipName, subFolder);
	}
	
	/**
	 * 
	 * @param clipName
	 *            String - the file name of the clip
	 */
	private void playClip(final String clipName, final String subFolder)
	{
		final Clip clip = loadClip(clipName, subFolder);
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
		loadClip(clipName, null);
		for (final String sub : m_subFolders)
		{
			loadClip(clipName, sub);
		}
	}
	
	private Clip loadClip(final String clipName, final String subFolder)
	{
		if (m_beSilent || isMuted(clipName))
			return null;
		final Clip clip = loadClipPath(clipName + (subFolder == null ? "" : ("_" + subFolder)), (subFolder != null));
		if (clip == null)
			return loadClipPath(clipName, false);
		return clip;
	}
	
	private Clip loadClipPath(final String pathName, final boolean subFolder)
	{
		Clip clip;
		if (m_sounds.containsKey(pathName))
		{
			final List<Clip> availableSounds = m_sounds.get(pathName);
			if (availableSounds == null || availableSounds.isEmpty())
				return null;
			Collections.shuffle(availableSounds); // we want to pick a random sound from this folder, as users don't like hearing the same ones over and over again
			clip = availableSounds.get(0);
		}
		else
		{
			final URL thisSoundFolderURL = m_resourceLoader.getResource("sounds" + File.separator + pathName);
			if (thisSoundFolderURL == null)
			{
				if (!subFolder)
					System.out.println("No Sounds Found For: " + pathName);
				m_sounds.put(pathName, new ArrayList<Clip>());
				return null;
			}
			URI thisSoundFolderURI;
			File thisSoundFolder;
			try
			{
				thisSoundFolderURI = thisSoundFolderURL.toURI();
				thisSoundFolder = new File(thisSoundFolderURI);
			} catch (final URISyntaxException e)
			{
				thisSoundFolder = new File(thisSoundFolderURL.getPath());
			}
			if (thisSoundFolder == null || !thisSoundFolder.exists() || !thisSoundFolder.isDirectory())
			{
				m_sounds.put(pathName, new ArrayList<Clip>());
				return null;
			}
			final List<Clip> availableSounds = new ArrayList<Clip>();
			for (final File sound : thisSoundFolder.listFiles())
			{
				if (!(sound.getName().endsWith(".wav") || sound.getName().endsWith(".au") || sound.getName().endsWith(".aiff") || sound.getName().endsWith(".midi")))
					continue;
				final Clip newClip = loadClip(sound);// m_resourceLoader.getResourceAsStream("sounds" + File.separator + clipName));
				if (newClip != null)
					availableSounds.add(newClip);
			}
			if (availableSounds.isEmpty())
			{
				m_sounds.put(pathName, new ArrayList<Clip>());
				return null;
			}
			clip = availableSounds.get(0);
			m_sounds.put(pathName, availableSounds);
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
	/*
	private synchronized Clip loadClip(final InputStream inputStream)
	{
		try
		{
			final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
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
	*/
}
