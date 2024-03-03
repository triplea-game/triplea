package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.triplea.java.collections.CollectionUtils;

/**
 * An attachment, attached to a player that will describe which political actions a player may take.
 * Note: Empty collection fields default to null to minimize memory use and serialization size.
 */
public class PoliticalActionAttachment extends AbstractUserActionAttachment {
  private static final long serialVersionUID = 4392770599777282477L;

  // list of relationship changes to be performed if this action is performed successfully
  private @Nullable List<String> relationshipChange = null;

  public PoliticalActionAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Collection<PoliticalActionAttachment> getPoliticalActionAttachments(
      final GamePlayer player) {
    return player.getAttachments().values().stream()
        .filter(a -> a.getName().startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX))
        .filter(PoliticalActionAttachment.class::isInstance)
        .map(PoliticalActionAttachment.class::cast)
        .collect(Collectors.toList());
  }

  public static PoliticalActionAttachment get(
      final GamePlayer player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, null);
  }

  static PoliticalActionAttachment get(
      final GamePlayer player,
      final String nameOfAttachment,
      final Collection<GamePlayer> playersToSearch) {
    PoliticalActionAttachment paa =
        (PoliticalActionAttachment) player.getAttachment(nameOfAttachment);
    if (paa == null) {
      if (playersToSearch == null) {
        throw new IllegalStateException(
            "PoliticalActionAttachment: No attachment for:"
                + player.getName()
                + " with name: "
                + nameOfAttachment);
      }

      for (final GamePlayer otherPlayer : playersToSearch) {
        if (otherPlayer.equals(player)) {
          continue;
        }
        paa = (PoliticalActionAttachment) otherPlayer.getAttachment(nameOfAttachment);
        if (paa != null) {
          return paa;
        }
      }
      throw new IllegalStateException(
          "PoliticalActionAttachment: No attachment for:"
              + player.getName()
              + " with name: "
              + nameOfAttachment);
    }
    return paa;
  }

  @VisibleForTesting
  void setRelationshipChange(final String relChange) throws GameParseException {
    final String[] s = splitOnColon(relChange);
    if (s.length != 3) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n Use: player1:player2:newRelation\n"
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n player: "
              + s[0]
              + " unknown in: "
              + getName()
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n player: "
              + s[1]
              + " unknown in: "
              + getName()
              + thisErrorMsg());
    }
    if (!Matches.isValidRelationshipName(getData().getRelationshipTypeList()).test(s[2])) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n relationshipType: "
              + s[2]
              + " unknown in: "
              + getName()
              + thisErrorMsg());
    }
    if (relationshipChange == null) {
      relationshipChange = new ArrayList<>();
    }
    relationshipChange.add(relChange.intern());
  }

  private void setRelationshipChange(final List<String> value) {
    relationshipChange = value;
  }

  private List<String> getRelationshipChange() {
    return getListProperty(relationshipChange);
  }

  private void resetRelationshipChange() {
    relationshipChange = null;
  }

  public List<RelationshipChange> getRelationshipChanges() {
    return getRelationshipChange().stream()
        .map(this::parseRelationshipChange)
        .collect(Collectors.toList());
  }

  private RelationshipChange parseRelationshipChange(final String encodedRelationshipChange) {
    final String[] tokens = splitOnColon(encodedRelationshipChange);
    assert tokens.length == 3;
    return new RelationshipChange(
        getData().getPlayerList().getPlayerId(tokens[0]),
        getData().getPlayerList().getPlayerId(tokens[1]),
        getData().getRelationshipTypeList().getRelationshipType(tokens[2]));
  }

  /** Returns a set of all other players involved in this PoliticalAction. */
  public Set<GamePlayer> getOtherPlayers() {
    final Set<GamePlayer> otherPlayers = new LinkedHashSet<>();
    for (final String relationshipChange : getRelationshipChange()) {
      final String[] s = splitOnColon(relationshipChange);
      otherPlayers.add(getData().getPlayerList().getPlayerId(s[0]));
      otherPlayers.add(getData().getPlayerList().getPlayerId(s[1]));
    }
    otherPlayers.remove(getAttachedTo());
    return otherPlayers;
  }

  /** Returns the valid actions for this player. */
  public static Collection<PoliticalActionAttachment> getValidActions(
      final GamePlayer player,
      final Map<ICondition, Boolean> testedConditions,
      final GameState data) {
    if (!Properties.getUsePolitics(data.getProperties()) || !player.amNotDeadYet()) {
      return new ArrayList<>();
    }
    return CollectionUtils.getMatches(
        getPoliticalActionAttachments(player),
        Matches.politicalActionAffectsAtLeastOneAlivePlayer(player)
            .and(Matches.abstractUserActionAttachmentCanBeAttempted(testedConditions)));
  }

  @Override
  public void validate(final GameState data) throws GameParseException {
    super.validate(data);
    if (relationshipChange == null) {
      throw new GameParseException("value: relationshipChange can't be empty" + thisErrorMsg());
    }
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "relationshipChange":
        return MutableProperty.of(
            this::setRelationshipChange,
            this::setRelationshipChange,
            this::getRelationshipChange,
            this::resetRelationshipChange);
      default:
        return super.getPropertyOrNull(propertyName);
    }
  }

  /**
   * A relationship change specified in a political action attachment. Specifies the relationship
   * type that will exist between two players after the action is successful.
   */
  @AllArgsConstructor(access = AccessLevel.PACKAGE)
  @EqualsAndHashCode
  @ToString
  public static final class RelationshipChange {
    public final GamePlayer player1;
    public final GamePlayer player2;
    public final RelationshipType relationshipType;
  }
}
