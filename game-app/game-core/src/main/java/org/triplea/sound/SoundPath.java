package org.triplea.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NonNls;

/** Contains the sound file names and the directory of all sound files. */
public final class SoundPath {
  // MAKE SURE TO ADD NEW SOUNDS TO THE getAllSoundOptions() METHOD! (or else the user's preference
  // will not be saved)

  @NonNls public static final String CLIP_BATTLE_X_HIT = "_hit";
  @NonNls public static final String CLIP_BATTLE_X_MISS = "_miss";
  @NonNls public static final String CLIP_BATTLE_X_PREFIX = "battle_";
  @NonNls public static final String CLIP_CHAT_MESSAGE = "chat_message";
  @NonNls public static final String CLIP_CHAT_SLAP = "chat_slap";
  @NonNls public static final String CLIP_CHAT_JOIN_GAME = "chat_join_game";
  @NonNls public static final String CLIP_CLICK_BUTTON = "click_button";
  @NonNls public static final String CLIP_TRIGGERED_DEFEAT_SOUND = "defeat_";
  @NonNls public static final String CLIP_TRIGGERED_VICTORY_SOUND = "victory_";
  @NonNls public static final String CLIP_BATTLE_AA_HIT = "battle_aa_hit";
  @NonNls public static final String CLIP_BATTLE_AA_MISS = "battle_aa_miss";
  @NonNls public static final String CLIP_BATTLE_AIR = "battle_air";
  @NonNls public static final String CLIP_BATTLE_AIR_SUCCESSFUL = "battle_air_successful";
  @NonNls public static final String CLIP_BATTLE_BOMBARD = "battle_bombard";
  @NonNls public static final String CLIP_BATTLE_FAILURE = "battle_failure";
  @NonNls public static final String CLIP_BATTLE_LAND = "battle_land";
  @NonNls public static final String CLIP_BATTLE_RETREAT_AIR = "battle_retreat_air";
  @NonNls public static final String CLIP_BATTLE_RETREAT_LAND = "battle_retreat_land";
  @NonNls public static final String CLIP_BATTLE_RETREAT_SEA = "battle_retreat_sea";
  @NonNls public static final String CLIP_BATTLE_RETREAT_SUBMERGE = "battle_retreat_submerge";
  @NonNls public static final String CLIP_BATTLE_SEA_NORMAL = "battle_sea_normal";
  @NonNls public static final String CLIP_BATTLE_SEA_SUBS = "battle_sea_subs";
  @NonNls public static final String CLIP_BATTLE_SEA_SUCCESSFUL = "battle_sea_successful";
  @NonNls public static final String CLIP_BATTLE_STALEMATE = "battle_stalemate";
  @NonNls public static final String CLIP_BOMBING_ROCKET = "bombing_rocket";
  @NonNls public static final String CLIP_BOMBING_STRATEGIC = "bombing_strategic";
  @NonNls public static final String CLIP_GAME_START = "game_start";
  @NonNls public static final String CLIP_GAME_WON = "game_won";
  @NonNls public static final String CLIP_TRIGGERED_NOTIFICATION_SOUND = "notification_";
  @NonNls public static final String CLIP_PHASE_BATTLE = "phase_battle";
  @NonNls public static final String CLIP_PHASE_END_TURN = "phase_end_turn";
  @NonNls public static final String CLIP_PHASE_MOVE_COMBAT = "phase_move_combat";
  @NonNls public static final String CLIP_PHASE_MOVE_NONCOMBAT = "phase_move_noncombat";
  @NonNls public static final String CLIP_PHASE_PLACEMENT = "phase_placement";
  @NonNls public static final String CLIP_PHASE_POLITICS = "phase_politics";
  @NonNls public static final String CLIP_PHASE_PURCHASE = "phase_purchase";
  @NonNls public static final String CLIP_PHASE_TECHNOLOGY = "phase_technology";
  @NonNls public static final String CLIP_PHASE_USER_ACTIONS = "phase_user_actions";
  @NonNls public static final String CLIP_PLACED_AIR = "placed_air";
  @NonNls public static final String CLIP_PLACED_INFRASTRUCTURE = "placed_infrastructure";
  @NonNls public static final String CLIP_PLACED_LAND = "placed_land";
  @NonNls public static final String CLIP_PLACED_SEA = "placed_sea";
  @NonNls public static final String CLIP_POLITICAL_ACTION_FAILURE = "political_action_failure";

  @NonNls
  public static final String CLIP_POLITICAL_ACTION_SUCCESSFUL = "political_action_successful";

  @NonNls public static final String CLIP_REQUIRED_YOUR_TURN_SERIES = "required_your_turn_series";
  @NonNls public static final String CLIP_TECHNOLOGY_FAILURE = "technology_failure";
  @NonNls public static final String CLIP_TECHNOLOGY_SUCCESSFUL = "technology_successful";
  @NonNls public static final String CLIP_TERRITORY_CAPTURE_BLITZ = "territory_capture_blitz";
  @NonNls public static final String CLIP_TERRITORY_CAPTURE_CAPITAL = "territory_capture_capital";
  @NonNls public static final String CLIP_TERRITORY_CAPTURE_LAND = "territory_capture_land";
  @NonNls public static final String CLIP_TERRITORY_CAPTURE_SEA = "territory_capture_sea";
  @NonNls public static final String CLIP_USER_ACTION_FAILURE = "user_action_failure";
  @NonNls public static final String CLIP_USER_ACTION_SUCCESSFUL = "user_action_successful";

