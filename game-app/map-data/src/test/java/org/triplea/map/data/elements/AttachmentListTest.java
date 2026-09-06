package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.AttachmentList.Attachment;
import org.triplea.map.data.elements.AttachmentList.Attachment.Option;

public class AttachmentListTest {

  @Test
  void readAttachmentListTag() {
    final AttachmentList attachmentList = parseMapXml("attachment-list.xml").getAttachmentList();

    final List<Attachment> attachments = attachmentList.getAttachments();
    assertThat(attachments).hasSize(2);

    assertThat(attachments.get(0).getForeach()).isEqualTo("foreach-value");
    assertThat(attachments.get(0).getName()).isEqualTo("name-value");
    assertThat(attachments.get(0).getAttachTo()).isEqualTo("attachTo-value");
    assertThat(attachments.get(0).getJavaClass()).isEqualTo("javaClass-value");
    assertThat(attachments.get(0).getType()).isNull();

    List<Option> options = attachments.get(0).getOptions();
    assertThat(options).hasSize(2);
    assertThat(options.get(0).getName()).isEqualTo("default-option");
    assertThat(options.get(0).getValue()).isEqualTo("some-value");
    assertThat(options.get(0).getCount()).isNull();

    assertThat(options.get(1).getName()).isEqualTo("default-option2");
    assertThat(options.get(1).getValue()).isEqualTo("some-value2");
    assertThat(options.get(1).getCount()).isEqualTo("2");

    assertThat(attachments.get(1).getForeach()).isEqualTo("foreach-value2");
    assertThat(attachments.get(1).getName()).isEqualTo("name-value2");
    assertThat(attachments.get(1).getAttachTo()).isEqualTo("attachTo-value2");
    assertThat(attachments.get(1).getJavaClass()).isEqualTo("javaClass-value2");
    assertThat(attachments.get(1).getType()).isEqualTo("resource");

    options = attachments.get(1).getOptions();
    assertThat(options).hasSize(1);
    assertThat(options.get(0).getName()).isEqualTo("option-name");
    assertThat(options.get(0).getValue()).isEqualTo("option-value");
    assertThat(options.get(0).getCount()).isEqualTo("option-count");
  }
}
