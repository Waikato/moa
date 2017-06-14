/*
 *    MovielensDataset.java
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

package moa.recommender.dataset.impl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javacliparser.FileOption;

import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.recommender.dataset.Dataset;
import moa.tasks.TaskMonitor;

public class MovielensDataset extends AbstractOptionHandler implements Dataset {

    private String strLine;
    
    private BufferedReader br;
    
    public FileOption fileOption = new FileOption("file", 'f',
            "File to load.", "/Users/abifet/Downloads/ml-1M/ratings.dat", "dat", false);

    @Override
    public String getPurposeString() {
        return "A Movie Lens Dataset reader.";
    }

    public void init() {
        FileInputStream fstream = null;
        try {
            //fstream = new FileInputStream("/Users/abifet/Downloads/ml-1M/ratings.dat");
            fstream = new FileInputStream(this.fileOption.getFile());
            DataInputStream in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MovielensDataset.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public boolean next() {
        try {
            return (strLine = ((BufferedReader) br).readLine()) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String toString() {
        return "Movielens";
    }

    @Override
    public int curUserID() {
        String[] split = strLine.split("::");
        return Integer.valueOf(split[0]);
    }

    @Override
    public int curItemID() {
        String[] split = strLine.split("::");
        return Integer.valueOf(split[1]);
    }

    @Override
    public double curRating() {
        String[] split = strLine.split("::");
        return Double.valueOf(split[2]);
    }

    @Override
    public void reset() {
        try {
            br.close();
            //FileInputStream fstream = new FileInputStream("/home/alex/datasets/ml-1m/ratings.dat");
            //DataInputStream in = new DataInputStream(fstream);
            //br = new BufferedReader(new InputStreamReader(in));
            this.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        this.init();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
