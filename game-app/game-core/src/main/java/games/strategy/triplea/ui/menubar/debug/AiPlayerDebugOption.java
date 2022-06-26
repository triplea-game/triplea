package games.strategy.triplea.ui.menubar.debug;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class AiPlayerDebugOption {

  public enum OptionType {
    /** If the option can be selected and deselected, then use ON_OFF */
    ON_OFF,
    /**
     * If the option is grouped with other options and only one can be selected at a time, then use
     * ON_OFF_EXCLUSIVE
     */
    ON_OFF_EXCLUSIVE,
    /** If the option just runs an action when selected, then use NORMAL */
    NORMAL,
  }

  /** The menu title */
  @Nonnull String title;
  /**
   * The type of option
   *
   * <p>Not used if subOptions is non-empty
   */
  @Builder.Default OptionType optionType = OptionType.NORMAL;
  /**
   * Used to group the related ON_OFF_EXCLUSIVE options
   *
   * <p>Only used if optionType is ON_OFF_EXCLUSIVE
   */
  @Builder.Default String exclusiveGroup = "";
  /** These options become available when the parent option is selected */
  @Builder.Default List<AiPlayerDebugOption> subOptions = List.of();

  /**
   * The action to occur when the user clicks on the option.
   *
   * <p>Not used if subOptions is non-empty
   *
   * <p>Can't use a lambda "(action) -> {}" because Error Prone fails while parsing
   */
  @Builder.Default
  Consumer<AiPlayerDebugAction> actionListener = AiPlayerDebugOption::actionListener;

  @Builder.Default int mnemonic = KeyEvent.VK_UNDEFINED;

  /** This exists solely to be a default actionListener. It should not do anything. */
  private static void actionListener(final AiPlayerDebugAction action) {
    /* default: nothing to do */
  }
}
