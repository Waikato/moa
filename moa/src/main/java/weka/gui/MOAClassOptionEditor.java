/*
 * MOAClassOptionEditor.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyEditorSupport;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.gui.ClassOptionEditComponent;
import com.github.javacliparser.gui.OptionsConfigurationPanel;
import moa.options.ClassOption;
import com.github.javacliparser.Option;

/**
 * An editor for MOA ClassOption objects.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see ClassOption
 */
public class MOAClassOptionEditor
  extends PropertyEditorSupport {

	/** the custom editor. */
	protected Component m_CustomEditor;

	/** the component for editing. */
	protected ClassOptionEditComponent m_EditComponent;

	/**
	 * Returns true since this editor is paintable.
	 *
	 * @return 		always true.
	 */
	public boolean isPaintable() {
		return false;
	}

	/**
	 * Returns true because we do support a custom editor.
	 *
	 * @return 		always true
	 */
	public boolean supportsCustomEditor() {
		return true;
	}

	/**
	 * Closes the dialog.
	 */
	protected void closeDialog() {
		if (m_CustomEditor instanceof Container) {
			Dialog dlg = PropertyDialog.getParentDialog((Container) m_CustomEditor);
			if (dlg != null)
				dlg.setVisible(false);
		}
	}

	/**
	 * Creates the custom editor.
	 *
	 * @return		the editor
	 */
	protected Component createCustomEditor() {
		JPanel			panel;

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		m_EditComponent = (ClassOptionEditComponent) getEditComponent((ClassOption) getValue());
		m_EditComponent.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				m_EditComponent.applyState();
				setValue(m_EditComponent.getEditedOption());
			}
		});
		panel.add(m_EditComponent, BorderLayout.CENTER);

		return panel;
	}

        public JComponent getEditComponent(Option option){
             return OptionsConfigurationPanel.getEditComponent(option);
        }

	/**
	 * Gets the custom editor component.
	 *
	 * @return 		the editor
	 */
	public Component getCustomEditor() {
		if (m_CustomEditor == null)
			m_CustomEditor = createCustomEditor();

		return m_CustomEditor;
	}

  /**
   * Paints a representation of the current Object.
   *
   * @param gfx 	the graphics context to use
   * @param box 	the area we are allowed to paint into
   */
  public void paintValue(Graphics gfx, Rectangle box) {
    FontMetrics 	fm;
    int 					vpad;
    String 				val;

    fm   = gfx.getFontMetrics();
    vpad = (box.height - fm.getHeight()) / 2 ;
    val  = ((ClassOption) getValue()).getValueAsCLIString();
    gfx.drawString(val, 2, fm.getHeight() + vpad);
  }
}
