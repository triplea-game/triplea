package games.strategy.engine.data.export;

import java.lang.reflect.Field;

import games.strategy.engine.data.IAttachment;

public class PlayerAttachmentExporter extends DefaultAttachmentExporter {
  @Override
  protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException {
    final String fieldName = field.getName();
    if (fieldName.equals("m_captureUnitOnEnteringBy")) {
      return mCaptureUnitOnEnteringByHandler(field, attachment);
    }
    if (fieldName.equals("m_giveUnitControl")) {
      return mGiveUnitControlHandler(field, attachment);
    }
    return super.printOption(field, attachment);
  }

  private String mGiveUnitControlHandler(final Field field, final IAttachment attachment) throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }

  private String mCaptureUnitOnEnteringByHandler(final Field field, final IAttachment attachment) throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }
}
