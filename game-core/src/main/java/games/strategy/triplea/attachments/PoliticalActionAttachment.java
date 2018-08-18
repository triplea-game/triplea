package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;

/**
 * An attachment, attached to a player that will describe which political
 * actions a player may take.
 */
@MapSupport
public class PoliticalActionAttachment extends AbstractUserActionAttachment {
  private static final long serialVersionUID = 4392770599777282477L;

  // list of relationship changes to be performed if this action is performed sucessfully
  private List<String> m_relationshipChange = new ArrayList<>();

  public PoliticalActionAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Collection<PoliticalActionAttachment> getPoliticalActionAttachments(final PlayerID player) {
    return player.getAttachments().values().stream()
        .filter(a -> a.getName().startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX))
        .filter(PoliticalActionAttachment.class::isInstance)
        .map(PoliticalActionAttachment.class::cast)
        .collect(Collectors.toList());
  }

  public static PoliticalActionAttachment get(final PlayerID player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, null);
  }

  public static PoliticalActionAttachment get(final PlayerID player, final String nameOfAttachment,
      final Collection<PlayerID> playersToSearch) {
    PoliticalActionAttachment paa = (PoliticalActionAttachment) player.getAttachment(nameOfAttachment);
    if (paa == null) {
      if (playersToSearch == null) {
        throw new IllegalStateException(
            "PoliticalActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
      }

      for (final PlayerID otherPlayer : playersToSearch) {
        if (otherPlayer == player) {
          continue;
        }
        paa = (PoliticalActionAttachment) otherPlayer.getAttachment(nameOfAttachment);
        if (paa != null) {
          return paa;
        }
      }
      throw new IllegalStateException(
          "PoliticalActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
    }
    return paa;
  }

  private void setRelationshipChange(final String relChange) throws GameParseException {
    final String[] s = splitOnColon(relChange);
    if (s.length != 3) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange
          + " \n Use: player1:player2:newRelation\n" + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0]
          + " unknown in: " + getName() + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1]
          + " unknown in: " + getName() + thisErrorMsg());
    }
    if (!Matches.isValidRelationshipName(getData()).test(s[2])) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n relationshipType: "
          + s[2] + " unknown in: " + getName() + thisErrorMsg());
    }
    m_relationshipChange.add(relChange);
  }

  private void setRelationshipChange(final List<String> value) {
    m_relationshipChange = value;
  }

  public List<String> getRelationshipChange() {
    return m_relationshipChange;
  }

  private void resetRelationshipChange() {
    m_relationshipChange = new ArrayList<>();
  }

  /**
   * Returns a set of all other players involved in this PoliticalAction.
   */
  public Set<PlayerID> getOtherPlayers() {
    final Set<PlayerID> otherPlayers = new LinkedHashSet<>();
    for (final String relationshipChange : m_relationshipChange) {
      final String[] s = splitOnColon(relationshipChange);
      otherPlayers.add(getData().getPlayerList().getPlayerId(s[0]));
      otherPlayers.add(getData().getPlayerList().getPlayerId(s[1]));
    }
    otherPlayers.remove((getAttachedTo()));
    return otherPlayers;
  }

  /**
   * Returns the valid actions for this player.
   */
  public static Collection<PoliticalActionAttachment> getValidActions(final PlayerID player,
      final HashMap<ICondition, Boolean> testedConditions, final GameData data) {
    if (!Properties.getUsePolitics(data) || !player.amNotDeadYet(data)) {
      return new ArrayList<>();
    }
    return CollectionUtils.getMatches(getPoliticalActionAttachments(player),
        Matches.politicalActionAffectsAtLeastOneAlivePlayer(player, data)
            .and(Matches.abstractUserActionAttachmentCanBeAttempted(testedConditions)));
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    super.validate(data);
    if (m_relationshipChange.isEmpty()) {
      throw new GameParseException("value: relationshipChange can't be empty" + thisErrorMsg());
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("relationshipChange",
            MutableProperty.of(
                this::setRelationshipChange,
                this::setRelationshipChange,
                this::getRelationshipChange,
                this::resetRelationshipChange))
        .build();
  }
}
