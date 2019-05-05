package games.strategy.triplea.attachments;


/**
 * Parameter object for fire trigger methods, first and foremost in {@link TriggerAttachment}.
 */
public class FireTriggerParams {

  /**
   * Filter value for before or after a given step.
   *
   * <p>
   * See {@link #testWhen}, documentation for trigger attachment option "when" as well as
   * {@link AbstractTriggerAttachment#whenOrDefaultMatch} .
   * </p>
   */
  public final String beforeOrAfter;

  /**
   * Filter value for a specific step/phase (like "russianPolitics" step).
   *
   * <p>
   * See {@link #testWhen}, documentation for trigger attachment option "when" as well as
   * {@link AbstractTriggerAttachment#whenOrDefaultMatch} .
   * </p>
   */
  public final String stepName;

  /**
   * TODO: Document.
   *
   * <p>
   * See documentation for trigger attachment option "uses".
   * </p>
   */
  public final boolean useUses;

  /**
   * TODO: Document.
   *
   * <p>
   * See documentation for trigger attachment option "uses".
   * </p>
   */
  public final boolean testUses;

  /**
   * TODO: Document.
   *
   * <p>
   * See documentation for trigger attachment option "chance".
   * </p>
   */
  public final boolean testChance;

  /**
   * Whether to filter for "when".
   *
   * <p>
   * See documentation for trigger attachment option "when" as well as
   * {@link AbstractTriggerAttachment#whenOrDefaultMatch} .
   * </p>
   */
  public final boolean testWhen;

  public FireTriggerParams(final String beforeOrAfter, final String stepName,
      final boolean useUses, final boolean testUses, final boolean testChance, final boolean testWhen) {
    this.beforeOrAfter = beforeOrAfter;
    this.stepName = stepName;
    this.useUses = useUses;
    this.testUses = testUses;
    this.testChance = testChance;
    this.testWhen = testWhen;
  }
}
