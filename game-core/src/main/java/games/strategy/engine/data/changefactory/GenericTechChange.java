package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.TechAttachment;

class GenericTechChange extends Change {
  private static final long serialVersionUID = -2439447526511535571L;

  private final Attachable attachedTo;
  private final String attachmentName;
  private final boolean newValue;
  private final boolean oldValue;
  private final String property;

  GenericTechChange(
      final TechAttachment attachment, final boolean newValue, final String property) {
    checkNotNull(attachment, "null attachment; newValue: " + newValue + ", property: " + property);

    attachedTo = attachment.getAttachedTo();
    attachmentName = attachment.getName();
    oldValue = attachment.hasGenericTech(property);
    this.newValue = newValue;
    this.property = property;
  }

  GenericTechChange(
      final Attachable attachTo,
      final String attachmentName,
      final boolean newValue,
      final boolean oldValue,
      final String property) {
    this.attachmentName = attachmentName;
    attachedTo = attachTo;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.property = property;
  }

  @Override
  public void perform(final GameData data) {
    final TechAttachment attachment = (TechAttachment) attachedTo.getAttachment(attachmentName);
    attachment.setGenericTech(property, newValue);
  }

  @Override
  public Change invert() {
    return new GenericTechChange(attachedTo, attachmentName, oldValue, newValue, property);
  }

  @Override
  public String toString() {
    return "GenericTechChange attached to:"
        + attachedTo
        + " name:"
        + attachmentName
        + " new value:"
        + newValue
        + " old value:"
        + oldValue;
  }
}
