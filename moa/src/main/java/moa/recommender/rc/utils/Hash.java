/*
 *    Hash.java
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

package moa.recommender.rc.utils;

public class Hash {
    public static int hashCode(int a) {
       a = (a+0x7ed55d16) + (a<<12);
       a = (a^0xc761c23c) ^ (a>>>19);
       a = (a+0x165667b1) + (a<<5);
       a = (a+0xd3a2646c) ^ (a<<9);
       a = (a+0xfd7046c5) + (a<<3);
       a = (a^0xb55a4f09) ^ (a>>>16);
       return a;
    }
}
