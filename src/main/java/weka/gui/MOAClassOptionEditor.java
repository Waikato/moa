/*
 * MOAClassOptionEditor.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import moa.gui.ClassOptionEditComponent;
import moa.options.ClassOption;

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
		return true;
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
		JPanel			panelButtons;
		JButton			buttonOK;
		JButton			buttonCancel;
		JPanel			panelSpacer;
		
		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		m_EditComponent = (ClassOptionEditComponent) ((ClassOption) getValue()).getEditComponent();
		panel.add(m_EditComponent, BorderLayout.CENTER);
		
		panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(panelButtons, BorderLayout.SOUTH);

		panelSpacer = new JPanel();
		panelSpacer.setPreferredSize(new Dimension(200, 10));
		panelButtons.add(panelSpacer);
		
		buttonOK = new JButton("OK");
		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_EditComponent.applyState();
				setValue(m_EditComponent.getEditedOption());
				closeDialog();
			}
		});
		panelButtons.add(buttonOK);

		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		});
		panelButtons.add(buttonCancel);
		
		return panel;
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
