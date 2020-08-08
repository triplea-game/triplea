package org.triplea.sound;

import com.google.common.base.Splitter;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.advanced.AdvancedPlayer;
import lombok.extern.java.Log;
import org.triplea.io.FileUtils;
import org.triplea.java.UrlStreams;

/**
 * Utility for loading and playing sound clips. Stores a preference in the user preferences for
 * being silent. The property will persist and be reloaded after the virtual machine has been
 * stopped and restarted. <br>
 * <br>
 * How it works: <br>
 * <b>Sound.Default.Folder</b>=ww2 <br>
 * This is the "key" that tells the engine which sound folder to use as the DEFAULT sound folder.
 * <br>
 * The default folders are as follows: <br>
 * "<b>ww2</b>" (which should cover ww1 - ww2 - ww3 sounds), <br>
 * "<b>preindustrial</b>" (anything from muskets/cannons (1500) to right before ww1 (1900), <br>
 * "<b>classical</b>" (the ancient era, anything before cannons became a mainstay (10,000 bce - 1500
 * ad) <br>
 * "<b>future</b>" (sci-fi, spaceships, lasers, etc) <br>
 * <br>
 * After this, you can specify specific sounds if you want, using the "sound key location" (aka:
 * sound map folder). <br>
 * The sound key location is the exact folder name for a sound you want, located under the "generic"
 * folder. What I mean by this is that all sound key locations that triplea supports, are the names
 * of all the folders in the "assets/sounds/generic/" folder. <br>
 * example: <br>
 * <b>battle_aa_miss</b>=ww2/battle_aa_miss;future/battle_aa_miss/battle_aa_miss_01_ufo_flyby.mp3
 * <br>
 * "battle_aa_miss" is one of the folders under "generic", therefore it is a "sound location key"
 * <br>
 * We can set this equal to any list of sounds paths, each separated by a semicolon (;). The engine
 * will pick one at random each time we need to play this sound. <br>
 * The "sound path" can be a "folder" or a "file". If it is a folder, we will use all the sounds in
 * that folder. If it is a file, we will only use that file. We can use a file and folder and
 * another file and another folder, all together. <br>
 * Example: "<b>ww2/battle_aa_miss</b>" is the sound path for a folder, so we will use all the
 * sounds in that folder. "<b>future/battle_aa_miss/battle_aa_miss_01_ufo_flyby.mp3</b>" is a
 * specific file, so we will use just this file. Because we use both of these together, the engine
 * will make a list of all the files in that folder, plus that single file we specified, then it
 * will randomly pick one of this whole list every time it needs to play the "battle_aa_miss" sound.
 * <br>
 * <br>
 * So, lets say that you want to play 2 sounds, for the "battle_land" sound key. One of them is
 * located at "tripleainstallfolder/assets/sounds/generic/battle_land_01_angry_drumming_noise.mp3".
 * The other is located at
 * "tripleainstallfolder/assets/sounds/classical/battle_land_02_war_trumpets.mp3". Then the entry
 * would look like this: <br>
 * battle_land=
 * generic/battle_land_01_angry_drumming_noise.mp3;classical/battle_land_02_war_trumpets.mp3 <br>
 * If you wanted it to also play every single sound in the
 * "tripleainstallfolder/assets/sounds/ww2/battle_land/" folder, then you would add that folder to
 * path: <br>
 * battle_land= generic/battle_land_01_angry_drumming_noise.mp3;
 * classical/battle_land_02_war_trumpets.mp3;ww2/battle_land <br>
 * <br>
 * Furthermore, we can customize the sound key by adding "_nationName" onto the end of it. So if you
 * want a specific sound for a german land attack, then use: <br>
 * battle_land<b>_Germans</b>=misc/battle_land/battle_land_Germans_panzers_and_yelling_in_german.mp3
 * <br>
 * You can use nation specific sound keys for almost all sounds, though things like game_start, or
 * chat_message, will never use them. <br>
 * <br>
 * <br>
 * <b>You do not need to specify every single "sound key". This is why/because we have the
 * "Sound.Default.Folder".</b> <br>
 * <br>
 * The logic is as follows: <br>
 * Engine needs to play the "game_start" sound. <br>
 * 1. Check for a sound.properties file. <br>
 * 2. If none exists, pretend that one exists and that it only contains this line:
 * "Sound.Default.Folder=ww2" <br>
 * 3. Look in the sound.properties file for the specific sound key "game_start" <br>
 * 4. Create a list of all sounds that the key includes. If no key, then just use all the sounds in
 * "Sound.Default.Folder/sound_key/" (which for us would be "ww2/game_start/" folder). <br>
 * 5. If no sounds are found, then use all the sounds located at "generic/sound_key/" (which for us
 * would be "generic/game_start"). (if any sounds are found in step 4 above, then we ignore the
 * generic folder completely) <br>
 * 6. Randomize the list's order, then pick one, and play the sound.
 */
