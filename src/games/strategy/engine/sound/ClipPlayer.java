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


package games.strategy.engine.sound;


import javax.sound.sampled.*;
import java.io.*;
import java.util.prefs.*;
import java.util.*;

/**
 * Utility for loading and playing sound clips.
 *
 * Stores a preference in the user preferences for being silent.
 * The property will persist and be reloaded after the virtual machine
 * has been stopped and restarted.
 */

public class ClipPlayer
{
  private static final String SOUND_PREFERENCE = "beSilent";
  private static ClipPlayer s_clipPlayer;
  private boolean m_beSilent = false;
  private HashMap m_sounds = new HashMap();

  public static synchronized ClipPlayer getInstance()
  {
    if(s_clipPlayer == null)
      s_clipPlayer = new ClipPlayer();
    return s_clipPlayer;
  }

  /**
   * Singleton.
   */
  private ClipPlayer()
  {
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    m_beSilent = prefs.getBoolean(SOUND_PREFERENCE, false);
  }

  /**
   * Will we make noise?
   */
  public boolean getBeSilent()
  {
    return m_beSilent;
  }

  /**
   * If set to true, no sounds will play.
   *
   * This property is persisted using the java.util.prefs API, and will
   * persist after the vm has stopped.
   */
  public void setBeSilent(boolean aBool)
  {
    m_beSilent = aBool;
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    prefs.putBoolean(SOUND_PREFERENCE, m_beSilent);
    try
    {
      prefs.flush();
    }
    catch (BackingStoreException ex)
    {
      ex.printStackTrace();
    }

  }


  /**
   * Clips are located using Class.getResource().  All clips must have a position relative to a
   * class file.
   *
   * @param clipName String - the file name of the clip
   * @param resourceLocation Class - the location of the clip.
   */
  public void playClip(String clipName, Class resourceLocation)
  {
    if(m_beSilent)
      return;

    Clip clip = loadClip(clipName, resourceLocation);
    if(clip != null)
    {
      clip.setFramePosition(0);
      clip.loop(0);
    }
  }

  /**
   * To reduce the delay when the clip is first played, we can preload clips here.
   */
  public void preLoadClip(String clipName, Class resourceLocation)
  {
    loadClip(clipName, resourceLocation);
  }

  private Clip loadClip(String clipName, Class resourceLocation)
  {
    Clip clip;
    if(m_sounds.containsKey(clipName))
    {
      clip = (Clip) m_sounds.get(clipName);
    }
    else
    {
      clip = loadClip(resourceLocation.getResourceAsStream(clipName));
      m_sounds.put(clipName, clip);
    }
    return clip;
  }

  private synchronized Clip loadClip(InputStream input)
  {
    try
    {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(input);

      AudioFormat format = audioInputStream.getFormat();
      DataLine.Info info = new DataLine.Info(Clip.class, format);

      Clip clip = (Clip) AudioSystem.getLine(info);
      clip.open(audioInputStream);
      return clip;

    }
    catch (LineUnavailableException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch(UnsupportedAudioFileException e)
    {
      e.printStackTrace();
    }
    return null;
  }

//  public static void main(String[] args) throws Exception
//  {
//
//    getInstance().playClip("start.wav", ClipPlayer.class);
//
//    Object lock = new Object();
//    synchronized(lock)
//    {
//      try
//      {
//        lock.wait(5000);
//      }
//      catch (InterruptedException ex)
//      {
//      }
//    }
//
//    getInstance().playClip("start.wav", ClipPlayer.class);
//
//  }

}
