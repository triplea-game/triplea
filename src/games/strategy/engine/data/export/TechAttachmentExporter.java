package games.strategy.engine.data.export;

import java.lang.reflect.Field;

import games.strategy.engine.data.IAttachment;

public class TechAttachmentExporter extends DefaultAttachmentExporter {
  @Override
  protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException {
    final String fieldName = field.getName();
    if (fieldName.equals("m_GenericTech")) {
      // GenericTech not set by XML
      return "";
    }
    // return mGenericTechHandler(field,attachment);
    return super.printOption(field, attachment);
  }

  @Override
  protected String printBooleanOption(final Field field, final String option, final IAttachment attachment)
      throws AttachmentExportException {
    return printBooleanOption(field, option, attachment, true);
  }
}
