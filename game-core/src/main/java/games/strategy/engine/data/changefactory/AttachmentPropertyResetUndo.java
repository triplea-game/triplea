package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;

class AttachmentPropertyResetUndo extends Change {
  private static final long serialVersionUID = 5943939650116851332L;

  private final Attachable attachedTo;
  private final String attachmentName;
  private final Object newValue;
  private final String property;

  AttachmentPropertyResetUndo(
      final Attachable attachTo,
      final String attachmentName,
      final Object newValue,
      final String property) {
    this.attachmentName = attachmentName;
    attachedTo = attachTo;
    this.newValue = newValue;
    this.property = property;
  }

  @Override
  public void perform(final GameDataInjections data) {
    final IAttachment attachment = attachedTo.getAttachment(attachmentName);
    try {
      attachment.getPropertyOrThrow(property).setValue(newValue);
    } catch (final MutableProperty.InvalidValueException e) {
      throw new IllegalStateException(
          String.format(
              "failed to set value '%s' on property '%s' for attachment '%s' associated with '%s'",
              newValue, property, attachmentName, attachedTo),
          e);
    }
  }

  @Override
  public Change invert() {
    return new AttachmentPropertyReset(attachedTo, attachmentName, newValue, property);
  }

  @Override
  public String toString() {
    return "AttachmentPropertyClearUndo attached to:"
        + attachedTo
        + " name:"
        + attachmentName
        + " new value:"
        + newValue;
  }
}
