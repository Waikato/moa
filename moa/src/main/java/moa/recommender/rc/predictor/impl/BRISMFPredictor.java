/*
 *    BRISMFPredictor.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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

package moa.recommender.rc.predictor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import moa.recommender.rc.data.RecommenderData;
import moa.recommender.rc.utils.Pair;
import moa.recommender.rc.utils.Rating;
import moa.recommender.rc.utils.SparseVector;
import moa.recommender.rc.utils.Updatable;

/**
 * Implementation of the algorithm described in Scalable 
 * Collaborative Filtering Approaches for Large Recommender
 * Systems (Gábor Takács, István Pilászy, Bottyán Németh, 
 * and Domonkos Tikk). A feature vector is learned for every 
 * user and item, so that the prediction of a rating is roughly
 * the dot product of the corresponding user and item vector. 
 * Stochastic gradient descent is used to train the model, 
 * minimizing its prediction error. Both Tikhonov regularization
 * and early stopping are used to reduce overfitting. The 
 * algorithm allows batch training (from scratch, using all
 * ratings available at the moment) as well as incremental,
 * by retraining only the affected user and item vectors when 
 * a new rating is inserted.
 * 
 * <p>Parameters:</p>
 * <ul>
 * <li> features - the number of features to be trained for each user and 
 *      item</li>
 * <li> learning rate - the learning rate used in the regularization</li>
 * <li> ratio - the regularization ratio to be used in the Tikhonov 
 *      regularization</li>
 * <li> iterations - the number of iterations to be used when retraining
 *      user and item features (online training). </li>
 * </lu>
 * 
 */

public class BRISMFPredictor implements Updatable {
    
    protected RecommenderData data;
    protected int nFeatures;
    protected HashMap<Integer, float[]> userFeature;
    protected HashMap<Integer, float[]> itemFeature;
    protected Random rnd;
    protected double lRate = 0.01;
    protected double rFactor = 0.02;
    protected int nIterations = 30;
    
    public void setLRate(double lRate) {
        this.lRate = lRate;
    }
    
    public void setRFactor(double rFactor) {
        this.rFactor = rFactor;
    }
    
    public void setNIterations(int nIterations) {
        this.nIterations = nIterations; 
    }
    
    public RecommenderData getData() {
    	return data;
    }
    
    public BRISMFPredictor(int nFeatures, RecommenderData data, boolean train) {
        this.data = data;
        this.nFeatures = nFeatures;
        this.userFeature = new HashMap<Integer, float[]>();
        this.itemFeature = new HashMap<Integer, float[]>();
        this.rnd = new Random(12345);
        data.attachUpdatable(this);
        if (train) train();
    }
    
    public BRISMFPredictor(int nFeatures, RecommenderData data, double lRate, double rFactor, boolean train) {
        this.data = data;
        this.nFeatures = nFeatures;
        this.userFeature = new HashMap<Integer, float[]>();
        this.itemFeature = new HashMap<Integer, float[]>();
        this.rnd = new Random(12345);
        this.lRate = lRate;
        this.rFactor = rFactor;
        data.attachUpdatable(this);
        if (train) train();
    }
    
    private void resetFeatures(float[] feats, boolean userFeats) {
        int n = feats.length;
        for (int i = 0; i < n; ++i)
            feats[i] = (float)0.01*(rnd.nextFloat()*2 - 1);
        if (userFeats) feats[0] = 1;
        else feats[1] = 1;
    }
    
    public double predictRating(int userID, int itemID) {
        float[] userFeats = userFeature.get(userID);
        float[] itemFeats = itemFeature.get(itemID);
        return predictRating(userFeats, itemFeats);
    }
    
    public double predictRating(float userFeats[], float itemFeats[]) {
        double ret = data.getGlobalMean();
        if (userFeats != null && itemFeats != null)
            for (int i = 0; i < nFeatures; ++i)
                ret += userFeats[i]*itemFeats[i];

        if (ret < data.getMinRating()) ret = data.getMinRating();
        else if (ret > data.getMaxRating()) ret = data.getMaxRating();
        
        return ret;
    }
    
    public float[] trainUserFeats(List<Integer> itm, List<Double> rat, int nIts) {
        float[] userFeats = new float[nFeatures];
        resetFeatures(userFeats, true);
        
        int n = itm.size();
        for (int k = 0; k < nIts; ++k) {
            for (int i = 0; i < n; ++i) {
                int itemID = itm.get(i);
                float[] itemFeats = itemFeature.get(itemID);
                double rating = rat.get(i);
                double pred = predictRating(userFeats, itemFeats);
                double err = rating - pred;
                
                if (itemFeats != null)
                    for (int j = 1; j < nFeatures; ++j) 
                        userFeats[j] += lRate*(err*itemFeats[j] - rFactor*userFeats[j]);
            }
        }
        
        return userFeats;
    }
    
