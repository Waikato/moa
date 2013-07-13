/*
 *    JesterDataset.java
 *    Copyright (C) 2012 Universitat Politecnica de Catalunya
 *    @author Alex Catarineu (a.catarineu@gmail.com)
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
package moa.recommender.dataset.impl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import moa.recommender.dataset.Dataset;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.FileOption;
import moa.tasks.TaskMonitor;

public class JesterDataset extends AbstractOptionHandler implements Dataset {

    private String strLine;
    private BufferedReader br;
    public FileOption fileOption = new FileOption("file", 'f',
            "File to load.", "/home/alicia/datasets/jester/jester_ratings.dat", "dat", false);

    /* public JesterDataset() throws IOException {
     super();
     FileInputStream fstream = new FileInputStream("/home/alicia/datasets/jester/jester_ratings.dat");
     DataInputStream in = new DataInputStream(fstream);
     br = new BufferedReader(new InputStreamReader(in));
     }*/
    @Override
    public String getPurposeString() {
        return "A Jester Dataset reader.";
    }

    public void init() {
        FileInputStream fstream = null;
        try {
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
            return (strLine = br.readLine()) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String toString() {
        return "Jester";
    }

    @Override
    public int curUserID() {
        String[] split = strLine.split("\\s+");
        return Integer.valueOf(split[0]);
    }

    @Override
    public int curItemID() {
        String[] split = strLine.split("\\s+");
        return Integer.valueOf(split[1]);
    }

    @Override
    public double curRating() {
        String[] split = strLine.split("\\s+");
        double rating = Double.valueOf(split[2]);
        return (rating / 10) * 2 + 3;
    }

    @Override
    public void reset() {
        try {
            br.close();
            //FileInputStream fstream = new FileInputStream("/home/alicia/datasets/jester/jester_ratings.dat");
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
