package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.Arrays;
import java.util.List;


/**
 * TODO: WORK IN PROGRESS!.
 */
class ModeratorsTabModel {

  /**
   * Indicates current user can add or remove moderators.
   */
  boolean isModeratorAdmin() {
    return true;
  }

  static List<String> getModeratorList() {
    return Arrays.asList("Prastle", "Champ");
  }
}
