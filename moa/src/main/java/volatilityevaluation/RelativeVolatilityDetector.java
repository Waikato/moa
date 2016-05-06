package volatilityevaluation;

import cutpointdetection.CutPointDetector;

public class RelativeVolatilityDetector
{
	// private ADWIN cutpointDetector;
	private CutPointDetector cutpointDetector;
	private Reservoir reservoir;
	private Buffer buffer;
	private double confidence;

	private int timestamp = 0;

	public RelativeVolatilityDetector(CutPointDetector cutpointDetector, int resSize)
	{
		this.cutpointDetector = cutpointDetector;
		this.reservoir = new Reservoir(resSize);
		this.buffer = new Buffer(resSize);
		this.confidence = 0.05;
	}

	public RelativeVolatilityDetector(CutPointDetector cutpointDetector, int resSize, double confidence)
	{
		this.cutpointDetector = cutpointDetector;
		this.reservoir = new Reservoir(resSize);
		this.buffer = new Buffer(resSize);
		this.confidence = confidence;
	}
	
	public double getBufferMean()
	{
		return buffer.getMean();
	}
	
	// Binary input stream (Relative Variance)
	public Boolean setInputVar(double inputValue)
	{
		if (cutpointDetector.setInput(inputValue))
		{
			if (buffer.isFull())
			{
				reservoir.addElement(buffer.add(++timestamp));
			} 
			else
			{
				buffer.add(++timestamp);
			}

			if (buffer.isFull() && reservoir.isFull())
			{

				double RelativeVar = buffer.getStdev() / reservoir.getReservoirStdev();

				if (RelativeVar > 1.0 + confidence || RelativeVar < 1.0 - confidence) // <<<<< Threshold
				{
					reservoir.clear();
					return true;		    
				} 
				else
				{
					return false;
				}
			}
			timestamp = 0;
		} 
		else
		{
			timestamp++;
			return false;
		}
		return false;
	}

	// Interval input (Relative Variance)
	public Boolean setInputVarTP(double inputValue)
	{
		if (buffer.isFull())
		{
			reservoir.addElement(buffer.add(inputValue));
		} 
		else
		{
			buffer.add(inputValue);
		}

		if (buffer.isFull() && reservoir.isFull())
		{
			double RelativeVar = buffer.getStdev() / reservoir.getReservoirStdev();

			if (RelativeVar > 1.0 + confidence || RelativeVar < 1.0 - confidence) // <<<<< Threshold
			{
				buffer.clear();
				reservoir.clear();
				return true;
			} 
			else
			{
				return false;
			}
		}
		timestamp = 0;
		return false;
	}
/*
	public Boolean setInputKS(double inputValue)
	{
		if (cutpointDetector.setInput(inputValue))
		{
			if (buffer.isFull())
			{
				reservoir.addElement(buffer.add(++timestamp));
			} else
			{
				buffer.add(++timestamp);
			}

			if (buffer.isFull() && reservoir.isFull())
			{
				KolmogorovTest KStest = new KolmogorovTest();
				Mergesort Mergesort = new Mergesort();

				double prob = KStest.test(Mergesort.sort(buffer.getBuffer()),Mergesort.sort(reservoir.getReservoir()));
				// System.out.println(prob);

				double RA = buffer.getMean() / reservoir.getReservoirMean();

				if (prob > 0.05 && (RA > 1.5 || RA < 0.75))
				{
					return true;
				} 
				else
				{
					return false;
				}
			}
			timestamp = 0;
		} else
		{
			timestamp++;
			return false;
		}
		return false;
	}

	public Boolean setInputKSTP(double inputValue)
	{
		if (buffer.isFull())
		{
			reservoir.addElement(buffer.add(inputValue));
		} else
		{
			buffer.add(inputValue);
		}

		if (buffer.isFull() && reservoir.isFull())
		{
			KolmogorovTest KStest = new KolmogorovTest();
			Mergesort Mergesort = new Mergesort();

			double prob = KStest.test(Mergesort.sort(buffer.getBuffer()),
					Mergesort.sort(reservoir.getReservoir()));
			// System.out.println(prob);

			double RA = buffer.getMean() / reservoir.getReservoirMean();

			if (prob > 0.05 && (RA > 1.1 || RA < 0.9))
			{
				return true;
			} else
			{
				return false;
			}
		}
		timestamp = 0;
		return false;

	}
*/
	// Relative Average
	public Boolean setInput(double inputValue)
	{
		if (cutpointDetector.setInput(inputValue))
		{
			reservoir.addElement(++timestamp);

			double mean = reservoir.getReservoirMean();
			double stdev = reservoir.getReservoirStdev();
			double Z = (timestamp - mean) / stdev;

			double relativeAverage = timestamp / mean;
			timestamp = 0;

			if (relativeAverage > 1.0 + confidence || relativeAverage < 1.0 - confidence)
			{
				return true;
			} 
			else
			{
				return false;
			}

		} else
		{
			timestamp++;
			return false;
		}
	}

