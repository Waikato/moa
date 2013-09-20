/**
 * [ClassOptionWithNamesSelectionPanel.java]
 * 
 * ClassOptionWithNames: Selection panel
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Richard Kirkby
 * Data Management and Data Exploration Group, RWTH Aachen University
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

import moa.core.AutoClassDiscovery;
import moa.core.AutoExpandVector;
import moa.options.ClassOption;
import moa.options.OptionHandler;
import moa.tasks.Task;

public class ClassOptionWithNamesSelectionPanel extends JPanel {
	
    private static final long serialVersionUID = 1L;
    
    private boolean debug = false;

    protected JComboBox classChoiceBox;
    protected JComponent chosenObjectEditor;
    protected Object chosenObject;

    public ClassOptionWithNamesSelectionPanel(Class<?> requiredType,
            String initialCLIString, String nullString, String[] classNames) {
        Class<?>[] classesFound = findSuitableClasses(requiredType, classNames);
        if (debug) {
        	System.out.print("[ClassOptionWithNamesSelectionPanel] classNames = ");
        	for (String s : classNames) {
        		System.out.print(s + ", ");
        	}
        	System.out.println();
        	
        	System.out.print("[ClassOptionWithNamesSelectionPanel] classesFound = ");
        	for (Class<?> cl : classesFound) {
        		System.out.print(cl.getSimpleName() + ", ");
        	}
        	System.out.println();
        }
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
                Object chosen = ((Class<?>) ClassOptionWithNamesSelectionPanel.this.classChoiceBox.getSelectedItem()).newInstance();
                classChoiceChanged(chosen);
            } catch (Exception ex) {
                GUIUtils.showExceptionDialog(ClassOptionWithNamesSelectionPanel.this,
                        "Problem", ex);
            }
        }
        this.classChoiceBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    Object chosen = ((Class<?>) ClassOptionWithNamesSelectionPanel.this.classChoiceBox.getSelectedItem()).newInstance();
                    classChoiceChanged(chosen);
                } catch (Exception ex) {
                    GUIUtils.showExceptionDialog(
                            ClassOptionWithNamesSelectionPanel.this, "Problem", ex);
                }
            }
        });
    }

    public Class<?>[] findSuitableClasses(Class<?> requiredType, String[] classNames) {
        AutoExpandVector<Class<?>> finalClasses = new AutoExpandVector<Class<?>>();
        Class<?>[] classesFound = AutoClassDiscovery.findClassesOfType("moa",
                requiredType);
        for (Class<?> cl : classesFound) {
        	for (int i = 0; i < classNames.length; i++) {
        		if (cl.getSimpleName().contains(classNames[i])) {
        			finalClasses.add(cl);
        			break;
        		}
        	}
        }
        Class<?>[] tasksFound = AutoClassDiscovery.findClassesOfType("moa",
                Task.class);
        for (Class<?> foundTask : tasksFound) {
            try {
                Task task = (Task) foundTask.newInstance();
                if (requiredType.isAssignableFrom(task.getTaskResultType())) {
                	for (int i = 0; i < classNames.length; i++) {
                		if (foundTask.getSimpleName().contains(classNames[i])) {
                			finalClasses.add(foundTask);
                			break;
                		}
                	}
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return finalClasses.toArray(new Class<?>[finalClasses.size()]);
    }

    public static String showSelectClassDialog(Component parent, String title,
            Class<?> requiredType, String initialCLIString, String nullString, String[] classNames) {
        ClassOptionWithNamesSelectionPanel panel = new ClassOptionWithNamesSelectionPanel(
                requiredType, initialCLIString, nullString, classNames);
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
