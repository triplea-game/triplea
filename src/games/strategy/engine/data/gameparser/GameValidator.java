package games.strategy.engine.data.gameparser;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.TechAdvance;

public class GameValidator {
  void validate(final GameData data) throws GameParseException {
    // validate unit attachments
    for (final UnitType u : data.getUnitTypeList()) {
      validateAttachments(data, u);
    }
    for (final Territory t : data.getMap()) {
      validateAttachments(data, t);
    }
    for (final Resource r : data.getResourceList().getResources()) {
      validateAttachments(data, r);
    }
    for (final PlayerID r : data.getPlayerList().getPlayers()) {
      validateAttachments(data, r);
    }
    for (final RelationshipType r : data.getRelationshipTypeList().getAllRelationshipTypes()) {
      validateAttachments(data, r);
    }
    for (final TerritoryEffect r : data.getTerritoryEffectList().values()) {
      validateAttachments(data, r);
    }
    for (final TechAdvance r : data.getTechnologyFrontier().getTechs()) {
      validateAttachments(data, r);
    }
    // if relationships are used, every player should have a relationship with every other player
    validateRelationships(data);
  }
  private void validateRelationships(final GameData data) throws GameParseException {
    // for every player
    for (final PlayerID player : data.getPlayerList()) {
      // in relation to every player
      for (final PlayerID player2 : data.getPlayerList()) {
        // See if there is a relationship between them
        if ((data.getRelationshipTracker().getRelationshipType(player, player2) == null)) {
          throw new GameParseException(data.getGameName(),
              "No relation set for: " + player.getName() + " and " + player2.getName());
          // or else throw an exception!
        }
      }
    }
  }

  private void validateAttachments(final GameData data, final Attachable attachable) throws GameParseException {
    for (final IAttachment a : attachable.getAttachments().values()) {
      a.validate(data);
    }
  }
}