	public Boolean setInputTP(double inputValue)
	{
		reservoir.addElement(inputValue);

		double mean = reservoir.getReservoirMean();
		double stdev = reservoir.getReservoirStdev();

		double relativeAverage = inputValue / mean;
		timestamp = 0;

		if (relativeAverage > 1.0 + confidence || relativeAverage < 1.0 - confidence)
		{
			return true;
		} 
		else
		{
			return false;
		}
	}
/*
	public String setInputTPS(double inputValue) throws IOException
	{
		reservoir.addElement(inputValue);

		double mean = reservoir.getReservoirMean();
		double stdev = reservoir.getReservoirStdev();
		double Z = (inputValue - mean) / stdev;

		// double confidence = 1.69;
		double confidence = 1.96;
		// double confidence = 2.58;

		String s = "";

		if (Math.abs(Z) > confidence)
		{
			s = "true,";
		} else
		{
			s = "false,";
		}

		double relativeAverage = inputValue / mean;
		return s + mean + "," + stdev + "," + Z + "," + relativeAverage + "\n";
	}

	public String setInputS(double inputValue)
	{
		if (cutpointDetector.setInput(inputValue))
		{
			reservoir.addElement(++timestamp);
			String s = "";
			double mean = reservoir.getReservoirMean();
			double stdev = reservoir.getReservoirStdev();
			double Z = (timestamp - mean) / stdev;

			// double confidence = 1.69;
			double confidence = 1.96;
			// double confidence = 2.58;

			double relativeAverage = timestamp / mean;

			if (Math.abs(Z) > confidence)
			{
				s = "true,";
			} else
			{
				s = "false,";
			}

			timestamp = 0;

			return s + mean + "," + stdev + "," + Z + "," + relativeAverage;
		} else
		{
			timestamp++;
			return "";
		}
	}
*/
	public class Mergesort
	{
		private double[] numbers;
		private double[] helper;

		private int number;

		public double[] sort(double[] values)
		{
			this.numbers = values;
			number = values.length;
			this.helper = new double[number];
			mergesort(0, number - 1);
			return numbers;
		}

		private void mergesort(int low, int high)
		{
			// Check if low is smaller then high, if not then the array is
			// sorted
			if (low < high)
			{
				// Get the index of the element which is in the middle
				int middle = low + (high - low) / 2;
				// Sort the left side of the array
				mergesort(low, middle);
				// Sort the right side of the array
				mergesort(middle + 1, high);
				// Combine them both
				merge(low, middle, high);
			}
		}

		private void merge(int low, int middle, int high)
		{

			// Copy both parts into the helper array
			for (int i = low; i <= high; i++)
			{
				helper[i] = numbers[i];
			}

			int i = low;
			int j = middle + 1;
			int k = low;
			// Copy the smallest values from either the left or the right side
			// back
			// to the original array
			while (i <= middle && j <= high)
			{
				if (helper[i] <= helper[j])
				{
					numbers[k] = helper[i];
					i++;
				} else
				{
					numbers[k] = helper[j];
					j++;
				}
				k++;
			}
			// Copy the rest of the left side of the array into the target array
			while (i <= middle)
			{
				numbers[k] = helper[i];
				k++;
				i++;
			}

		}
	}
}
