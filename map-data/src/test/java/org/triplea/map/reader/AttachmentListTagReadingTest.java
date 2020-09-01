package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.GameTag;
import org.triplea.map.data.elements.AttachmentListTag.Attachment;
import org.triplea.map.data.elements.AttachmentListTag.Attachment.Option;

public class AttachmentListTagReadingTest {

  @Test
  void readAttachmentListTag() {
    final GameTag parsedMap = parseMapXml("resources/attachment-list-tag.xml");

    final List<Attachment> attachments = parsedMap.getAttachmentListTag().getAttachments();
    assertThat(attachments, hasSize(2));

    assertThat(attachments.get(0).getForeach(), is("foreach-value"));
    assertThat(attachments.get(0).getName(), is("name-value"));
    assertThat(attachments.get(0).getAttachTo(), is("attachTo-value"));
    assertThat(attachments.get(0).getJavaClass(), is("javaClass-value"));
    assertThat(attachments.get(0).getType(), is("unitType"));

    List<Option> options = attachments.get(0).getOptions();
    assertThat(options, hasSize(2));
    assertThat(options.get(0).getName(), is("default-option"));
    assertThat(options.get(0).getValue(), is("some-value"));
    assertThat(options.get(0).getCount(), is(""));

    assertThat(options.get(1).getName(), is("default-option2"));
    assertThat(options.get(1).getValue(), is("some-value2"));
    assertThat(options.get(1).getCount(), is("2"));

    assertThat(attachments.get(1).getForeach(), is("foreach-value2"));
    assertThat(attachments.get(1).getName(), is("name-value2"));
    assertThat(attachments.get(1).getAttachTo(), is("attachTo-value2"));
    assertThat(attachments.get(1).getJavaClass(), is("javaClass-value2"));
    assertThat(attachments.get(1).getType(), is("resource"));

    options = attachments.get(1).getOptions();
    assertThat(options, hasSize(1));
    assertThat(options.get(0).getName(), is("option-name"));
    assertThat(options.get(0).getValue(), is("option-value"));
    assertThat(options.get(0).getCount(), is("option-count"));
  }
}
