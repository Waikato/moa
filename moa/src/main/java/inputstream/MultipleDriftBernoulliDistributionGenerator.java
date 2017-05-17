package inputstream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class MultipleDriftBernoulliDistributionGenerator {
	public void generateInput(double startingMean, int numDrifts, int randomSeed, int length, boolean writeSetting, String fileDir){
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(fileDir, writeSetting));
			//compute length of sub stream
			int substreamLength = length/numDrifts;
			double[] mean = {startingMean};
			BernoulliDistributionGenerator gen = null;
			Random ran = new Random(randomSeed);
			
			for(int i=0; i<numDrifts-1; i++)
			{
				gen = new BernoulliDistributionGenerator(mean, substreamLength, randomSeed);
				
				while(gen.hasNextTransaction())
				{
					int bit = Integer.parseInt(gen.getNextTransaction());
					bWriter.write(bit + "\n");
				}
				mean[0] = ran.nextDouble();
				
			}
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
