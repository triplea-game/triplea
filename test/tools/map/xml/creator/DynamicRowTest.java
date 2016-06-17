package tools.map.xml.creator;

import static org.junit.Assert.assertEquals;

import java.awt.GridBagConstraints;

import org.junit.Test;


public class DynamicRowTest {

  @Test
  public void testGetGbcDefaultTemplateWith() {
    final int gridx = 2;
    final int gridy = 3;
    final GridBagConstraints gbcTemplate = MapXmlUIHelper.getGbcDefaultTemplateWith(gridx, gridy);
    assertEquals(gbcTemplate.gridx, gridx);
    assertEquals(gbcTemplate.gridy, gridy);
  }
}
