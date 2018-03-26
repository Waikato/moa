/*
 *    DietzfelbingerHash.java
 *    Copyright (C) 2015 TU Dortmund University, Germany
 *    @author Jan Stallmann (jan.stallmann@tu-dortmund.de)
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
package moa.clusterers.kmeanspm;

import java.util.Random;

/**
 * Provides a Dietzfelbinger hash function.
 *
 * Citation: Mikkel Thorup:
 * High Speed Hashing for Integers and Strings.
 * CoRR abs/1504.06804 (2015)
 *
 */
public class DietzfelbingerHash {

	private final int hashSize;
	private Random random;

	private long randomFactor;
	private long randomOffset;

	/**
	 * Creates a Dietzfelbinger hash function.
	 *
	 * @param hashSize
	 *            size of the hash function (must be smaller than 31)
	 * @param random
	 *            instance to generate a stream of pseudorandom numbers
	 */
	public DietzfelbingerHash(int hashSize, Random random) {
		assert(hashSize < 31);
		this.hashSize = hashSize;
		this.random = random;
		nextHashFunction();
	}

	/**
	 * Generates a new Dietzfelbinger hash function.
	 *
	 */
	public void nextHashFunction() {
		this.randomFactor = random.nextLong();
		this.randomOffset = random.nextLong();
	}

	/**
	 * Dietzfelbinger hash function.
	 *
	 * @param value
	 *            to hash
	 * @return the result
	 */
	public int hash(long value) {
		return (int) ((this.randomFactor * value + this.randomOffset) >>> (64 - this.hashSize));
	}

}
