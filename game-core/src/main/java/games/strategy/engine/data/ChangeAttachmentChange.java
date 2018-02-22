package games.strategy.engine.data;

public class ChangeAttachmentChange extends Change {
  private static final long serialVersionUID = -6447264150952218283L;
  private final Attachable attachedTo;
  private final String attachmentName;
  private final Object newValue;
  private final Object oldValue;
  private final String property;
  private boolean clearFirst = false;

  public Attachable getAttachedTo() {
    return attachedTo;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  /**
   * @param attachment An attachment object which we will update via reflexion
   * @param newValue The new value for the property
   * @param property The property by String name.
   */
  public ChangeAttachmentChange(final IAttachment attachment, final Object newValue, final String property) {
    if (attachment == null) {
      throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
    }
    attachedTo = attachment.getAttachedTo();
    clearFirst = false;
    attachmentName = attachment.getName();
    oldValue = attachment.getOrError(property).getValue();
    this.newValue = newValue;
    this.property = property;
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public ChangeAttachmentChange(final IAttachment attachment, final Object newValue, final String property,
      final boolean resetFirst) {
    if (attachment == null) {
      throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
    }
    attachedTo = attachment.getAttachedTo();
    clearFirst = resetFirst;
    attachmentName = attachment.getName();
    oldValue = attachment.getOrError(property).getValue();
    this.newValue = newValue;
    this.property = property;
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public ChangeAttachmentChange(final Attachable attachTo, final String attachmentName, final Object newValue,
      final Object oldValue, final String property, final boolean resetFirst) {
    this.attachmentName = attachmentName;
    attachedTo = attachTo;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.property = property;
    clearFirst = resetFirst;
  }

  @Override
  public void perform(final GameData data) {
    final IAttachment attachment = attachedTo.getAttachment(attachmentName);
    final AttachmentProperty<?> attachmentProperty = attachment.getOrError(property);
    if (clearFirst) {
      attachmentProperty.resetValue();
    }
    attachmentProperty.setObjectValue(newValue);
  }

  @Override
  public Change invert() {
    return new ChangeAttachmentChange(attachedTo, attachmentName, oldValue, newValue, property, clearFirst);
  }

  @Override
  public String toString() {
    return "ChangAttachmentChange attached to:" + attachedTo + " name:" + attachmentName + " new value:"
        + newValue + " old value:" + oldValue;
  }
}
