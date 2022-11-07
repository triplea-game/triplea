package games.strategy.triplea.attachments;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.delegate.IDelegateBridge;
import java.util.List;
import java.util.Map;

/**
 * The purpose of this class is to have all ATTACHMENT classes that use "conditions" (conditions are
 * RulesAttachment's) implement this class, so that conditions can be tested for independently and
 * recursively.
 */
public interface ICondition extends IAttachment {
  /**
   * Returns attached RulesAttachments. Yes, this should be RulesAttachment, not ICondition. The
   * reason being that you can ONLY attach RulesAttachments to a class that implements ICondition.
   */
  List<RulesAttachment> getConditions();

  /**
   * Tests if the attachment, as a whole, is satisfied. This includes and takes account of all
   * conditions that make up this ICondition, as well as invert, conditionType, and in the case of
   * RulesAttachments it also takes into account all the different rules that must be tested for,
   * like alliedOwnershipTerritories, unitPresence, etc etc. IDelegateBridge is only needed for
   * actually testing the conditions. Once they have been tested, (once you have
   * HashMap&lt;ICondition, Boolean> testedConditions filled out), then IDelegateBridge is not
   * required and can be null (or use the shortcut method). Therefore use this method while testing
   * the conditions the first time.
   */
  boolean isSatisfied(Map<ICondition, Boolean> testedConditions, IDelegateBridge bridge);

  /**
   * HashMap&lt;ICondition, Boolean> testedConditions must be filled with completed tests of all
   * conditions already, or this will give you errors.
   */
  boolean isSatisfied(Map<ICondition, Boolean> testedConditions);
}
