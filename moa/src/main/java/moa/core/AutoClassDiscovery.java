/*
 *    AutoClassDiscovery.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.core;

import nz.ac.waikato.cms.locator.ClassCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Class for discovering classes via reflection in the java class path.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 */
public class AutoClassDiscovery {

    protected static final Map<String, String[]> cachedClassNames = new HashMap<String, String[]>();

    protected static ClassCache m_Cache;

    public static synchronized String[] findClassNames(String packageNameToSearch) {
        String[] cached = cachedClassNames.get(packageNameToSearch);
        if (cached == null) {
            HashSet<String> classNames = new HashSet<String>();

            if (m_Cache == null)
                m_Cache = new ClassCache();
            Iterator<String> iter = m_Cache.packages();
            while (iter.hasNext()) {
                String pkg = iter.next();
                if (pkg.equals(packageNameToSearch) || pkg.startsWith(packageNameToSearch + "."))
                    classNames.addAll(m_Cache.getClassnames(pkg));
            }
            cached = classNames.toArray(new String[classNames.size()]);
            Arrays.sort(cached);
            cachedClassNames.put(packageNameToSearch, cached);
        }
        return cached;
    }

    public static Class[] findClassesOfType(String packageNameToSearch,
            Class<?> typeDesired) {
        ArrayList<Class<?>> classesFound = new ArrayList<Class<?>>();
        String[] classNames = findClassNames(packageNameToSearch);
        for (String className : classNames) {
            if (isPublicConcreteClassOfType(className, typeDesired)) {
                try {
                    classesFound.add(Class.forName(className));
                } catch (Exception ignored) {
                    // ignore classes that we cannot instantiate
                }
            }
        }
        return classesFound.toArray(new Class[classesFound.size()]);
    }

    public static boolean isPublicConcreteClassOfType(String className,
            Class<?> typeDesired) {
        Class<?> testClass = null;
        try {
            testClass = Class.forName(className);
        } catch (Exception e) {
            return false;
        }
        int classModifiers = testClass.getModifiers();
        return (java.lang.reflect.Modifier.isPublic(classModifiers)
                && !java.lang.reflect.Modifier.isAbstract(classModifiers)
                && typeDesired.isAssignableFrom(testClass) && hasEmptyConstructor(testClass));
    }

    public static boolean hasEmptyConstructor(Class<?> type) {
        try {
            type.getConstructor();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