  private SoundPath() {}

  public static Set<String> getAllSoundOptions() {
    return getAllSoundOptionsWithDescription().keySet();
  }

  private static Map<String, String> getAllSoundOptionsWithDescription() {
    final Map<String, String> soundOptions = new HashMap<>();

    soundOptions.put(SoundPath.CLIP_CHAT_MESSAGE, "Chat Messaging");
    soundOptions.put(SoundPath.CLIP_CHAT_SLAP, "Chat Slapping");
    soundOptions.put(SoundPath.CLIP_CHAT_JOIN_GAME, "Joined Chat");
    soundOptions.put(SoundPath.CLIP_CLICK_BUTTON, "Click Button");
    soundOptions.put(SoundPath.CLIP_GAME_START, "Game Start");
    soundOptions.put(SoundPath.CLIP_GAME_WON, "Game Won");
    soundOptions.put(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, "Start of Your Turn Control");
    soundOptions.put(SoundPath.CLIP_BATTLE_AA_HIT, "AA Hit");
    soundOptions.put(SoundPath.CLIP_BATTLE_AA_MISS, "AA Miss");
    soundOptions.put(SoundPath.CLIP_BATTLE_AIR, "Air Battle");
    soundOptions.put(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, "Air Battle Won");
    soundOptions.put(SoundPath.CLIP_BATTLE_BOMBARD, "Bombardment");
    soundOptions.put(SoundPath.CLIP_BATTLE_FAILURE, "Battle Lost");
    soundOptions.put(SoundPath.CLIP_BATTLE_LAND, "Land Battle");
    soundOptions.put(SoundPath.CLIP_BATTLE_RETREAT_AIR, "Air Retreat");
    soundOptions.put(SoundPath.CLIP_BATTLE_RETREAT_LAND, "Land Retreat");
    soundOptions.put(SoundPath.CLIP_BATTLE_RETREAT_SEA, "Sea Retreat");
    soundOptions.put(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, "Sub Submerge");
    soundOptions.put(SoundPath.CLIP_BATTLE_SEA_NORMAL, "Naval Battle");
    soundOptions.put(SoundPath.CLIP_BATTLE_SEA_SUBS, "Submarine Battle");
    soundOptions.put(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL, "Sea Battle Won");
    soundOptions.put(SoundPath.CLIP_BATTLE_STALEMATE, "Battle Stalemate");
    soundOptions.put(SoundPath.CLIP_BOMBING_ROCKET, "Rocket Attack");
    soundOptions.put(SoundPath.CLIP_BOMBING_STRATEGIC, "Strategic Bombing");
    soundOptions.put(SoundPath.CLIP_PHASE_BATTLE, "Phase: Battle");
    soundOptions.put(SoundPath.CLIP_PHASE_END_TURN, "Phase: End Turn");
    soundOptions.put(SoundPath.CLIP_PHASE_MOVE_COMBAT, "Phase: Combat Movement");
    soundOptions.put(SoundPath.CLIP_PHASE_MOVE_NONCOMBAT, "Phase: NonCombat Movement");
    soundOptions.put(SoundPath.CLIP_PHASE_PLACEMENT, "Phase: Placement");
    soundOptions.put(SoundPath.CLIP_PHASE_POLITICS, "Phase: Politics");
    soundOptions.put(SoundPath.CLIP_PHASE_PURCHASE, "Phase: Purchase Phase");
    soundOptions.put(SoundPath.CLIP_PHASE_TECHNOLOGY, "Phase: Technology");
    soundOptions.put(SoundPath.CLIP_PHASE_USER_ACTIONS, "Phase: User Actions");
    soundOptions.put(SoundPath.CLIP_PLACED_AIR, "Place Air Units");
    soundOptions.put(SoundPath.CLIP_PLACED_INFRASTRUCTURE, "Place Infrastructure");
    soundOptions.put(SoundPath.CLIP_PLACED_LAND, "Place Land Units");
    soundOptions.put(SoundPath.CLIP_PLACED_SEA, "Place Sea Units");
    soundOptions.put(SoundPath.CLIP_POLITICAL_ACTION_FAILURE, "Political Action Failed");
    soundOptions.put(SoundPath.CLIP_POLITICAL_ACTION_SUCCESSFUL, "Political Action Successful");
    soundOptions.put(SoundPath.CLIP_TECHNOLOGY_FAILURE, "Technology Failed");
    soundOptions.put(SoundPath.CLIP_TECHNOLOGY_SUCCESSFUL, "Technology Researched");
    soundOptions.put(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, "Captured By Blitzing");
    soundOptions.put(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, "Captured Capital");
    soundOptions.put(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, "Captured Land Territory");
    soundOptions.put(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, "Captured Sea Zone");
    soundOptions.put(SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND, "Triggered Notification Sound");
    soundOptions.put(SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND, "Triggered Defeat Sound");
    soundOptions.put(SoundPath.CLIP_TRIGGERED_VICTORY_SOUND, "Triggered Victory Sound");
    soundOptions.put(SoundPath.CLIP_USER_ACTION_FAILURE, "Action Operation Failed");
    soundOptions.put(SoundPath.CLIP_USER_ACTION_SUCCESSFUL, "Action Operation Successful");
    return soundOptions;
  }

  static List<SoundOptionCheckBox> getSoundOptions() {
    return getAllSoundOptionsWithDescription().entrySet().stream()
        .map(e -> new SoundOptionCheckBox(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }
}
