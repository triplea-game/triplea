package org.triplea.generic.xml.reader;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.triplea.generic.xml.TestUtils;

@RequiredArgsConstructor
public abstract class AbstractXmlMapperTest {
  protected XmlMapper xmlMapper;

  private final String file;
  private InputStream inputStream;

  @BeforeEach
  void setup() throws Exception {
    inputStream = TestUtils.openFile(file);
    xmlMapper = new XmlMapper(inputStream);
  }

  @AfterEach
  void tearDown() throws Exception {
    xmlMapper.close();
    inputStream.close();
  }
}
