package util.triplea.mapXmlCreator;

import java.awt.GridBagConstraints;

import org.junit.Test;

import junit.framework.TestCase;

public class DynamicRowTest extends TestCase {

  @Test
  public void testGetGbcDefaultTemplateWith() {
    final int gridx = 2;
    final int gridy = 3;
    final GridBagConstraints gbcTemplate = MapXmlUIHelper.getGbcDefaultTemplateWith(gridx, gridy);
    assertEquals(gbcTemplate.gridx, gridx);
    assertEquals(gbcTemplate.gridy, gridy);
  }

}
