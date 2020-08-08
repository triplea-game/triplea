package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

class AddAttachmentChange extends Change {
  private static final long serialVersionUID = -21015135248288454L;

  private final IAttachment attachment;
  private final String originalAttachmentName;
  private final Attachable originalAttachable;
  private final Attachable attachable;
  private final String name;

  AddAttachmentChange(
      final IAttachment attachment, final Attachable attachable, final String name) {
    this.attachment = attachment;
    originalAttachmentName = attachment.getName();
    originalAttachable = attachment.getAttachedTo();
    this.attachable = attachable;
    this.name = name;
  }

  @Override
  protected void perform(final GameData data) {
    attachable.addAttachment(name, attachment);
    attachment.setName(name);
    attachment.setAttachedTo(attachable);
  }

  @Override
  public Change invert() {
    return new RemoveAttachmentChange(attachment, originalAttachable, originalAttachmentName);
  }
}
