package games.strategy.util;

import java.lang.ref.SoftReference;

import javax.swing.JEditorPane;

/**
 * For when your component contains images or data that is very very big, and you want it to be reclaimed as needed by the GC.
 * Example, when a JEditorPane has rich HTML in it, with huge images.
 * 
 * @author veqryn
 * 
 */
public class SoftJEditorPane
{
	protected SoftReference<JEditorPane> m_component;
	protected final String m_text;
	
	public SoftJEditorPane(final String text)
	{
		m_text = text;
	}
	
	protected JEditorPane createComponent()
	{
		final JEditorPane pane = new JEditorPane()
		/* {
			private static final long serialVersionUID = -7445877574463005826L;
			
			@Override
			protected void finalize()
			{
				System.out.println("JEditorPane finalized: " + this);
			}
		} */;
		pane.setEditable(false);
		pane.setContentType("text/html");
		pane.setText(m_text);
		pane.setCaretPosition(0);
		// System.out.println("JEditorPane created: " + pane);
		return pane;
	}
	
	public synchronized JEditorPane getComponent()
	{
		if (m_component == null)
		{
			m_component = new SoftReference<JEditorPane>(createComponent());
		}
		JEditorPane component = m_component.get();
		if (component == null)
		{
			component = createComponent();
			m_component = new SoftReference<JEditorPane>(component);
		}
		return component;
	}
	
	public String getText()
	{
		return m_text;
	}
	
	public void dispose()
	{
		if (m_component != null)
		{
			JEditorPane component = m_component.get();
			if (component != null)
			{
				component.setText("");
				component.removeAll();
				component = null;
			}
			m_component = null;
		}
	}
	
	/*
	public static void main(final String[] args)
	{
		final ReferenceQueue queue = new ReferenceQueue();
		final ArrayList blocks = new ArrayList();
		int size = 1310720;
		final int basesize = size;
		for (int id = 0; true; id++)
		{
			blocks.add(new SoftReference(new MemoryBlock(id, size), queue));
			
			while (true)
			{
				final java.lang.ref.Reference ref = queue.poll();
				if (ref == null)
					break;
				blocks.remove(ref);
				System.out.println("removing something: " + ref);
			}
			System.out.println("blocks: " + blocks);
			size += basesize;
		}
	}
	*/
}

/*
class MemoryBlock
{
	int id;
	int size;
	byte[] block;
	
	public MemoryBlock(final int id, final int size)
	{
		this.id = id;
		this.size = size;
		block = new byte[size];
		System.out.println("MemoryBlock created: " + this);
	}
	
	@Override
	public String toString()
	{
		return "{id=" + id + ",size=" + size + "}";
	}
	
	@Override
	protected void finalize()
	{
		System.out.println("MemoryBlock finalized: " + this);
	}
}
*/