@Log
public class ClipPlayer {
  private static final String ASSETS_SOUNDS_FOLDER = "sounds";
  private static final String SOUND_PREFERENCE_PREFIX = "sound_";
  private static final String MP3_SUFFIX = ".mp3";
  private static ClipPlayer clipPlayer;

  protected final Map<String, List<URL>> sounds = new HashMap<>();
  private final Set<String> mutedClips = new HashSet<>();
  private final ResourceLoader resourceLoader;

  private ClipPlayer(final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
    final Preferences prefs = Preferences.userNodeForPackage(ClipPlayer.class);
    final Set<String> choices = SoundPath.getAllSoundOptions();

    for (final String sound : choices) {
      final boolean muted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + sound, false);
      if (muted) {
        mutedClips.add(sound);
      }
    }
  }

  static synchronized ClipPlayer getInstance() {
    return Optional.ofNullable(clipPlayer)
        .orElseGet(() -> new ClipPlayer(ResourceLoader.getGameEngineAssetLoader()));
  }

  public static synchronized void getInstance(final ResourceLoader resourceLoader) {
    // make a new clip player if we switch resource loaders (ie: if we switch maps)
    if (clipPlayer == null || clipPlayer.resourceLoader != resourceLoader) {
      // make a new clip player with our new resource loader
      clipPlayer = new ClipPlayer(resourceLoader);
    }
  }

  boolean isSoundClipMuted(final String clipName) {
    if (mutedClips.contains(clipName)) {
      return true;
    }
    if (!SoundPath.getAllSoundOptions().contains(clipName)) {
      // for custom sound clips, with custom paths, silence based on more similar sound clip
      // settings
      if (clipName.startsWith(SoundPath.CLIP_BATTLE_X_PREFIX)
          && clipName.endsWith(SoundPath.CLIP_BATTLE_X_HIT)) {
        return mutedClips.contains(SoundPath.CLIP_BATTLE_AA_HIT);
      }
      if (clipName.startsWith(SoundPath.CLIP_BATTLE_X_PREFIX)
          && clipName.endsWith(SoundPath.CLIP_BATTLE_X_MISS)) {
        return mutedClips.contains(SoundPath.CLIP_BATTLE_AA_MISS);
      }
      if (clipName.startsWith(SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND)) {
        return mutedClips.contains(SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND);
      }
      if (clipName.startsWith(SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND)) {
        return mutedClips.contains(SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND);
      }
      if (clipName.startsWith(SoundPath.CLIP_TRIGGERED_VICTORY_SOUND)) {
        return mutedClips.contains(SoundPath.CLIP_TRIGGERED_VICTORY_SOUND);
      }
    }
    return false;
  }

  void setSoundClipMute(final String clipName, final boolean value) {
    // we want to avoid unnecessary calls to preferences
    final boolean isCurrentCorrect = mutedClips.contains(clipName) == value;
    if (isCurrentCorrect) {
      return;
    }
    if (value) {
      mutedClips.add(clipName);
    } else {
      mutedClips.remove(clipName);
    }
    final Preferences prefs = Preferences.userNodeForPackage(ClipPlayer.class);
    prefs.putBoolean(SOUND_PREFERENCE_PREFIX + clipName, value);
  }

  /**
   * Flushes sounds preferences to persisted data store. This method is *slow* and resource
   * expensive.
   */
  void saveSoundPreferences() {
    final Preferences prefs = Preferences.userNodeForPackage(ClipPlayer.class);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  public static void play(final String clipName) {
    play(clipName, null);
  }

  /**
   * Plays the specified player-specific clip.
   *
   * @param clipPath - the folder containing sound clips to be played. One of the sound clip files
   *     will be chosen at random.
   * @param gamePlayer - the name of the player, or null
   */
  public static void play(final String clipPath, final GamePlayer gamePlayer) {
    getInstance().playClip(clipPath, gamePlayer);
  }

  private void playClip(final String clipName, final GamePlayer gamePlayer) {
    if (!isSoundEnabled() || isSoundClipMuted(clipName)) {
      return;
    }
    // run in a new thread, so that we do not delay the game
    String folder = clipName;
    if (gamePlayer != null) {
      folder += "_" + gamePlayer.getName();
    }

    final URI clip = loadClip(folder).orElse(loadClip(clipName).orElse(null));
    // clip may still be null, we try to load all phases/all sound, for example: clipName =
    // "phase_technology", folder =
    // "phase_technology_Japanese"

    if (clip != null) {
      new Thread(
              () -> {
                try {
                  final Optional<InputStream> inputStream = UrlStreams.openStream(clip.toURL());
                  if (inputStream.isPresent()) {
                    final AudioDevice audioDevice =
                        FactoryRegistry.systemRegistry().createAudioDevice();
                    new AdvancedPlayer(inputStream.get(), audioDevice).play();
                  }
                } catch (final Exception e) {
                  log.log(Level.SEVERE, "Failed to play: " + clip, e);
                }
              })
          .start();
    }
  }

  private boolean isSoundEnabled() {
    return !Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"))
        && !"true".equals(System.getenv("java.awt.headless"))
        && ClientSetting.soundEnabled.getSetting();
  }

  private Optional<URI> loadClip(final String clipName) {
    return (isSoundEnabled() && !isSoundClipMuted(clipName))
        ? Optional.ofNullable(loadClipPath(clipName))
        : Optional.empty();
  }

  private URI loadClipPath(final String pathName) {
    if (!sounds.containsKey(pathName)) {
      // parse sounds for the first time
      sounds.put(pathName, parseClipPaths(pathName));
    }
    final List<URL> availableSounds = sounds.get(pathName);
    if (availableSounds == null || availableSounds.isEmpty()) {
      return null;
    }
    // we want to pick a random sound from this folder, as users don't like hearing the same ones
    // over and over again
    Collections.shuffle(availableSounds);
    try {
      return availableSounds.get(0).toURI();
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The user may or may not have a sounds.properties file. If they do not, we should have a default
   * folder (ww2) that we use for sounds. Because we do not want a lot of duplicate sound files, we
   * also have a "generic" sound folder. If a sound cannot be found for a soundpath using the
   * sounds.properties or default folder, then we try to find one in the generic folder. The
   * sounds.properties file can specify all the sounds to use for a specific sound path (multiple
   * per path). If there is no key for that path, we try by the default way. <br>
   * <br>
   * Example sounds.properties keys:<br>
   * Sound.Default.Folder=ww2<br>
   * battle_aa_miss=
   * ww2/battle_aa_miss/battle_aa_miss_01_aa_artillery_and_flyby.mp3;ww2/battle_aa_miss/
   * battle_aa_miss_02_just_aa_artillery. mp3<br>
   * phase_purchase_Germans=phase_purchase_Germans/game_start_Germans_01_anthem.mp3
   */
  private List<URL> parseClipPaths(final String pathName) {
    // Check if there is a sound.properties path override for this resource
    String resourcePath = SoundProperties.getInstance(resourceLoader).getProperty(pathName);
    if (resourcePath == null) {
      resourcePath =
          SoundProperties.getInstance(resourceLoader).getDefaultEraFolder() + "/" + pathName;
    }
    resourcePath = resourcePath.replace('\\', '/');
    final List<URL> availableSounds = new ArrayList<>();
    if ("NONE".equals(resourcePath)) {
      sounds.put(pathName, availableSounds);
      return availableSounds;
    }
    for (final String path : Splitter.on(';').split(resourcePath)) {
      availableSounds.addAll(findClipFiles(ASSETS_SOUNDS_FOLDER + "/" + path));
    }
    if (availableSounds.isEmpty()) {
      final String genericPath = SoundProperties.GENERIC_FOLDER + "/" + pathName;
      availableSounds.addAll(findClipFiles(ASSETS_SOUNDS_FOLDER + "/" + genericPath));
    }
    return availableSounds;
  }

  /**
   * Returns a collection of clip files found at the specified location.
   *
   * @param resourceAndPathUrl (URL uses '/', not File.separator or '\')
   */
  protected List<URL> findClipFiles(final String resourceAndPathUrl) {
    final List<URL> availableSounds = new ArrayList<>();
    final URL thisSoundUrl = resourceLoader.getResource(resourceAndPathUrl);
    if (thisSoundUrl == null) {
      return availableSounds;
    }
    final URI thisSoundUri;
    File thisSoundFile;
    // we are checking to see if this is a file, to see if it is a directory, or a sound, or a
    // zipped directory, or a
    // zipped sound. There might be a better way to do this...
    try {
      thisSoundUri = thisSoundUrl.toURI();
      try {
        thisSoundFile = new File(thisSoundUri);
      } catch (final Exception e) {
        try {
          thisSoundFile = new File(thisSoundUri.getPath());
        } catch (final Exception e3) {
          thisSoundFile = new File(thisSoundUrl.getPath());
        }
      }
    } catch (final URISyntaxException e1) {
      try {
        thisSoundFile = new File(thisSoundUrl.getPath());
      } catch (final Exception e4) {
        thisSoundFile = null;
      }
    } catch (final Exception e2) {
      thisSoundFile = null;
    }

    if (thisSoundFile == null || !thisSoundFile.exists()) {
      // final long startTime = System.currentTimeMillis();
      // we are probably using zipped sounds. there might be a better way to do this...
      final String soundFilePath = thisSoundUrl.getPath();
      if (soundFilePath != null && soundFilePath.length() > 5 && soundFilePath.contains(".zip!")) {
        // so the URL with a zip or jar in it, will start with "file:", and unfortunately when you
        // make a file and test
        // if it exists, if it starts with that it doesn't exist
        final int index1 =
            Math.max(
                0,
                Math.min(
                    soundFilePath.length(),
                    soundFilePath.contains("file:") ? soundFilePath.indexOf("file:") + 5 : 0));
        final String zipFilePath =
            soundFilePath.substring(
                index1,
                Math.max(index1, Math.min(soundFilePath.length(), soundFilePath.lastIndexOf("!"))));
        if (zipFilePath.length() > 5 && zipFilePath.endsWith(".zip")) {
          // the file path may have spaces, which in a URL are equal to %20, but if we make a file
          // using that it will fail, so we need to decode
          final String decoded = URLDecoder.decode(zipFilePath, StandardCharsets.UTF_8);

          try {
            final File zipFile = new File(decoded);
            if (zipFile.exists()) {
              try (ZipFile zf = new ZipFile(zipFile)) {
                final List<URL> newSounds =
                    zf.stream()
                        .filter(zipElement -> isZippedMp3(zipElement, resourceAndPathUrl))
                        .map(ZipEntry::getName)
                        .map(
                            name -> {
                              try {
                                return resourceLoader.getResource(name);
                              } catch (final RuntimeException e) {
                                log.log(Level.SEVERE, "Failed to load sound resource: " + name, e);
                              }
                              return null;
                            })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                availableSounds.addAll(newSounds);
              }
            }
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to read sound file: " + decoded, e);
          }
        }
      }
    } else {
      // we must be using unzipped sounds
      if (!thisSoundFile.isDirectory()) {
        if (!isSoundFileNamed(thisSoundFile)) {
          return availableSounds;
        }
        availableSounds.add(thisSoundUrl);
      }
    }

    if (thisSoundFile != null) {
      if (thisSoundFile.isDirectory()) {
        for (final File soundFile : FileUtils.listFiles(thisSoundFile)) {
          if (isSoundFileNamed(soundFile)) {
            try {
              final URL individualSoundUrl = soundFile.toURI().toURL();
              availableSounds.add(individualSoundUrl);
            } catch (final MalformedURLException e) {
              final String msg =
                  "Error " + e.getMessage() + " with sound file: " + soundFile.getPath();
              log.log(Level.SEVERE, msg, e);
            }
          }
        }
      } else {
        if (!isSoundFileNamed(thisSoundFile)) {
          return availableSounds;
        }
        availableSounds.add(thisSoundUrl);
      }
    }

    return availableSounds;
  }

  private static boolean isZippedMp3(final ZipEntry zipElement, final String resourceAndPathUrl) {
    return zipElement != null
        && zipElement.getName() != null
        && zipElement.getName().contains(resourceAndPathUrl)
        && zipElement.getName().endsWith(MP3_SUFFIX);
  }

  private static boolean isSoundFileNamed(final File soundFile) {
    return soundFile.getName().endsWith(MP3_SUFFIX);
  }
}
