/*
 *    SingleClassifierDrift.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import sizeof.agent.SizeOfAgent;
import moa.AbstractMOAObject;
import moa.classifiers.Classifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.MiscUtils;
import moa.options.ClassOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import weka.core.Instance;
import weka.core.Utils;

/**
 * Class for handling concept drift datasets with a wrapper on a
 * classifier.<p>data
 *
 * Valid options are:<p>
 *
 * -l classname <br>
 * Specify the full class name of a classifier as the basis for 
 * the concept drift classifier.<p>
 *
 *
 * @author Manuel Baena (mbaena@lcc.uma.es)
 * @version 1.1 
 */
public class SingleClassifierDrift extends AbstractClassifier {
	
	public class DriftDetectionMethod extends AbstractMOAObject {
	    
	    private static final long serialVersionUID = 1L;

		public static final int DDM_INCONTROL_LEVEL = 0;
		public static final int DDM_WARNING_LEVEL = 1;
		public static final int DDM_OUTCONTROL_LEVEL = 2;

		public int computeNextVal(boolean prediction) {
			return 0;
			};
		
		//@Override
		public void getModelDescription(StringBuilder out, int indent){
			};
	    public void getDescription(StringBuilder sb, int indent) {
              // TODO Auto-generated method stub
        };

	}
	
	public class JGamaMethod extends DriftDetectionMethod {
		

		private static final int JGAMAMETHOD_MINNUMINST = 30;
		private int m_n;
		private double m_p;
		private double m_s;
		private double m_psmin;
		private double m_pmin;
		private double m_smin;


		public JGamaMethod() {
			initialize();
		}

		private void initialize() {
			m_n=1;
			m_p = 1;
			m_s = 0;
			m_psmin = Double.MAX_VALUE;
			m_pmin = Double.MAX_VALUE;
			m_smin = Double.MAX_VALUE;    
		}
		
		@Override
		public int computeNextVal(boolean prediction) {
			if (prediction == false) {
				m_p = m_p + (1.0-m_p)/(double)m_n; 
			 } else {
				 m_p = m_p - (m_p)/(double)m_n; 
			 }
			 m_s = Math.sqrt(m_p*(1-m_p)/(double)m_n);
			 
			 
			 m_n++;

			 //System.out.print(prediction + " " + m_n + " " +  (m_p+m_s) + " ");
			 
			 if (m_n < JGAMAMETHOD_MINNUMINST) {
				 return DDM_INCONTROL_LEVEL;
			 }

			 if(m_p+m_s <= m_psmin){
				 m_pmin = m_p;
				 m_smin = m_s;
				 m_psmin = m_p+m_s;
			 }

			 
			 if (m_n > JGAMAMETHOD_MINNUMINST && m_p+m_s > m_pmin + 3*m_smin){
				 initialize();
				 return DDM_OUTCONTROL_LEVEL;
			 } else if (m_p+m_s > m_pmin + 2*m_smin) {
				 return DDM_WARNING_LEVEL;
			 } else {
				 return DDM_INCONTROL_LEVEL;
			 }
		}
		
	}

	public class EDDM extends DriftDetectionMethod {
		private static final double FDDM_OUTCONTROL = 0.9;
		private static final double FDDM_WARNING = 0.95;

		private static final double FDDM_MINNUMINSTANCES = 30;

		private double m_numErrors;
		private int m_minNumErrors = 30;
		private int m_n;
		private int m_d;
		private int m_lastd;

		private double m_mean;
		private double m_stdTemp;
		private double m_m2smax;
		private int m_lastLevel;

		public EDDM() {
			initialize();
		}

		private void initialize() {
			m_n=1;
			m_numErrors=0;
			m_d=0;
			m_lastd=0;
			m_mean=0.0;
			m_stdTemp=0.0;
			m_m2smax=0.0;
			m_lastLevel = DDM_INCONTROL_LEVEL;
		}
		
		@Override
		public int computeNextVal(boolean prediction) {
			//System.out.print(prediction + " " + m_n + " " + probability + " ");
			m_n++;
			if (prediction == false) {
				m_numErrors += 1;
				m_lastd = m_d;
				m_d = m_n-1;
				int distance = m_d - m_lastd;
				double oldmean = m_mean;
				m_mean = m_mean + ((double)distance - m_mean)/m_numErrors;
				m_stdTemp = m_stdTemp + (distance - m_mean)*(distance-oldmean);
				double std = Math.sqrt(m_stdTemp/m_numErrors);
				double m2s = m_mean + 2*std;

				//System.out.print(m_numErrors + " " + m_mean + " " + std + " " + m2s + " " + m_m2smax + " ");

				if (m2s > m_m2smax) {
					if (m_n > FDDM_MINNUMINSTANCES) {
						m_m2smax = m2s;
					}
					m_lastLevel = DDM_INCONTROL_LEVEL;
					//System.out.print(1 + " ");
				} else {
					double p = m2s/m_m2smax;
					//System.out.print(p + " ");
					if (m_n > FDDM_MINNUMINSTANCES && m_numErrors > m_minNumErrors && p < FDDM_OUTCONTROL) {
						initialize();
						return DDM_OUTCONTROL_LEVEL;
					} else if (m_n > FDDM_MINNUMINSTANCES && m_numErrors > m_minNumErrors && p < FDDM_WARNING) {
						m_lastLevel=DDM_WARNING_LEVEL;
						return DDM_WARNING_LEVEL;
					} else {
						m_lastLevel=DDM_INCONTROL_LEVEL;
						return DDM_INCONTROL_LEVEL;
					}
				}
			} else {
				//System.out.print(m_numErrors + " " + m_mean + " " + Math.sqrt(m_stdTemp/m_numErrors) + " " + (m_mean + 2*Math.sqrt(m_stdTemp/m_numErrors)) + " " + m_m2smax + " ");
				//System.out.print(((m_mean + 2*Math.sqrt(m_stdTemp/m_numErrors))/m_m2smax) + " ");
			}
			return m_lastLevel;
		}
		
	}

