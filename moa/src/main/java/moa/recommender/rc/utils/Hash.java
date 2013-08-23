/*
 *    Hash.java
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
