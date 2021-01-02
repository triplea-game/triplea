package games.strategy.triplea.ui.menubar.debug;

import java.util.List;
import java.util.function.Consumer;
import lombok.Value;

@Value
public class AiPlayerDebugOption {
  public enum ActionType {
    ON_OFF,
    ON_OFF_EXCLUSIVE,
    NORMAL,
  }

  String title;
  ActionType actionType;
  String exclusiveGroup;
  List<AiPlayerDebugOption> subActions;
  Consumer<AiPlayerDebugAction> actionListener;
}
