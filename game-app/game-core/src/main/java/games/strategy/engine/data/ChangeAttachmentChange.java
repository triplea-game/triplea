package games.strategy.engine.data;

import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;

/** A game data change that captures a change to an attachment property value. */
public class ChangeAttachmentChange extends Change {
  private static final long serialVersionUID = -6447264150952218283L;
  @Getter private final Attachable attachedTo;
  @Getter private final String attachmentName;
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
  public ChangeAttachmentChange(
      final IAttachment attachment, final Object newValue, final String property) {
    this(
        attachment.getAttachedTo(),
        attachment.getName(),
        newValue,
        DefaultAttachment.copyPropertyValue(attachment.getPropertyOrThrow(property).getValue()),
        property,
        false);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the
   * setting method is actually adding things to a list rather than overwriting.
   */
  public ChangeAttachmentChange(
      final IAttachment attachment,
      final Object newValue,
      final String property,
      final boolean clearFirst) {
    this(
        attachment.getAttachedTo(),
        attachment.getName(),
        newValue,
        DefaultAttachment.copyPropertyValue(attachment.getPropertyOrThrow(property).getValue()),
        property,
        clearFirst);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the
   * setting method is actually adding things to a list rather than overwriting.
   */
  public ChangeAttachmentChange(
      final Attachable attachTo,
      final @Nullable String attachmentName,
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

  @Override
  public void perform(final GameState data) {
    final IAttachment attachment = attachedTo.getAttachment(attachmentName);
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
    if (attachment instanceof TechAttachment techAttachment) {
      techAttachment.getData().getTechTracker().clearCache();
    } else if (attachment instanceof TechAbilityAttachment techAbilityAttachment) {
      techAbilityAttachment.getData().getTechTracker().clearCache();
    }
  }

  @Override
  public Change invert() {
    return new ChangeAttachmentChange(
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
