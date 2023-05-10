/*
 *    ProcessGraphAxes.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de) 
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.gui.visualization;

import java.awt.Graphics;

/**
 * ProcessGraphAxes is an implementation of AbstractGraphAxes, drawing x labels
 * based on the process frequency.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphAxes
 */
public class ProcessGraphAxes extends AbstractGraphAxes {

	private static final long serialVersionUID = 1L;

    private int processFrequency;

    /**
     * Sets the process frequency
     * @param frequency new process frequency
     */
    public void setProcessFrequency(int frequency){
        processFrequency = frequency;
    }

	@Override
	protected void drawXLabels(Graphics g) {
        //x axis labels
        int w = 100;

        for (int i = 0; w*i < width-X_OFFSET_RIGHT; i++) {
            g.drawLine(w*i+X_OFFSET_LEFT, height+Y_OFFSET_TOP, 
            		   w*i+X_OFFSET_LEFT, height+Y_OFFSET_TOP+5);

            String label = Integer.toString((int)(w*i*processFrequency/x_resolution));

            int str_length = g.getFontMetrics().stringWidth(label);
            g.drawString(label,w*i+X_OFFSET_LEFT-str_length/2,height+Y_OFFSET_TOP+18);
        }	
	}

}
