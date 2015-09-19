package games.strategy.sound;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

class ClipCache {
  private final HashMap<URL, Clip> clipMap = new HashMap<URL, Clip>();
  private final List<URL> cacheOrder = new ArrayList<URL>();

  // MacOS and Linux can only handle 30 or 32 sound files being open at same time,
  // so we'll be safe and pick 24
  private final int maxSize = 24;

  public ClipCache() {}

  public synchronized Clip get(final URL file) {
    Clip clip = clipMap.get(file);
    if (clip != null) {
      cacheOrder.remove(file);
      cacheOrder.add(file);
      return clip;
    }
    if (clipMap.size() >= maxSize) {
      final URL leastPlayed = cacheOrder.get(0);
      final Clip leastClip = clipMap.remove(leastPlayed);
      leastClip.stop();
      leastClip.flush();
      leastClip.close();
      cacheOrder.remove(leastPlayed);
    }
    clip = createClip(file);
    clipMap.put(file, clip);
    cacheOrder.add(file);
    return clip;
  }
  
  private static synchronized Clip createClip(final URL clipFile) {
    try {
      final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
      final AudioFormat format = audioInputStream.getFormat();
      final DataLine.Info info = new DataLine.Info(Clip.class, format);
      final Clip clip = (Clip) AudioSystem.getLine(info);
      clip.open(audioInputStream);
      return clip;
    }
    // these can happen if the sound isnt configured, its not that bad.
    catch (final LineUnavailableException e) {
      e.printStackTrace(System.out);
    } catch (final IOException e) {
      e.printStackTrace(System.out);
    } catch (final UnsupportedAudioFileException e) {
      e.printStackTrace(System.out);
    }
    return null;
  }

  public synchronized void removeAll() {
    for (final Clip clip : clipMap.values()) {
      clip.stop();
      clip.flush();
      clip.close();
    }
    clipMap.clear();
    cacheOrder.clear();
  }

}
