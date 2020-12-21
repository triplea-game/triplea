package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.IAttachment;

/** Resets the value to the default value. */
class AttachmentPropertyReset extends Change {
  private static final long serialVersionUID = 9208154387325299072L;

  private final Attachable attachedTo;
  private final String attachmentName;
  private final Object oldValue;
  private final String property;

  AttachmentPropertyReset(final IAttachment attachment, final String property) {
    checkNotNull(attachment, "null attachment; property: " + property);

    attachedTo = attachment.getAttachedTo();
    attachmentName = attachment.getName();
    oldValue = attachment.getPropertyOrThrow(property).getValue();
    this.property = property;
  }

  AttachmentPropertyReset(
      final Attachable attachTo,
      final String attachmentName,
      final Object oldValue,
      final String property) {
    this.attachmentName = attachmentName;
    attachedTo = attachTo;
    this.oldValue = oldValue;
    this.property = property;
  }

  @Override
  public void perform(final GameDataInjections data) {
    final IAttachment attachment = attachedTo.getAttachment(attachmentName);
    attachment.getPropertyOrThrow(property).resetValue();
  }

  @Override
  public Change invert() {
    return new AttachmentPropertyResetUndo(attachedTo, attachmentName, oldValue, property);
  }

  @Override
  public String toString() {
    return "AttachmentPropertyClear attached to:"
        + attachedTo
        + " name:"
        + attachmentName
        + ", reset old value:"
        + oldValue;
  }
}
