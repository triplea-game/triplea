package org.triplea.sound;

import com.google.common.base.Splitter;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.advanced.AdvancedPlayer;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;
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
@Slf4j
public class ClipPlayer {
  private static final String ASSETS_SOUNDS_FOLDER = "sounds";
  private static final String SOUND_PREFERENCE_PREFIX = "sound_";
  private static final String MP3_SUFFIX = ".mp3";

  private static final Set<String> mutedClips = ConcurrentHashMap.newKeySet();

  static {
    final Preferences prefs = Preferences.userNodeForPackage(ClipPlayer.class);
    final Set<String> choices = SoundPath.getAllSoundOptions();

    for (final String sound : choices) {
      final boolean muted = prefs.getBoolean(SOUND_PREFERENCE_PREFIX + sound, false);
      if (muted) {
        mutedClips.add(sound);
      }
    }
  }

  private final Map<String, List<URL>> sounds = new ConcurrentHashMap<>();

  private final ResourceLoader resourceLoader;

  public ClipPlayer(final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public ClipPlayer() {
    this(new ResourceLoader(Path.of("sounds")));
  }

  public static boolean hasAudio() {
    try {
      FactoryRegistry.systemRegistry().createAudioDevice();
      return true;
    } catch (final JavaLayerException e) {
      log.info("Unable to create audio device, is there audio on the system? " + e.getMessage(), e);
      return false;
    }
  }

  static boolean isSoundClipMuted(final String clipName) {
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

  static void setSoundClipMute(final String clipName, final boolean value) {
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
  static void saveSoundPreferences() {
    final Preferences prefs = Preferences.userNodeForPackage(ClipPlayer.class);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  public void play(final String clipName) {
    play(clipName, null);
  }

  /**
   * Plays the specified player-specific clip.
   *
   * @param clipName - the folder containing sound clips to be played. One of the sound clip files
   *     will be chosen at random.
   * @param gamePlayer - the name of the player, or null
   */
  public void play(final String clipName, @Nullable final GamePlayer gamePlayer) {
    if (!isSoundEnabled() || isSoundClipMuted(clipName)) {
      return;
    }
    // run in a new thread, so that we do not delay the game
    String folder = clipName;
    if (gamePlayer != null) {
      folder += "_" + gamePlayer.getName();
    }

    loadClipPath(folder)
        .or(() -> loadClipPath(clipName))
        .ifPresent(
            clip ->
                ThreadRunner.runInNewThread(
                    () ->
                        UrlStreams.openStream(
                            URI.create(clip.toString()),
                            inputStream -> {
                              try {
                                new AdvancedPlayer(inputStream).play();
                              } catch (final Exception e) {
                                log.error("Failed to play: " + clip, e);
                              }
                              return null;
                            })));
  }

  private boolean isSoundEnabled() {
    return !GameRunner.headless()
        && !"true".equals(System.getenv("java.awt.headless"))
        && ClientSetting.soundEnabled.getSetting()
        && hasAudio();
  }

  private Optional<URL> loadClipPath(final String pathName) {
    final List<URL> availableSounds = sounds.computeIfAbsent(pathName, this::parseClipPaths);
    if (availableSounds.isEmpty()) {
      return Optional.empty();
    }
    // we want to pick a random sound from this folder, as users
    // don't like hearing the same ones over and over again
    return Optional.of(availableSounds.get((int) (Math.random() * availableSounds.size())));
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
    String resourcePath =
        Optional.ofNullable(new SoundProperties(resourceLoader).getProperty(pathName))
            .orElseGet(
                () -> new SoundProperties(resourceLoader).getDefaultEraFolder() + "/" + pathName)
            .replace('\\', '/');
    final List<URL> availableSounds = new ArrayList<>();
    if ("NONE".equals(resourcePath)) {
      return availableSounds;
    }
    for (final String path : Splitter.on(';').split(resourcePath)) {
      availableSounds.addAll(findClipFiles(ASSETS_SOUNDS_FOLDER + "/" + path));
    }
    if (availableSounds.isEmpty()) {
      @NonNls final String genericPath = SoundProperties.GENERIC_FOLDER + "/" + pathName;
      availableSounds.addAll(findClipFiles(ASSETS_SOUNDS_FOLDER + "/" + genericPath));
    }
    return availableSounds;
  }

  /**
   * Returns a collection of clip files found at the specified location.
   *
   * @param resourceAndPathUrl (URL uses '/', not File.separator or '\')
   */
  private List<URL> findClipFiles(final String resourceAndPathUrl) {
    final URL thisSoundUrl = resourceLoader.getResource(resourceAndPathUrl);
    if (thisSoundUrl == null) {
      return List.of();
    }
    final Path thisSoundPath;
    try {
      thisSoundPath = Path.of(thisSoundUrl.toURI());
    } catch (final URISyntaxException e) {
      throw new IllegalStateException("Invalid URL format", e);
    }

    if (Files.isDirectory(thisSoundPath)) {
      try (Stream<Path> files = Files.list(thisSoundPath)) {
        return files
            .filter(ClipPlayer::hasMp3Extension)
            .flatMap(
                soundFile -> {
                  try {
                    return Stream.of(soundFile.toUri().toURL());
                  } catch (final MalformedURLException e) {
                    log.error("Error " + e.getMessage() + " with sound file: " + soundFile, e);
                    return Stream.empty();
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      } catch (final IOException e) {
        log.error("Failed to list files in directory " + thisSoundPath, e);
      }
    } else if (hasMp3Extension(thisSoundPath) && Files.isReadable(thisSoundPath)) {
      return List.of(thisSoundUrl);
    }

    return List.of();
  }

  private static boolean hasMp3Extension(final Path soundFile) {
    return soundFile.toString().endsWith(MP3_SUFFIX);
  }
}
