package games.strategy.engine.data.gameparser;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class GameParsingValidation {

  private GameData data;

  void validate() throws GameParseException {
    // validate unit attachments
    for (final UnitType u : data.getUnitTypeList()) {
      validateAttachments(u);
    }
    for (final Territory t : data.getMap()) {
      validateAttachments(t);
    }
    for (final Resource r : data.getResourceList().getResources()) {
      validateAttachments(r);
    }
    for (final GamePlayer r : data.getPlayerList().getPlayers()) {
      validateAttachments(r);
    }
    for (final RelationshipType r : data.getRelationshipTypeList().getAllRelationshipTypes()) {
      validateAttachments(r);
    }
    for (final TerritoryEffect r : data.getTerritoryEffectList().values()) {
      validateAttachments(r);
    }
    for (final TechAdvance r : data.getTechnologyFrontier().getTechs()) {
      validateAttachments(r);
    }
    // if relationships are used, every player should have a relationship with every other player
    validateRelationships();
  }

  private void validateRelationships() throws GameParseException {
    // for every player
    for (final GamePlayer player : data.getPlayerList()) {
      // in relation to every player
      for (final GamePlayer player2 : data.getPlayerList()) {
        // See if there is a relationship between them
        if ((data.getRelationshipTracker().getRelationshipType(player, player2) == null)) {
          // or else throw an exception!
          throw new GameParseException(
              "No relation set for: " + player.getName() + " and " + player2.getName());
        }
      }
    }
  }

  private void validateAttachments(final Attachable attachable) throws GameParseException {
    for (final IAttachment a : attachable.getAttachments().values()) {
      a.validate(data);
    }
  }

  static void validateForeachVariables(
      final List<String> foreachVariables,
      final Map<String, List<String>> variables,
      final String foreach)
      throws GameParseException {
    if (foreachVariables.isEmpty()) {
      return;
    }
    if (!variables.keySet().containsAll(foreachVariables)) {
      throw new GameParseException("Attachment has invalid variables in foreach: " + foreach);
    }
    final int length = variables.get(foreachVariables.get(0)).size();
    for (final String foreachVariable : foreachVariables) {
      final List<String> foreachValue = variables.get(foreachVariable);
      if (length != foreachValue.size()) {
        throw new GameParseException(
            "Attachment foreach variables must have same number of elements: " + foreach);
      }
    }
  }

  void checkThatAllUnitsHaveAttachments() throws GameParseException {
    final Collection<UnitType> errors = new ArrayList<>();
    for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      if (ua == null) {
        errors.add(ut);
      }
    }
    if (!errors.isEmpty()) {
      throw new GameParseException(
          data.getGameName()
              + " does not have unit attachments for: "
              + MyFormatter.defaultNamedToTextList(errors));
    }
  }
}
