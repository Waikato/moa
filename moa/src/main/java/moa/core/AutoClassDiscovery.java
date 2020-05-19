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
import nz.ac.waikato.cms.locator.FixedClassListTraversal;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for discovering classes via reflection in the java class path.
 * <br>
 * If analyzing of classpath fails, it falls back on reading class names
 * from file list {@link #CLASS_LIST} as resource stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 */
public class AutoClassDiscovery {

    protected static final Map<String, String[]> cachedClassNames = new HashMap<String, String[]>();

    protected static ClassCache m_Cache;

    public final static String CLASS_LIST = "moa.classes";

    /**
     * Initializes the class cache
     */
    protected static synchronized void initCache() {
        if (m_Cache == null) {
            m_Cache = new ClassCache();
            // failed to locate any classes on the classpath, maybe inside Weka?
            // try loading fixed list of classes
            if (m_Cache.getClassnames("moa.classifiers.trees").isEmpty()) {
                InputStream inputStream = null;
                try {
                    inputStream = m_Cache.getClass().getClassLoader().getResourceAsStream(CLASS_LIST);
                    m_Cache = new ClassCache(new FixedClassListTraversal(inputStream));
                }
                catch (Exception e) {
                    System.err.println("Failed to initialize class cache from fixed list (" + CLASS_LIST + ")!");
                    e.printStackTrace();
                }
                finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Exception e) {
                            // ignored
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns all class names stored in the cache.
     * @return the class names
     */
    public static List<String> getAllClassNames() {
        List<String> result = new ArrayList<>();
        Iterator<String> pkgs = m_Cache.packages();
        while (pkgs.hasNext()) {
            String pkg = pkgs.next();
            if (pkg.startsWith("moa")) {
                Set<String> classnames = m_Cache.getClassnames(pkg);
                result.addAll(classnames);
            }
        }
        return result;
    }

    public static synchronized String[] findClassNames(String packageNameToSearch) {
        String[] cached = cachedClassNames.get(packageNameToSearch);
        if (cached == null) {
            HashSet<String> classNames = new HashSet<String>();

            initCache();
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
        ArrayList<Class<?>> classesFound = new ArrayList<>();
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

    /**
     * Outputs all class names below "moa" either to stdout or to the
     * file provided as first argument.
     *
     * @param args optional file for storing the classnames
     * @throws Exception if writing to file fails
     */
    public static void main(String[] args) throws Exception {
        initCache();
        List<String> allClassnames = getAllClassNames();
        PrintStream out = System.out;
        if (args.length > 0)
            out = new PrintStream(new File(args[0]));
        Collections.sort(allClassnames);
        for (String clsname: allClassnames)
            out.println(clsname);
        out.flush();
        if (args.length > 0)
            out.close();
    }
}
