/*
 *    ClassOptionSelectionPanel.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.gui;

import com.github.javacliparser.gui.OptionsConfigurationPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import moa.capabilities.CapabilityRequirement;
import moa.core.AutoClassDiscovery;
import moa.core.AutoExpandVector;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.tasks.Task;

/**
 * Creates a panel that displays the classes available, letting the user select
 * a class.
 *
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class ClassOptionSelectionPanel extends JPanel {

    // TODO: idea - why not retain matching options between classes when the
    // class type is changed
    // e.g. if user switches from LearnModel to EvaluateLearner, retain whatever
    // the 'stream' option was set to
    private static final long serialVersionUID = 1L;

    /** The requirements that classes must meet in order to be listed. */
    private static CapabilityRequirement requiredCapabilities = null;

    /**
     * Sets the capability requirement of listed classes.
     *
     * @param requirements	The capability requirements.
     */
    public static void setRequiredCapabilities(CapabilityRequirement requirements) {
        requiredCapabilities = requirements;
    }

    protected JComboBox classChoiceBox;

    protected JComponent chosenObjectEditor;

    protected Object chosenObject;

    public ClassOptionSelectionPanel(Class<?> requiredType,
            String initialCLIString, String nullString) {
        // Class<?>[] classesFound = AutoClassDiscovery.findClassesOfType("moa",
        // requiredType);
        Class<?>[] classesFound = findSuitableClasses(requiredType);
        this.classChoiceBox = new JComboBox(classesFound);
        setLayout(new BorderLayout());
        add(this.classChoiceBox, BorderLayout.NORTH);
        Object initialObject = null;
        try {
            initialObject = ClassOption.cliStringToObject(initialCLIString,
                    requiredType, null);
        } catch (Exception ignored) {
            // ignore exception
        }
        if (initialObject != null) {
            this.classChoiceBox.setSelectedItem(initialObject.getClass());
            classChoiceChanged(initialObject);
        } else {
            try {
                Object chosen = ((Class<?>) ClassOptionSelectionPanel.this.classChoiceBox.getSelectedItem()).newInstance();
                classChoiceChanged(chosen);
            } catch (Exception ex) {
                GUIUtils.showExceptionDialog(ClassOptionSelectionPanel.this,
                        "Problem", ex);
            }
        }
        this.classChoiceBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Object chosen = ((Class<?>) ClassOptionSelectionPanel.this.classChoiceBox.getSelectedItem()).newInstance();
                    classChoiceChanged(chosen);
                } catch (Exception ex) {
                    GUIUtils.showExceptionDialog(
                            ClassOptionSelectionPanel.this, "Problem", ex);
                }
            }
        });
    }

    public Class<?>[] findSuitableClasses(Class<?> requiredType) {
        AutoExpandVector<Class<?>> finalClasses = new AutoExpandVector<Class<?>>();
        Class<?>[] classesFound = AutoClassDiscovery.findClassesOfType("moa",
                requiredType);
        for (Class<?> foundClass : classesFound) {
            // Skip this class if it doesn't meet the capabilities requirement
            if (requiredCapabilities != null && !requiredCapabilities.isMetBy(foundClass))
                continue;

            finalClasses.add(foundClass);
        }
        Class<?>[] tasksFound = AutoClassDiscovery.findClassesOfType("moa",
                Task.class);
        for (Class<?> foundTask : tasksFound) {
            try {
                Task task = (Task) foundTask.newInstance();
                if (requiredType.isAssignableFrom(task.getTaskResultType())) {
		    // Skip this task if its result type doesn't meet the capabilities requirement
                    if (requiredCapabilities != null && !requiredCapabilities.isMetBy(task.getTaskResultType()))
                        continue;

                    finalClasses.add(foundTask);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return finalClasses.toArray(new Class<?>[finalClasses.size()]);
    }

    public static String showSelectClassDialog(Component parent, String title,
            Class<?> requiredType, String initialCLIString, String nullString) {
        ClassOptionSelectionPanel panel = new ClassOptionSelectionPanel(
                requiredType, initialCLIString, nullString);
        if (JOptionPane.showOptionDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                null, null) == JOptionPane.OK_OPTION) {
            return panel.getChosenObjectCLIString(requiredType);
        }
        return initialCLIString;
    }

    public String getChosenObjectCLIString(Class<?> requiredType) {
        if (this.chosenObjectEditor instanceof OptionsConfigurationPanel) {
            ((OptionsConfigurationPanel) this.chosenObjectEditor).applyChanges();
        }
        return ClassOption.objectToCLIString(this.chosenObject, requiredType);
    }

    public void classChoiceChanged(Object chosen) {
        this.chosenObject = chosen;
        JComponent newChosenObjectEditor = null;
        if (this.chosenObject instanceof OptionHandler) {
            OptionHandler chosenOptionHandler = (OptionHandler) this.chosenObject;
            newChosenObjectEditor = new OptionsConfigurationPanel(
                    chosenOptionHandler.getPurposeString(), chosenOptionHandler.getOptions());
        }
        if (this.chosenObjectEditor != null) {
            remove(this.chosenObjectEditor);
        }
        this.chosenObjectEditor = newChosenObjectEditor;
        if (this.chosenObjectEditor != null) {
            add(this.chosenObjectEditor, BorderLayout.CENTER);
        }
        Component component = this;
        while ((component != null) && !(component instanceof JDialog)) {
            component = component.getParent();
        }
        if (component != null) {
            Window window = (Window) component;
            window.pack();
        }
    }
}
