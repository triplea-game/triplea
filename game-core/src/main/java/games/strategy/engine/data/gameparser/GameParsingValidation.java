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
public class GameParsingValidation {

  private final GameData data;

  public List<String> validate() {
    final List<String> validationErrors = new ArrayList<>();
    // validate unit attachments
    for (final UnitType u : data.getUnitTypeList()) {
      validationErrors.addAll(validateAttachments(u));
    }
    for (final Territory t : data.getMap()) {
      validationErrors.addAll(validateAttachments(t));
    }
    for (final Resource r : data.getResourceList().getResources()) {
      validationErrors.addAll(validateAttachments(r));
    }
    for (final GamePlayer r : data.getPlayerList().getPlayers()) {
      validationErrors.addAll(validateAttachments(r));
    }
    for (final RelationshipType r : data.getRelationshipTypeList().getAllRelationshipTypes()) {
      validationErrors.addAll(validateAttachments(r));
    }
    for (final TerritoryEffect r : data.getTerritoryEffectList().values()) {
      validationErrors.addAll(validateAttachments(r));
    }
    for (final TechAdvance r : data.getTechnologyFrontier().getTechs()) {
      validationErrors.addAll(validateAttachments(r));
    }
    // if relationships are used, every player should have a relationship with every other player
    validationErrors.addAll(validateRelationships());
    validationErrors.addAll(checkThatAllUnitsHaveAttachments());
    return validationErrors;
  }

  private List<String> validateRelationships() {
    final List<String> validationErrors = new ArrayList<>();
    // for every player
    for (final GamePlayer player : data.getPlayerList()) {
      // in relation to every player
      for (final GamePlayer player2 : data.getPlayerList()) {
        // See if there is a relationship between them
        if (data.getRelationshipTracker().getRelationship(player, player2) == null) {
          validationErrors.add(
              "No relation set for: " + player.getName() + " and " + player2.getName());
        }
      }
    }
    return validationErrors;
  }

  private List<String> validateAttachments(final Attachable attachable) {
    final List<String> validationErrors = new ArrayList<>();
    for (final IAttachment a : attachable.getAttachments().values()) {
      try {
        a.validate(data);
      } catch (final GameParseException e) {
        validationErrors.add(e.getMessage());
      }
    }
    return validationErrors;
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

  private List<String> checkThatAllUnitsHaveAttachments() {
    final Collection<UnitType> errors = new ArrayList<>();
    for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      if (ua == null) {
        errors.add(ut);
      }
    }

    if (!errors.isEmpty()) {
      return List.of(
          data.getGameName()
              + " does not have unit attachments for: "
              + MyFormatter.defaultNamedToTextList(errors));

    } else {
      return List.of();
    }
  }
}
