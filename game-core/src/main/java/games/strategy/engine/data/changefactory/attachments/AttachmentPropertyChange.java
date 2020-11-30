package games.strategy.engine.data.changefactory.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.changefactory.serializers.PrimitiveNamedAttachable;
import java.util.Optional;

/** A game data change that captures a change to an attachment property value. */
public class AttachmentPropertyChange extends Change {
  private static final long serialVersionUID = 5554561367125101920L;
  private final PrimitiveNamedAttachable attachedTo;
  private final String attachmentName;
  private final Object newValue;
  private final Object oldValue;
  private final String property;
  private final boolean clearFirst;

  /**
   * Initializes a new instance of the ChangeAttachmentChange class.
   *
   * @param attachment The attachment to be updated.
   * @param newValue The new value for the property.
   * @param property The property name.
   */
  public AttachmentPropertyChange(
      final IAttachment attachment, final Object newValue, final String property) {
    this(attachment, newValue, property, false);
  }

  /**
   * Initializes a new instance of the ChangeAttachmentChange class.
   *
   * @param attachment The attachment to be updated.
   * @param newValue The new value for the property.
   * @param property The property name.
   * @param clearFirst Clears the property before the attribute is updated with the newValue. This
   *     is only needed when the attribute being updated is a collection.
   */
  public AttachmentPropertyChange(
      final IAttachment attachment,
      final Object newValue,
      final String property,
      final boolean clearFirst) {
    this(
        attachment.getAttachedTo().getPrimitiveForm(),
        attachment.getName(),
        newValue,
        attachment.getPropertyOrThrow(property).getValue(),
        property,
        clearFirst);
  }

  private AttachmentPropertyChange(
      final PrimitiveNamedAttachable attachTo,
      final String attachmentName,
      final Object newValue,
      final Object oldValue,
      final String property,
      final boolean clearFirst) {
    this.attachmentName =
        Optional.ofNullable(attachmentName)
            // replace-all to automatically correct legacy (1.8) attachment spelling
            .map(name -> name.replaceAll("ttatch", "ttach"))
            .orElse(null);
    attachedTo = attachTo;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.property = property;
    this.clearFirst = clearFirst;
  }

  public Attachable getAttachedTo(final GameData data) {
    return ((Attachable) attachedTo.getReference(data));
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  @Override
  public void perform(final GameData data) {
    final IAttachment attachment =
        ((Attachable) attachedTo.getReference(data)).getAttachment(attachmentName);
    final MutableProperty<?> attachmentProperty = attachment.getPropertyOrThrow(property);
    if (clearFirst) {
      attachmentProperty.resetValue();
    }
    try {
      attachmentProperty.setValue(newValue);
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
    return new AttachmentPropertyChange(
        attachedTo, attachmentName, oldValue, newValue, property, clearFirst);
  }

  @Override
  public String toString() {
    return "ChangAttachmentChange attached to:"
        + attachedTo
        + " name:"
        + attachmentName
        + " new value:"
        + newValue
        + " old value:"
        + oldValue;
  }
}