	public class EWMA extends DriftDetectionMethod {

		private static final int EWMA_MINNUMINST = 30;
		private int m_n;
		private double m_p;
		// Desviación de m_ewma
		private double m_s;
		private double m_ewma;
		private double m_lambda;
		// Mínimo valor alcanzado para ewma
		private double m_ewmamin;
		// Valor de m_p al alcanzar m_ewmamin
		private double m_pmin;
		// Valor de m_s al alcanzar m_ewmamin
		private double m_smin;
		private double m_stdTemp;

		public EWMA() {
			initialize();
			m_lambda = 0.01;//0.2;
		}

		public EWMA(double l) {
			initialize();
			m_lambda = l;
		}

		private void initialize() {
			m_n = 1;
			m_p = 0.0; //1.0
			m_s = 0.0;
			m_ewma = 0.0;
			m_ewmamin = 1.0;
			//m_stdTemp=0.0;
		}

		@Override
		public int computeNextVal(boolean prediction) {
			double oldmean = m_p;
			double dblPred=0.0;
			if (prediction == false) dblPred=1.0;
			m_p = m_p + (dblPred - m_p) / (double) m_n;
			m_ewma = m_lambda *dblPred+ (1 - m_lambda) * m_ewma;

			m_s = Math.sqrt(m_p*(1-m_p)/(double)m_n) ;//* Math.sqrt(m_lambda/(2-m_lambda));
			//m_stdTemp = m_stdTemp + (dblPred - m_p)*(dblPred-oldmean);
			//m_s = Math.sqrt(m_stdTemp/m_n);
			
			m_n++;
			if (m_n>4) m_lambda =  1.0/((double) m_n); // EWMA Adaptive

			//System.out.print(prediction + " " + m_n + " " + m_ewma + " ");

			if (m_n < EWMA_MINNUMINST) {
				return DDM_INCONTROL_LEVEL;
			}

			if (m_ewma <= m_ewmamin) {
				m_ewmamin = m_ewma;
				m_pmin = m_p;
				m_smin = m_s;//* Math.sqrt(m_lambda/(2-m_lambda));
			}
			//System.out.print(m_ewmamin + " " + m_pmin + " " + m_smin + " ");

			if (m_n > EWMA_MINNUMINST && m_ewma +m_s > m_pmin + 3 * m_smin) {
				initialize();
				return DDM_OUTCONTROL_LEVEL;
			} else if (m_ewma + m_s > m_pmin + 2 * m_smin) {
				return DDM_WARNING_LEVEL;
			} else {
				return DDM_INCONTROL_LEVEL;
			}
		}
		
	}

public class AEWMA extends DriftDetectionMethod {

private static final int EWMA_MINNUMINST = 10;
private int m_n;
private double m_p;
// Desviación de m_ewma
private double m_s;
private double m_ewma;
private double m_lambda;
// Mínimo valor alcanzado para ewma
private double m_ewmamin;
// Valor de m_p al alcanzar m_ewmamin
private double m_pmin;
// Valor de m_s al alcanzar m_ewmamin
private double m_smin;
private double m_stdTemp;

private int N; //number of samples in a group
private int M; //moving average
private int m_t; //counter of groups
private double st;
private double stm;
private double xt;
private double xtm;
private double xtt;
private double vt;
private double vtm;

public AEWMA() {
    initialize();
}

private void initialize() {
    m_n = 0;
    m_p = 1;
    m_s = 0;
    m_t = 0;
    m_ewma = 1;
    m_ewmamin = Double.MAX_VALUE;
    m_pmin = Double.MAX_VALUE;
    m_stdTemp = 0.0;
    m_lambda = 0.05;
    N = 40;
    //M = 50;
    xt = 0.0;
    xtt = 0.0;
    xtm = 0.0;
    st = 0.0;
    stm = 0.0;
    vt = 0.0;
    vtm = 0.0;
}

public int computeNextVal(boolean prediction, double probability) {	
	xtt += (prediction == true ? 0.0 : 1.0);        
	m_n++;
	if (m_n == N) {
		m_n = 0;
		m_t++;
		stm = st;
		xtm = xt;
		vtm = vt;
		xt = xtt /(double) N;
		st = Math.sqrt(xt*(1.0-xt) / (double) N ) ;
		xtt = 0.0;

		double Q = (xt-xtm)*(xt-xtm)-((st*st+stm+stm)/N);
		if (Q < 0.0) Q = 0.0;	
		double R = st*st/N;		
		m_lambda = (vtm+Q)/(vtm+Q+R);
		vt = m_lambda * R;

		m_ewma = m_lambda *xt + (1 - m_lambda) * m_ewma;
		m_p = m_p + (xt - m_p) / (double) N;

		if (m_t < EWMA_MINNUMINST) {
			return DDM_INCONTROL_LEVEL;
		}
		if (m_ewma <= m_ewmamin) {
			m_ewmamin = m_ewma;
			m_pmin = m_p;
			m_smin =  Math.sqrt(vt);//m_s * Math.sqrt(m_lambda/(2-m_lambda));
		}
		//System.out.println("T:"+m_t+",m_ewma:"+m_ewma+",m_ewma_min:"+m_ewmamin + ",m_pmin:" + m_pmin + ",m_smin:" + m_smin + " ");

		if (m_ewma > m_pmin + 3 * m_smin) {
			//System.out.println("OUT");
			initialize();
			return DDM_OUTCONTROL_LEVEL;
		} else if (m_ewma > m_pmin + 2 * m_smin) {
			//System.out.println("WARNING");
			return DDM_WARNING_LEVEL;
		} else {
			return DDM_INCONTROL_LEVEL;
		}
	} else {
		return DDM_INCONTROL_LEVEL;
	}   
}

}



	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", Classifier.class, "NaiveBayes");

