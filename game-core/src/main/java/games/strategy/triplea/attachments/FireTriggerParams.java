package games.strategy.triplea.attachments;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

/** Parameter object for fire trigger methods, first and foremost in {@link TriggerAttachment}. */
@Value
@Getter(AccessLevel.NONE)
public class FireTriggerParams {

  /**
   * Filter value for before or after a given step.
   *
   * <p>See {@link #testWhen}, documentation for trigger attachment option "when" as well as {@link
   * AbstractTriggerAttachment#whenOrDefaultMatch} .
   */
  public final String beforeOrAfter;

  /**
   * Filter value for a specific step/phase (like "russianPolitics" step).
   *
   * <p>See {@link #testWhen}, documentation for trigger attachment option "when" as well as {@link
   * AbstractTriggerAttachment#whenOrDefaultMatch} .
   */
  public final String stepName;

  /** See documentation for trigger attachment option "uses". */
  public final boolean useUses;

  /** See documentation for trigger attachment option "uses". */
  public final boolean testUses;

  /** See documentation for trigger attachment option "chance". */
  public final boolean testChance;

  /**
   * Whether to filter for "when".
   *
   * <p>See documentation for trigger attachment option "when" as well as {@link
   * AbstractTriggerAttachment#whenOrDefaultMatch} .
   */
  public final boolean testWhen;
}
