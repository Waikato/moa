/*
 *    RecurrentConceptDriftStream.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Miguel Abad (miguel.abad.arranz at alumnos dot upm dot es)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
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
package moa.streams;

import moa.core.Example;
import java.util.Random;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Instance;
import com.github.javacliparser.IntOption;



/**
 * Stream generator that adds recurrent concept drifts to examples in a stream.
 *<br/><br/>
 * Example:
 *<br/><br/>
 * <code>RecurrentConceptDriftStream -s (generators.AgrawalGenerator -f 7) <br/>
 *    -d (generators.AgrawalGenerator -f 2) -w 1000000 -p 900000</code>
 *<br/><br/>
 * s : Stream <br/>
 * d : Concept drift Stream<br/>
 * p : Central position of first concept drift change<br/>
 * w : Width of concept drift changes<br/>
 * x : Width of recurrence (number of instances during which new concept is used)
 * y : Number of stability period (number of instances between drifts)
 * z : Number of appearances of drift
 *
 * @author Miguel Abad (miguel.abad.arranz at alumnos dot upm dot es)x
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 1 $
 */
public class RecurrentConceptDriftStream extends ConceptDriftStream {
@Override
    public String getPurposeString() {
        return "Adds Recurrent Concept Drift to examples in a stream.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption widthRecurrenceOption = new IntOption("widthRecurrence", 'x', 
            "Number of instances during which new concept is used", 100);
    
    public IntOption stabPeriodOption = new IntOption("stabPeriod", 'y', 
            "Number of instances between drifts", 200);     
    
    public IntOption numRepOption = new IntOption("numRep", 'z', 
            "Number of instances between drifts", 4); 

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {

        this.inputStream = (InstanceStream) getPreparedClassOption(this.streamOption);
        this.driftStream = (InstanceStream) getPreparedClassOption(this.driftstreamOption);
        this.random = new Random(this.randomSeedOption.getValue());
        numberInstanceStream = 0;
        if (this.alphaOption.getValue() != 0.0) {
            this.widthOption.setValue((int) (1 / Math.tan(this.alphaOption.getValue() * Math.PI / 180)));
        }
    }

    @Override
    public Example nextInstance() {

        numberInstanceStream++;
        double x = numberInstanceStream;
        
        double probabilityDrift = 0;
           
        int iPos = this.positionOption.getValue();
        
        //As long as the probabilistic function is recursive, it depends
        //on the number of repetitions established in the options
        for (int iNumRep = 0; iNumRep < this.numRepOption.getValue(); iNumRep++){
            probabilityDrift += 1.0 / (1.0 + Math.exp(-4*(x-iPos)/this.widthOption.getValue())) - 
                1.0 / (1.0 + Math.exp(-4*(x-(iPos + this.widthRecurrenceOption.getValue()))/this.widthOption.getValue()));
            
            iPos += this.widthRecurrenceOption.getValue() + this.stabPeriodOption.getValue();
        }
        
        if (this.random.nextDouble() > probabilityDrift) {            
            return this.inputStream.nextInstance();
        } else {            
            return this.driftStream.nextInstance();
        }       
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
    
}