    public float[] trainItemFeats(int itemID, List<Integer> usr, List<Double> rat, int nIts) {
        float[] itemFeats = new float[nFeatures];
        resetFeatures(itemFeats, false);
        
        int n = usr.size();
        for (int k = 0; k < nIts; ++k) {
            for (int i = 0; i < n; ++i) {
                int userID = usr.get(i);
                float[] userFeats = userFeature.get(userID);
                double rating = rat.get(i);
                double pred = predictRating(userFeats, itemFeats);
                double err = rating - pred;
                
                if (userFeats != null) {
                    itemFeats[0] += lRate*(err*userFeats[0] - rFactor*itemFeats[0]);
                    for (int j = 2; j < nFeatures; ++j)
                        itemFeats[j] += lRate*(err*userFeats[j] - rFactor*itemFeats[j]);
                }
            }
        }
        
        return itemFeats;
    }
    
    public void trainUser(int userID, List<Integer> itm, List<Double> rat, int nIts) {
        userFeature.put(userID, trainUserFeats(itm, rat, nIts));
    }
    
    public void trainUser(int userID, int nIts) {
        SparseVector usrRats = data.getRatingsUser(userID);
        ArrayList<Integer> itm = new ArrayList<Integer>();
        ArrayList<Double> rat = new ArrayList<Double>();
        Iterator<Pair<Integer, Double>> it = usrRats.iterator();
        
        while (it.hasNext()) {
            Pair<Integer, Double> p = it.next();
            itm.add(p.getFirst());
            rat.add(p.getSecond());
        }
        trainUser(userID, itm, rat, nIts);
    }
    
    public void trainUser(int userID, List<Integer> itm, List<Double> rat) {
        userFeature.put(userID, trainUserFeats(itm, rat, nIterations));
    }
    
    public void trainItem(int itemID) {
        SparseVector itmRats = data.getRatingsItem(itemID);
        ArrayList<Integer> usr = new ArrayList<Integer>();
        ArrayList<Double> rat = new ArrayList<Double>();
        Iterator<Pair<Integer, Double>> it = itmRats.iterator();
        
        while (it.hasNext()) {
            Pair<Integer, Double> p = it.next();
            usr.add(p.getFirst());
            rat.add(p.getSecond());
        }
        trainItem(itemID, usr, rat);
    }
    
    public void trainItem(int itemID, int nIts) {
        SparseVector itmRats = data.getRatingsItem(itemID);
        ArrayList<Integer> usr = new ArrayList<Integer>();
        ArrayList<Double> rat = new ArrayList<Double>();
        Iterator<Pair<Integer, Double>> it = itmRats.iterator();
        
        while (it.hasNext()) {
            Pair<Integer, Double> p = it.next();
            usr.add(p.getFirst());
            rat.add(p.getSecond());
        }
        trainItem(itemID, usr, rat, nIts);
    }
    
    public void trainUser(int userID) {
        SparseVector usrRats = data.getRatingsUser(userID);
        ArrayList<Integer> itm = new ArrayList<Integer>();
        ArrayList<Double> rat = new ArrayList<Double>();
        Iterator<Pair<Integer, Double>> it = usrRats.iterator();
        
        while (it.hasNext()) {
            Pair<Integer, Double> p = it.next();
            itm.add(p.getFirst());
            rat.add(p.getSecond());
        }
        trainUser(userID, itm, rat);
    }
    
    public void trainItem(int itemID, List<Integer> usr, List<Double> rat) {
        itemFeature.put(itemID, trainItemFeats(itemID, usr, rat, nIterations));
    }
    
    public void trainItem(int itemID, List<Integer> usr, List<Double> rat, int nIts) {
        itemFeature.put(itemID, trainItemFeats(itemID, usr, rat, nIts));
    }
    
