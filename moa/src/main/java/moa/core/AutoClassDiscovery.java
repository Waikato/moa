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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class for discovering classes via reflection in the java class path.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class AutoClassDiscovery {

    protected static final Map<String, String[]> cachedClassNames = new HashMap<String, String[]>();

    public static String[] findClassNames(String packageNameToSearch) {
        String[] cached = cachedClassNames.get(packageNameToSearch);
        if (cached == null) {
            HashSet<String> classNames = new HashSet<String>();
            /*StringTokenizer pathTokens = new StringTokenizer(System
            .getProperty("java.class.path"), File.pathSeparator);*/
            String packageDirName = packageNameToSearch.replace('.',
                    File.separatorChar);
            String packageJarName = packageNameToSearch.length() > 0 ? (packageNameToSearch.replace('.', '/') + "/")
                    : "";
            String part = "";


            AutoClassDiscovery adc = new AutoClassDiscovery();
            URLClassLoader sysLoader = (URLClassLoader) adc.getClass().getClassLoader();
            URL[] cl_urls = sysLoader.getURLs();

            for (int i = 0; i < cl_urls.length; i++) {
                part = cl_urls[i].toString();
                if (part.startsWith("file:")) {
                    part = part.replace(" ", "%20");
                    try {
                        File temp = new File(new java.net.URI(part));
                        part = temp.getAbsolutePath();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }

                // find classes
                ArrayList<File> files = new ArrayList<File>();
                File dir = new File(part);
                if (dir.isDirectory()) {
                    File root = new File(dir.toString() + File.separatorChar + packageDirName);
                    String[] names = findClassesInDirectoryRecursive(root, "");
                    classNames.addAll(Arrays.asList(names));
                } else {
                    try {
                        JarFile jar = new JarFile(part);
                        Enumeration<JarEntry> jarEntries = jar.entries();
                        while (jarEntries.hasMoreElements()) {
                            String jarEntry = jarEntries.nextElement().getName();
                            if (jarEntry.startsWith(packageJarName)) {
                                String relativeName = jarEntry.substring(packageJarName.length());
                                if (relativeName.endsWith(".class")) {
                                    relativeName = relativeName.replace('/',
                                            '.');
                                    classNames.add(relativeName.substring(0,
                                            relativeName.length()
                                            - ".class".length()));
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // ignore unreadable files
                    }
                }
            }

            /*while (pathTokens.hasMoreElements()) {
            String pathToSearch = pathTokens.nextElement().toString();
            if (pathToSearch.endsWith(".jar")) {
            try {
            JarFile jar = new JarFile(pathToSearch);
            Enumeration<JarEntry> jarEntries = jar.entries();
            while (jarEntries.hasMoreElements()) {
            String jarEntry = jarEntries.nextElement()
            .getName();
            if (jarEntry.startsWith(packageJarName)) {
            String relativeName = jarEntry
            .substring(packageJarName.length());
            if (relativeName.endsWith(".class")) {
            relativeName = relativeName.replace('/',
            '.');
            classNames.add(relativeName.substring(0,
            relativeName.length()
            - ".class".length()));
            }
            }
            }
            } catch (IOException ignored) {
            // ignore unreadable files
            }
            } else {
            File root = new File(pathToSearch + File.separatorChar
            + packageDirName);
            String[] names = findClassesInDirectoryRecursive(root, "");
            for (String name : names) {
            classNames.add(name);
            }
            }
            } */
            cached = classNames.toArray(new String[classNames.size()]);
            Arrays.sort(cached);
            cachedClassNames.put(packageNameToSearch, cached);
        }
        return cached;
    }

    protected static String[] findClassesInDirectoryRecursive(File root,
            String packagePath) {
        HashSet<String> classNames = new HashSet<String>();
        if (root.isDirectory()) {
            String[] list = root.list();
            for (String string : list) {
                if (string.endsWith(".class")) {
                    classNames.add(packagePath
                            + string.substring(0, string.length()
                            - ".class".length()));
                } else {
                    File testDir = new File(root.getPath() + File.separatorChar
                            + string);
                    if (testDir.isDirectory()) {
                        String[] names = findClassesInDirectoryRecursive(
                                testDir, packagePath + string + ".");
                        classNames.addAll(Arrays.asList(names));
                    }
                }
            }
        }
        return classNames.toArray(new String[classNames.size()]);
    }

    public static Class[] findClassesOfType(String packageNameToSearch,
            Class<?> typeDesired) {
        ArrayList<Class<?>> classesFound = new ArrayList<Class<?>>();
        String[] classNames = findClassNames(packageNameToSearch);
        for (String className : classNames) {
            String fullName = packageNameToSearch.length() > 0 ? (packageNameToSearch
                    + "." + className)
                    : className;
            if (isPublicConcreteClassOfType(fullName, typeDesired)) {
                try {
                    classesFound.add(Class.forName(fullName));
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
