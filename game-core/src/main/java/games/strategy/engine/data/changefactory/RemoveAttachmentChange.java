package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

class RemoveAttachmentChange extends Change {
  private static final long serialVersionUID = 6365648682759047674L;

  private final IAttachment attachment;
  private final String originalAttachmentName;
  private final Attachable originalAttachable;
  private final Attachable attachable;
  private final String name;

  RemoveAttachmentChange(
      final IAttachment attachment, final Attachable attachable, final String name) {
    this.attachment = attachment;
    originalAttachmentName = attachment.getName();
    originalAttachable = attachment.getAttachedTo();
    this.attachable = attachable;
    this.name = name;
  }

  @Override
  protected void perform(final GameData data) {
    originalAttachable.removeAttachment(originalAttachmentName);
    attachment.setAttachedTo(attachable);
    attachment.setName(name);
    if (attachable != null && name != null) {
      attachable.addAttachment(name, attachment);
    }
  }

  @Override
  public Change invert() {
    return new AddAttachmentChange(attachment, originalAttachable, originalAttachmentName);
  }
}
