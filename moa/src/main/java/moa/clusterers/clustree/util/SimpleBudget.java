/*
 *    SimpleBudget.java
 *    Copyright (C) 2010 RWTH Aachen University, Germany
 *    @author Reidl (moa@cs.rwth-aachen.de)
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

package moa.clusterers.clustree.util;

public class SimpleBudget implements Budget {

    public static final int INT_ADD = 1;
    public static final int INT_MULT = 1;
    public static final int INT_DIV = 1;
    public static final int DOUBLE_ADD = 1;
    public static final int DOUBLE_MULT = 1;
    public static final int DOUBLE_DIV = 10;
    private int time;

    public SimpleBudget(int time) {
        assert (time >= 0);
        this.time = time;
    }

    @Override
    public boolean hasMoreTime() {
        return time > 0;
    }

    @Override
    public void integerAddition() {
        time -= INT_ADD;
    }

    @Override
    public void integerAddition(int number) {
        time -= INT_ADD * number;
    }

    @Override
    public void doubleAddition() {
        time -= DOUBLE_ADD;
    }

    @Override
    public void doubleAddition(int number) {
        time -= DOUBLE_ADD * number;
    }

    @Override
    public void integerMultiplication() {
        time -= INT_MULT;
    }

    @Override
    public void integerMultiplication(int number) {
        time -= INT_MULT * number;
    }

    @Override
    public void doubleMultiplication() {
        time -= DOUBLE_MULT;
    }

    @Override
    public void doubleMultiplication(int number) {
        time -= DOUBLE_MULT * number;
    }

    @Override
    public void integerDivision() {
        time -= INT_DIV;
    }

    @Override
    public void integerDivision(int number) {
        time -= INT_DIV * number;
    }

    @Override
    public void doubleDivision() {
        time -= DOUBLE_DIV;
    }

    @Override
    public void doubleDivision(int number) {
        time -= DOUBLE_DIV * number;
    }
}
