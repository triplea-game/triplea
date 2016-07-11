package games.strategy.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import games.strategy.engine.data.properties.IEditableProperty;

/**
 * Contains the sound file names and the directory of all sound files.
 */
public class SoundPath {
  // MAKE SURE TO ADD NEW SOUNDS TO THE getAllSoundOptions() METHOD! (or else the user's preference will not be saved)

  // standard sounds (files can be found in corresponding data/... folder to this package)
  public static final String CLIP_CHAT_MESSAGE = "chat_message";
  public static final String CLIP_CHAT_SLAP = "chat_slap";
  public static final String CLIP_CHAT_JOIN_GAME = "chat_join_game";
  // TODO
  public static final String CLIP_CLICK_BUTTON = "click_button";
  // TODO
  public static final String CLIP_CLICK_PLOT = "click_plot";
  public static final String CLIP_GAME_START = "game_start";
  public static final String CLIP_GAME_WON = "game_won";
  // TODO
  public static final String CLIP_REQUIRED_ACTION = "required_action";
  public static final String CLIP_REQUIRED_YOUR_TURN_SERIES = "required_your_turn_series";
  // TripleA sounds:
  // custom AA Guns:
  public static final String CLIP_BATTLE_X_PREFIX = "battle_";
  public static final String CLIP_BATTLE_X_HIT = "_hit";
  public static final String CLIP_BATTLE_X_MISS = "_miss";
  // custom triggered notification sounds:
  public static final String CLIP_TRIGGERED_NOTIFICATION_SOUND = "notification_";
  public static final String CLIP_TRIGGERED_DEFEAT_SOUND = "defeat_";
  public static final String CLIP_TRIGGERED_VICTORY_SOUND = "victory_";
  // normal sounds:
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
  public static final String CLIP_BATTLE_STALEMATE = "battle_stalemate";
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
  public static final String CLIP_PHASE_USER_ACTIONS = "phase_user_actions";
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
  public static final String CLIP_USER_ACTION_FAILURE = "user_action_failure";
  public static final String CLIP_USER_ACTION_SUCCESSFUL = "user_action_successful";

  public static Set<String> getAllSoundOptions() {
    return getAllSoundOptionsWithDescription().keySet();
  }

  public static Map<String,String> getAllSoundOptionsWithDescription() {
    final Map<String,String> rVal = new HashMap<>();
    
    rVal.put(SoundPath.CLIP_CHAT_MESSAGE, "Chat Messaging");
    rVal.put(SoundPath.CLIP_CHAT_SLAP, "Chat Slapping");
    rVal.put(SoundPath.CLIP_CHAT_JOIN_GAME, "Joined Chat");
    rVal.put(SoundPath.CLIP_CLICK_BUTTON, "Click Button");
    rVal.put(SoundPath.CLIP_CLICK_PLOT, "Click Plot");
    rVal.put(SoundPath.CLIP_GAME_START, "Game Start");
    rVal.put(SoundPath.CLIP_GAME_WON, "Game Won");
    rVal.put(SoundPath.CLIP_REQUIRED_ACTION, "Required Action");
    rVal.put(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, "Start of Your Turn Control");
    rVal.put(SoundPath.CLIP_BATTLE_AA_HIT, "AA Hit");
    rVal.put(SoundPath.CLIP_BATTLE_AA_MISS, "AA Miss");
    rVal.put(SoundPath.CLIP_BATTLE_AIR, "Air Battle");
    rVal.put(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, "Air Battle Won");
    rVal.put(SoundPath.CLIP_BATTLE_BOMBARD, "Bombardment");
    rVal.put(SoundPath.CLIP_BATTLE_FAILURE, "Battle Lost");
    rVal.put(SoundPath.CLIP_BATTLE_LAND, "Land Battle");
    rVal.put(SoundPath.CLIP_BATTLE_RETREAT_AIR, "Air Retreat");
    rVal.put(SoundPath.CLIP_BATTLE_RETREAT_LAND, "Land Retreat");
    rVal.put(SoundPath.CLIP_BATTLE_RETREAT_SEA, "Sea Retreat");
    rVal.put(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, "Sub Submerge");
    rVal.put(SoundPath.CLIP_BATTLE_SEA_NORMAL, "Naval Battle");
    rVal.put(SoundPath.CLIP_BATTLE_SEA_SUBS, "Submarine Battle");
    rVal.put(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL, "Sea Battle Won");
    rVal.put(SoundPath.CLIP_BATTLE_STALEMATE, "Battle Stalemate");
    rVal.put(SoundPath.CLIP_BOMBING_ROCKET, "Rocket Attack");
    rVal.put(SoundPath.CLIP_BOMBING_STRATEGIC, "Strategic Bombing");
    rVal.put(SoundPath.CLIP_PHASE_BATTLE, "Phase: Battle");
    rVal.put(SoundPath.CLIP_PHASE_END_TURN, "Phase: End Turn");
    rVal.put(SoundPath.CLIP_PHASE_MOVE_COMBAT, "Phase: Combat Movement");
    rVal.put(SoundPath.CLIP_PHASE_MOVE_NONCOMBAT, "Phase: NonCombat Movement");
    rVal.put(SoundPath.CLIP_PHASE_PLACEMENT, "Phase: Placement");
    rVal.put(SoundPath.CLIP_PHASE_POLITICS, "Phase: Politics");
    rVal.put(SoundPath.CLIP_PHASE_PURCHASE, "Phase: Purchase Phase");
    rVal.put(SoundPath.CLIP_PHASE_TECHNOLOGY, "Phase: Technology");
    rVal.put(SoundPath.CLIP_PHASE_USER_ACTIONS, "Phase: User Actions");
    rVal.put(SoundPath.CLIP_PLACED_AIR, "Place Air Units");
    rVal.put(SoundPath.CLIP_PLACED_INFRASTRUCTURE, "Place Infrastructure");
    rVal.put(SoundPath.CLIP_PLACED_LAND, "Place Land Units");
    rVal.put(SoundPath.CLIP_PLACED_SEA, "Place Sea Units");
    rVal.put(SoundPath.CLIP_POLITICAL_ACTION_FAILURE, "Political Action Failed");
    rVal.put(SoundPath.CLIP_POLITICAL_ACTION_SUCCESSFUL, "Political Action Successful");
    rVal.put(SoundPath.CLIP_TECHNOLOGY_FAILURE, "Technology Failed");
    rVal.put(SoundPath.CLIP_TECHNOLOGY_SUCCESSFUL, "Technology Researched");
    rVal.put(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, "Captured By Blitzing");
    rVal.put(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, "Captured Capital");
    rVal.put(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, "Captured Land Territory");
    rVal.put(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, "Captured Sea Zone");
    rVal.put(SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND, "Triggered Notification Sound");
    rVal.put(SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND, "Triggered Defeat Sound");
    rVal.put(SoundPath.CLIP_TRIGGERED_VICTORY_SOUND, "Triggered Victory Sound");
    rVal.put(SoundPath.CLIP_USER_ACTION_FAILURE, "Action Operation Failed");
    rVal.put(SoundPath.CLIP_USER_ACTION_SUCCESSFUL, "Action Operation Successful");
    return rVal;
  }

  public static List<IEditableProperty> getSoundOptions() {
    List<IEditableProperty> soundBoxes = new ArrayList<>();
    getAllSoundOptionsWithDescription().forEach(
        (path, description) -> soundBoxes.add(new SoundOptionCheckBox(path, description)));
    return soundBoxes;
  }
}