    public void train() {
        userFeature.clear();
        itemFeature.clear();
        
        int n = data.getNumRatings();
        
        Iterator<Integer> it = data.getUsers().iterator();
        while (it.hasNext()) {
            float[] feats = new float[nFeatures];
            resetFeatures(feats, true);
            userFeature.put(it.next(), feats);
        }
        
        it = data.getItems().iterator();
        while (it.hasNext()) {
            float[] feats = new float[nFeatures];
            resetFeatures(feats, false);
            itemFeature.put(it.next(), feats);
        }

        int exit = 0;
        double lastRMSE = 1e20;
        
        int count = 0;
        int trainDiv = Math.max(20, n/1000000);
        ArrayList<Rating> ratTest = new ArrayList<Rating>(n/trainDiv);
        do {
            long start = System.currentTimeMillis();
            Iterator<Rating> ratIt = data.ratingIterator();
            int idx = 0;
            
            while (ratIt.hasNext()) {
                Rating rat = ratIt.next();
                if (idx%trainDiv == 0) {
                    if (count == 0) ratTest.add(rat);
                }
                else {
                    int userID = rat.userID;
                    int itemID = rat.itemID;
                    double rating = rat.rating;
                    float[] userFeats = userFeature.get(userID);
                    float[] itemFeats = itemFeature.get(itemID);
                    
                    double pred = predictRating(userFeats, itemFeats);
                    double err = rating - pred;
                    
                    itemFeats[0] += lRate*(err*userFeats[0] - rFactor*itemFeats[0]);
                    userFeats[1] += lRate*(err*itemFeats[1] - rFactor*userFeats[1]);
                    for (int j = 2; j < nFeatures; ++j) {
                        double uv = userFeats[j];
                        userFeats[j] += lRate*(err*itemFeats[j] - rFactor*userFeats[j]);
                        itemFeats[j] += lRate*(err*uv - rFactor*itemFeats[j]);
                    }
                }
                ++idx;
            }
            int nTest = ratTest.size();

            double sum = 0;
            for (int i = 0; i < nTest; ++i) {
                int userID = ratTest.get(i).userID;
                int itemID = ratTest.get(i).itemID;
                double rating = ratTest.get(i).rating;
                double pred = predictRating(userID, itemID);
                sum += Math.pow(rating - pred, 2);
            }
            
            double curRMSE = Math.sqrt(sum/(double)nTest);
            System.out.println(curRMSE + " " + (System.currentTimeMillis() - start)/1000);
            if (curRMSE + 0.0001 >= lastRMSE) {
                ++exit;
            }
            lastRMSE = curRMSE;
            ++count;
        }
        while (exit < 1);
    }
    
    public float[] getUserFeatures(int userID) {
        return userFeature.get(userID);
    }

    public float[] getItemFeatures(int itemID) {
        return itemFeature.get(itemID);
    }

    public int getNumFeatures() {
        return nFeatures;
    }
    
    @Override
    public void updateNewUser(int userID, List<Integer> ratedItems,
            List<Double> ratings) {
        if (!ratedItems.isEmpty()) {
            trainUser(userID, ratedItems, ratings);
        }
    }

    @Override
    public void updateNewItem(int itemID, List<Integer> ratingUsers,
            List<Double> ratings) {
        if (!ratingUsers.isEmpty()) {
            trainItem(itemID, ratingUsers, ratings);
        }
    }

    @Override
    public void updateRemoveUser(int userID) {
        userFeature.remove(userID);
    }

    @Override
    public void updateRemoveItem(int itemID) {
        itemFeature.remove(itemID);
    }
    
    //We retrain the user/item separately, depending on a probability
    //calculated using the error when predicting the new rating
    //TODO: parametrize this
    @Override
    public void updateSetRating(int userID, int itemID, double rating) {
        double nUsr = data.countRatingsUser(userID);
        double nItm = data.countRatingsItem(itemID);
        double prob1 = Math.pow(0.99, nUsr);
        double prob2 = Math.pow(0.99, nItm);

        if (nUsr < 5 || rnd.nextDouble() < prob1) {
            SparseVector usrRats = data.getRatingsUser(userID);
            ArrayList<Integer> itm = new ArrayList<Integer>();
            ArrayList<Double> rat = new ArrayList<Double>();
            
            //Train user
            boolean found = false;
            Iterator<Pair<Integer, Double>> it = usrRats.iterator();
            while (it.hasNext()) {
                Pair<Integer, Double> p = it.next();
                itm.add(p.getFirst());
                if (p.getFirst() == itemID) {
                    found = true;
                    rat.add(rating);
                }
                else rat.add(p.getSecond());
            }
            if (!found) {
                itm.add(itemID);
                rat.add(rating);
            }
            trainUser(userID, itm, rat);
        }
        
        if (nItm < 5 || rnd.nextDouble() < prob2) {
            SparseVector itmRats = data.getRatingsItem(itemID);
            //Train item
            Iterator<Pair<Integer, Double>> it = itmRats.iterator();
            boolean found = false;
            ArrayList<Integer> usr = new ArrayList<Integer>();
            ArrayList<Double> rat = new ArrayList<Double>();
            while (it.hasNext()) {
                Pair<Integer, Double> p = it.next();
                usr.add(p.getFirst());
                if (p.getFirst() == userID) {
                    found = true;
                    rat.add(rating);
                }
                else rat.add(p.getSecond());
            }
            if (!found) {
                usr.add(itemID);
                rat.add(rating);
            }
            trainItem(itemID, usr, rat);
        }
    }

    @Override
    public void updateRemoveRating(int userID, int itemID) {
    }

	public List<Double> predictRatings(int userID, List<Integer> itemIDS) {
		int n = itemIDS.size();
		ArrayList<Double> ret = new ArrayList<Double>(n);
		for (int i = 0; i < n; ++i)
			ret.add(predictRating(userID, itemIDS.get(i)));
		return ret;
	}

}
