package org.triplea.generic.xml.reader;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@RequiredArgsConstructor
public abstract class AbstractXmlMapperTest {
  protected XmlMapper xmlMapper;

  private final String file;
  private InputStream inputStream;

  @BeforeEach
  void setup() throws Exception {
    inputStream = XmlMapperTestUtils.openFile(file);
    xmlMapper = new XmlMapper(inputStream);
  }

  @AfterEach
  void tearDown() throws Exception {
    xmlMapper.close();
    inputStream.close();
  }
}
