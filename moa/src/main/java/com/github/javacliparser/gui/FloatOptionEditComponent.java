/*
 *    FloatOptionEditComponent.java
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
package com.github.javacliparser.gui;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.Option;

/**
 * An OptionEditComponent that lets the user edit a float option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FloatOptionEditComponent extends JPanel implements
        OptionEditComponent {

    private static final long serialVersionUID = 1L;

    public static final int SLIDER_RESOLUTION = 100000;

    protected FloatOption editedOption;

    protected JSpinner spinner;

    protected JSlider slider;

    public FloatOptionEditComponent(Option opt) {
        FloatOption option = (FloatOption) opt;
        this.editedOption = option;
        double minVal = option.getMinValue();
        double maxVal = option.getMaxValue();
        setLayout(new GridLayout(1, 0));
        this.spinner = new JSpinner(new SpinnerNumberModel(option.getValue(),
                minVal, maxVal, 0.001));
        add(this.spinner);
        if ((minVal > Double.NEGATIVE_INFINITY)
                && (maxVal < Double.POSITIVE_INFINITY)) {
            this.slider = new JSlider(0, SLIDER_RESOLUTION,
                    floatValueToSliderValue(option.getValue()));
            add(this.slider);
            this.slider.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    FloatOptionEditComponent.this.spinner.setValue(sliderValueToFloatValue(FloatOptionEditComponent.this.slider.getValue()));
                }
            });
            this.spinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    FloatOptionEditComponent.this.slider.setValue(floatValueToSliderValue(((Double) FloatOptionEditComponent.this.spinner.getValue()).doubleValue()));
                }
            });
        }
    }

    protected int floatValueToSliderValue(double floatValue) {
        double minVal = this.editedOption.getMinValue();
        double maxVal = this.editedOption.getMaxValue();
        return (int) Math.round((floatValue - minVal) / (maxVal - minVal)
                * SLIDER_RESOLUTION);
    }

    protected double sliderValueToFloatValue(int sliderValue) {
        double minVal = this.editedOption.getMinValue();
        double maxVal = this.editedOption.getMaxValue();
        return minVal
                + (((double) sliderValue / SLIDER_RESOLUTION) * (maxVal - minVal));
    }

    @Override
    public void applyState() {
        this.editedOption.setValue(((Double) this.spinner.getValue()).doubleValue());
        // this.editedOption.setValue(Double.parseDouble(this.spinner.getValue().toString()));
    }

    @Override
    public Option getEditedOption() {
        return this.editedOption;
    }

    @Override
    public void setEditState(String cliString) {
        this.spinner.setValue(FloatOption.cliStringToDouble(cliString));
    }
}
