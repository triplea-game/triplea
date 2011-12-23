package games.strategy.engine.framework.startup.ui.editors;

import javax.swing.*;
import java.awt.*;

/**
 * Combobox render used for beans which have a displayname
 */
class DisplayNameComboBoxRender extends DefaultListCellRenderer
{
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		   IBean bean = (IBean) value;
		setText(bean.getDisplayName());
		return this;
	}
}