    public MultiChoiceOption driftDetectionMethodOption = new MultiChoiceOption(
			"driftDetectionMethod", 'd', "Drift detection method to use.", new String[] {
					"DDM", "EDDM", "EWMA", "AEWMA" }, new String[] {
					"DDM: Joao Gama Drift Detection Method",
					"EDDM: Early Drift Detection Method",
					"EWMA: Exponential Weighting Moving Average Detection Method",
					"AEWMA: Adaptive Exponential Weighting Moving Average Detection Method"  }, 0);

	public Classifier classifier; //*****canvi prov per OzaBagLS prov
	
	protected Classifier newclassifier;
	
	protected DriftDetectionMethod driftDetectionMethod;
	
	protected boolean newClassifierReset;

	@Override
	public int measureByteSize() {
		int size = (int) SizeOfAgent.sizeOf(this);
		size += classifier.measureByteSize();
		size += newclassifier.measureByteSize();
		return size;
	}

	@Override
	public void resetLearningImpl() {
		this.classifier = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		this.newclassifier = (Classifier) getPreparedClassOption(this.baseLearnerOption);
		this.classifier.resetLearning();
		this.newclassifier.resetLearning();
		this.driftDetectionMethod =  newDriftDetectionMethod();
		newClassifierReset = false;

	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		int trueClass = (int) inst.classValue();
		boolean prediction;
		if (Utils.maxIndex(this.classifier.getVotesForInstance(inst)) == trueClass) {
			prediction = true;
		} else {
			prediction = false;
		}   
		switch (this.driftDetectionMethod.computeNextVal(prediction) ){
			case DriftDetectionMethod.DDM_WARNING_LEVEL:
				//System.out.println("1 0 W");
				if (newClassifierReset == true) {
					this.newclassifier.resetLearning();
					newClassifierReset = false;
				}
				this.newclassifier.trainOnInstance(inst);
				break;

			case DriftDetectionMethod.DDM_OUTCONTROL_LEVEL:
				//System.out.println("0 1 O");
				this.classifier = null;
				this.classifier = this.newclassifier;
				if (this.classifier instanceof WEKAClassifier) {
					((WEKAClassifier) this.classifier).buildClassifier();
				}
				this.newclassifier = (Classifier) getPreparedClassOption(this.baseLearnerOption);
				this.newclassifier.resetLearning();
				break;

			case DriftDetectionMethod.DDM_INCONTROL_LEVEL:
				//System.out.println("0 0 I");
				newClassifierReset = true;
				break;
			default:
				//System.out.println("ERROR!");
			  
		}

		this.classifier.trainOnInstance(inst);
	}

	public double[] getVotesForInstance(Instance inst) {
		return this.classifier.getVotesForInstance(inst);
	}

	public boolean isRandomizable() {
		return true;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		((AbstractClassifier) this.classifier).getModelDescription(out, indent);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return ((AbstractClassifier) this.classifier).getModelMeasurementsImpl();
	}

	protected DriftDetectionMethod newDriftDetectionMethod() {
		switch (this.driftDetectionMethodOption.getChosenIndex()) {
			case 0:
				return new JGamaMethod();
			case 1:
				return new EDDM();
			case 2:
				return new EWMA();
			case 3:
				return new AEWMA();
		}
		return new DriftDetectionMethod();
	}
}
