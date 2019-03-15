/*
 *    AnyOut.java
 *
 *    @author I. Assent, P. Kranen, C. Baldauf, T. Seidl
 *    @author G. Piskas, A. Gounaris
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

package moa.clusterers.outliers.AnyOut;

import com.github.javacliparser.Options;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.ArrayList;
import moa.clusterers.outliers.MyBaseOutlierDetector;
import moa.clusterers.outliers.AnyOut.util.DataObject;
import moa.clusterers.outliers.AnyOut.util.DataSet;

public class AnyOut extends MyBaseOutlierDetector {
	private static final long serialVersionUID = 1L; 

	private final int FIRST_OBJ_ID = 0;
	private final double minDepth = 0.5;
	private final double maxDepth = 0.9;
	
	private AnyOutCore anyout;
	private int idCounter;
	private int windowSize;
	private ArrayList<DataObject> objects;
	private DataSet trainingSet;
	private int trainingCount;
	private int trainingSetSize;
	private int truePositive, falsePositive, totalOutliers;
	private int outlierClass;

	public AnyOut() {
		anyout = new AnyOutCore();
	}
	
	@Override
	public void resetLearningImpl() {
		anyout.resetLearning();
		super.resetLearningImpl();
	}
	
	@Override
	protected void Init() {
		trainingCount = 0;
		truePositive = 0;
		falsePositive = 0;
		totalOutliers = 0;
		outlierClass = -1;
		trainingSetSize = anyout.trainingSetSizeOption.getValue();
		idCounter = FIRST_OBJ_ID;
		windowSize = anyout.horizonOption.getValue();
		objects = new ArrayList<DataObject>();
		super.Init();
	}

	@Override
	protected void ProcessNewStreamObj(Instance i) {
		if (trainingSetSize >= trainingCount) {
			if (trainingSet == null) {
				trainingSet = new DataSet(i.numAttributes()-1);
			}
			//fill training set
			DataObject o = new DataObject(idCounter++, i);
			trainingSet.addObject(o);
			trainingCount++;
		} else {
			// Train once.
			if (trainingSetSize != -1) {
				anyout.train(trainingSet);
				trainingSet.clear();
				trainingSetSize = -1;
				outlierClass = i.classAttribute().numValues() - 1;
			}
			
			// Create DataObject from instance.
			DataObject o = new DataObject(idCounter++, i);
			objects.add(o);
			
			// Count ground truth.
			if (o.getClassLabel() == outlierClass) {
				totalOutliers += 1;
			}
			
			// Update window objects.
			if (objects.size() > windowSize) {
				DataObject obj = objects.get(0);
				objects.remove(0);
				anyout.removeObject(obj.getId());
				RemoveExpiredOutlier(new Outlier(obj.getInstance(), obj.getId(), obj));
			}
			
			// Calculate scores for the object.
			anyout.initObject(o.getId(), o.getFeatures());
			
			// Simulate anyout characteristics.
			double depth = Math.random();
			if (depth < minDepth) {
				depth = minDepth;
			} else if (depth > maxDepth) {
				depth = maxDepth;
			}
			
			while (anyout.moreImprovementsPossible(o.getId(), depth)){
				anyout.improveObjectOnce(o.getId());
			}			
			
			// Learn object into ClusTree.
			anyout.learnObject(o.getFeatures());

			// Evaluation of the window objects.
			for (DataObject obj : objects){
				int id = obj.getId();
				if(anyout.isOutlier(id)) {
					if(obj.isOutiler() == false) { // not already outlier.
						// Statistics gathering.
						if(obj.getClassLabel() == outlierClass) {
							truePositive += 1;
						}  else {
							falsePositive += 1;
						}
						AddOutlier(new Outlier(obj.getInstance(), id, obj));
						obj.setOutiler(true);
					}
				} else {
					RemoveOutlier(new Outlier(obj.getInstance(), id, obj));
					obj.setOutiler(false);
				}
			}
		}
	}
	
	@Override
	public String getPurposeString() {
		return "Anyout: Anytime Outlier Detector based on ClusTree";
	}

	@Override
	public Options getOptions() {
		return anyout.getOptions();
	}

	private int getWindowEnd() {
		return idCounter - 1;
	}

	private int getWindowStart() {
		int x = getWindowEnd() - windowSize + 1;
		if (x < FIRST_OBJ_ID)
			x = FIRST_OBJ_ID;
		return x;
	}
	    
    @Override
    protected boolean IsNodeIdInWin(long id) {
        if ((getWindowStart() <= id) && (id <= getWindowEnd()) )
            return true;
        else
            return false;
    }

    @Override
    public String getObjectInfo(Object o) {
    	DataObject obj = (DataObject) o;
    	double[] features = obj.getFeatures();
    	int id = obj.getId();
        StringBuilder sb = new StringBuilder();        
        sb.append("<html>");
        sb.append("<table>");
        sb.append("<tr><td><b>ID:</b></td><td>" + id + "</td></tr>");
        sb.append("<tr><td><b>X, Y:</b></td><td>" + String.format("%.4f", features[0]) + ", " + String.format("%.4f", features[1]) + "</td></tr>");
        sb.append("<tr><td><b>Oscore:</b></td><td>" + String.format("%.4f", anyout.getOutlierScore(id)) + "</td></tr>");
        sb.append("<tr><td><b>Conf:</b></td><td>" + String.format("%.4f", anyout.getConfidence(id)) + "</td></tr>");
        sb.append("</table>");
        sb.append("</html>");
        
        return sb.toString();
    }
    
    @Override
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        int sum = truePositive + falsePositive;
        sb.append("Statistics:\n\n");
        sb.append(String.format("  Outliers found: %d (%.1f%%)\n", sum, (100 * sum) / (double)totalOutliers));
        sb.append(String.format("  True positive found: %d (%.1f%%)\n", truePositive, (100 * truePositive) / (double)totalOutliers));
        sb.append(String.format("  False positive found: %d (%.1f%%)\n", falsePositive, (100 * falsePositive) / (double)totalOutliers));
        sb.append("\n");
        sb.append("  Max memory usage: " + iMaxMemUsage + " MB\n");
        sb.append("  Total process time: " + String.format("%.2f ms", nTotalRunTime / 1000.0) + "\n");
        
        return sb.toString();
    }
}
