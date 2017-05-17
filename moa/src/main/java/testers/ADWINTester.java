package testers;

import inputstream.HyperplaneGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import volatilityevaluation.VolatilityPredictionFeatureExtractor;
import classifiers.thirdparty.VFDT;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.util.ArrayList;
import java.util.Random;

import cutpointdetection.ADWIN;

public class ADWINTester implements Tester {

	int inst = 250000;

	public ADWINTester() {

	}

	public void doTest2() {
		try {
			String arff = "";
			String[][] source = new String[10][100000];
			BufferedReader br = new BufferedReader(
					new FileReader("C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_1M.arff"));
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";
			arff += br.readLine() + "\n";

			String line = "";
			int r = 0, c = 0;

			while ((line = br.readLine()) != null) {
				source[r][c] = line + "\n";

				c++;

				if (c % 100000 == 0) {
					r++;
					c = 0;
				}
			}

			br.close();

			for (int i = 0; i < 30; i++) {
				Random RNG = new Random(i);
				ArrayList<Integer> v = new ArrayList<Integer>();

				v.add(0);
				v.add(1);
				v.add(2);
				v.add(3);
				v.add(4);
				v.add(5);
				v.add(6);
				v.add(7);
				v.add(8);
				v.add(9);

				BufferedWriter bw = new BufferedWriter(new FileWriter(
						"C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane\\Hyperplane1M_" + i
								+ ".arff"));
				bw.write(arff);

				int row = 0;
				for (int k = 0; k < 10; k++) {
					row = RNG.nextInt(v.size());
					row = v.remove(row);

					System.out.print(row + ",");

					for (int j = 0; j < 100000; j++) {
						bw.write(source[row][j]);
					}
				}

				bw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doTest() {

	}

	public void doTest4() {
		try {
			for (int i = 0; i < 30; i++) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						"C:\\Users\\ACER\\Desktop\\ClassifierCaseStudy\\Stream\\Hyperplane_3drift\\Hyperplane1M_" + i
								+ ".arff"));

				bw.write("@RELATION uniform\n" + "\n" + "@ATTRIBUTE 1 numeric\n" + "@ATTRIBUTE 2 numeric\n"
						+ "@ATTRIBUTE 3 numeric\n" + "@ATTRIBUTE 4 numeric\n" + "@ATTRIBUTE 5 numeric\n"
						+ "@ATTRIBUTE 6 numeric\n" + "@ATTRIBUTE 7 numeric\n" + "@ATTRIBUTE 8 numeric\n"
						+ "@ATTRIBUTE class {1,0}\n" + "\n" + "@DATA\n");

				HyperplaneGenerator gen = new HyperplaneGenerator(i * 4, 8, 0, 0.1, 0.05);
				for (int j = 0; j < inst; j++) {
					bw.write(gen.getNextTransaction() + "\n");
				}
				gen.induceDrift();
				// HyperplaneGenerator gen2 = new HyperplaneGenerator(i*4+1, 8,
				// 0, 0.1, 0);
				for (int j = 0; j < inst; j++) {
					bw.write(gen.getNextTransaction() + "\n");
				}
				gen.induceDrift();
				// HyperplaneGenerator gen3 = new HyperplaneGenerator(i*4+2, 8,
				// 0, 0.1, 0);
				for (int j = 0; j < inst; j++) {
					bw.write(gen.getNextTransaction() + "\n");
				}
				gen.induceDrift();
				// HyperplaneGenerator gen4 = new HyperplaneGenerator(i*4+3, 8,
				// 0, 0, 0);
				for (int j = 0; j < inst; j++) {
					bw.write(gen.getNextTransaction() + "\n");
				}
				bw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doTest3() {
		try {

			for (int i = 0; i < 10; i++) {
				VolatilityPredictionFeatureExtractor extractor = new VolatilityPredictionFeatureExtractor();
				ArffLoader loader = new ArffLoader();
				loader.setFile(new File("C:\\Users\\ACER\\Desktop\\iid experiments\\poisson1\\" + i + ".arff"));
				Instances struc = loader.getStructure();
				struc.setClassIndex(struc.numAttributes() - 1);

				VFDT ht = new VFDT();
				ht.buildClassifier(struc);
				Instance current;

				int c = 0;
				while ((current = loader.getNextInstance(struc)) != null) {
					// ht.updateClassifier(current);
					ht.addInstance(current); //updated with my code

					double out = ht.classifyInstance(current);
					double pred = out == current.classValue() ? 1.0 : 0.0;

					if (c > 100000) {
						extractor.extract(pred);
					}
					c++;
				}

				System.out.println(extractor.getModel());

				BufferedWriter bw = new BufferedWriter(
						new FileWriter("C:\\Users\\ACER\\Desktop\\iid experiments\\poisson1\\gamma_" + i + ".csv"));

				for (double d : extractor.getReservoir().elements) {
					bw.write(d + "\n");
				}
				bw.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
