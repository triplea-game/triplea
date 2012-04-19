package games.strategy.engine.framework.startup.ui.editors;

import javax.swing.*;
import java.awt.*;

/**
 * Render to render IBeans in a combo box
 */
class DisplayNameComboBoxRender extends DefaultListCellRenderer
{
	private static final long serialVersionUID = -93452907043224502L;
	
	@Override
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		final IBean bean = (IBean) value;
		setText(bean.getDisplayName());
		return this;
	}
}
