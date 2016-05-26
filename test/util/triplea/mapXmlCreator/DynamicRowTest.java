package util.triplea.mapXmlCreator;

import org.junit.Test;

import java.awt.GridBagConstraints;

import static org.junit.Assert.assertEquals;


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
