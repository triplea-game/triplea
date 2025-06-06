package games.strategy.engine.history;

import games.strategy.engine.data.GamePlayer;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

/** A history node that represents one step of game play (e.g. "Britain Combat Move"). */
public class Step extends IndexedHistoryNode {
  @Serial private static final long serialVersionUID = 1015799886178275645L;
  private final @Nullable GamePlayer player;
  @Getter @NonNls private final String stepName;
  @NonNls private final String delegateName;

  Step(
      final @NonNls String stepName,
      final @NonNls String delegateName,
      final @Nullable GamePlayer player,
      final int changeStartIndex,
      final String displayName) {
    super(displayName, changeStartIndex);
    this.stepName = stepName;
    this.delegateName = delegateName;
    this.player = player;
  }

  public Optional<GamePlayer> getPlayerId() {
    return Optional.ofNullable(player);
  }

  public GamePlayer getPlayerIdOrThrow() {
    return Optional.ofNullable(player)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    MessageFormat.format("No expected player for Step {0}", this.stepName)));
  }

  @Override
  public SerializationWriter getWriter() {
    return new StepHistorySerializer(stepName, delegateName, player, super.getTitle());
  }
}
