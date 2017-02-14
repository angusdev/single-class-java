/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Find the class / package in classpath and the directory or jar file of the class file.
 * 
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
public class FindClasses {
    public static boolean DEBUG = false;

    private static final Pattern INNER_CLASS_PATTERN = Pattern.compile("\\$");

    public static final Set<String> BLACKLIST = new HashSet<String>();
    static {
        BLACKLIST.add("oracle.sql.LoadCorejava");
        BLACKLIST.add("oracle.sql.LnxLibServer");
        BLACKLIST.add("oracle.sql.LdxLibServer");
    }

    private static void debug(Object obj) {
        if (DEBUG) {
            System.out.println(obj);
        }
    }

    public interface Predicate {
        boolean evaluate(String classname, String packageName, String baseName);
    }

    public static abstract class ChainPredicate implements Predicate {
        private List<Predicate> chain = new ArrayList<>();

        @Override
        public final boolean evaluate(String classname, String packageName, String baseName) {
            if (!eval(classname, packageName, baseName)) {
                return false;
            }

            if (chain != null) {
                for (Predicate p : chain) {
                    if (!p.evaluate(classname, packageName, baseName)) {
                        return false;
                    }
                }
            }

            return true;
        }

        public final ChainPredicate and(Predicate p) {
            if (p != null) {
                chain.add(p);
            }
            return this;
        }

        public abstract boolean eval(String classname, String packageName, String baseName);
    }

    /**
     * Match the class name with given regular expression.
     */
    public static class ClassnamePredicate extends ChainPredicate {
        private Pattern pattern;
        private boolean includePackage;

        public ClassnamePredicate(String regex) {
            this(regex, false);
        }

        public ClassnamePredicate(String regex, boolean includePackage) {
            this.pattern = Pattern.compile(regex);
            this.includePackage = includePackage;
        }

        @Override
        public boolean eval(final String classname, final String packageName, final String baseName) {
            if (this.includePackage) {
                return pattern.matcher(classname).find();
            }
            else {
                return pattern.matcher(baseName).find();
            }
        }
    }

    /**
     * Include class from a package.
     */
    public static class PackagePredicate extends ChainPredicate {
        private String packagePrefix;
        private boolean includeSubPackage;

        /**
         * Include classes from a package.
         * 
         * @param packagePrefix
         *            e.g. org.apache.commons (no tailing '.') e.g. null (no package class) (ignore
         *            <code>includeSubPackage</code>)
         * @param includeSubPackage
         *            include class from sub package
         */
        public PackagePredicate(String packagePrefix, boolean includeSubPackage) {
            this.packagePrefix = packagePrefix;
            this.includeSubPackage = includeSubPackage;
        }

        @Override
        public boolean eval(final String classname, final String packageName, final String baseName) {
            if (packagePrefix == null) {
                return packageName.length() == 0;
            }
            else {
                if (includeSubPackage) {
                    return packageName.startsWith(packagePrefix);
                }
                else {
                    return packageName.equals(packagePrefix);
                }
            }
        }
    }

    /**
     * An abstract class to evaluate the <code>Class</code> object instead of the classname.
     */
    public static abstract class ClassPredicate extends ChainPredicate {
        @Override
        public boolean eval(final String classname, final String packageName, final String baseName) {
            debug("ClassPredicate evaulate " + classname);
            if (BLACKLIST.contains(classname)) {
                return false;
            }

            boolean result = false;
            try {
                result = eval(Class.forName(classname));
            }
            catch (ClassNotFoundException ex) {
                debug("ClassNotFound " + classname);
            }
            catch (NoClassDefFoundError ex) {
                debug("ClassNotFound " + classname);
            }

            return result;
        }

        public abstract boolean eval(Class<?> clazz);
    }

    /**
     * Include the sub type of the given class.
     */
    public static class SubTypePredicate extends ClassPredicate {
        private String parentClassname;
        private Class<?> parentClass;

        public SubTypePredicate(String parentClassname) {
            this.parentClassname = parentClassname;

            try {
                this.parentClass = Class.forName(parentClassname);
                debug("can load parentClass " + parentClassname);
            }
            catch (ClassNotFoundException ex) {
                debug("cannot load parentClass" + parentClassname);
            }
        }

        @Override
        public boolean eval(Class<?> clazz) {
            if (clazz == null) {
                return false;
            }
            else if (clazz.getName().equals(parentClassname)) {
                return true;
            }
            else if (parentClass != null) {
                return parentClass.isAssignableFrom(clazz);
            }
            else {
                if (eval(clazz.getSuperclass())) {
                    return true;
                }
                for (Class<?> c : clazz.getInterfaces()) {
                    if (eval(c)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private void processDirectory(final Path path, final Set<String[]> result, final Predicate predicate,
            final boolean includeInnerClass, String source) throws IOException {
        debug("processDirectory " + path);

        final String sep = path.getFileSystem().getSeparator();
        final String parentPath = path.toAbsolutePath().toString();
        final String finalSource = source;
        final int pathPrefixLength = parentPath.length() + (parentPath.endsWith(sep) ? 0 : sep.length());
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String filename = file.toAbsolutePath().toString();
                if (filename.toLowerCase().endsWith(".class")) {
                    String classname = filename.substring(pathPrefixLength, filename.length() - 6).replace(sep, ".");
                    String packageName = null;
                    String baseName = null;
                    if (classname != null) {
                        int pos = classname.lastIndexOf(".");
                        if (pos >= 0) {
                            baseName = classname.substring(pos + 1);
                            packageName = classname.substring(0, pos);
                        }
                        else {
                            baseName = classname;
                            packageName = "";
                        }
                    }
                    if (includeInnerClass || !INNER_CLASS_PATTERN.matcher(baseName).find()) {
                        boolean toAdd = false;
                        if (predicate != null) {
                            toAdd = predicate.evaluate(classname, packageName, baseName);
                        }

                        if (toAdd) {
                            debug("add " + classname);
                            String[] r = { classname, finalSource };
                            result.add(r);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processArchive(File file, final Set<String[]> result, final Predicate predicate,
            final boolean includeInnerClass) throws IOException {
        debug("processArchive " + file);
        try {
            Path zipfile = Paths.get(file.getAbsolutePath());
            FileSystem fs = FileSystems.newFileSystem(zipfile, getClass().getClassLoader());
            for (Path p : fs.getRootDirectories()) {
                processDirectory(p, result, predicate, includeInnerClass, file.getAbsolutePath());
            }
        }
        catch (ProviderNotFoundException ex) {
            ex.printStackTrace();

        }
    }

    private void processURL(URL url, Set<String[]> result, Predicate predicate, final boolean scanArchive,
            final boolean scanDirectory, boolean includeInnerClass) throws IOException {
        debug("processURL " + url);

        if (url == null) {
            return;
        }
        if (!"file".equals(url.getProtocol())) {
            // skip non file URL
            return;
        }
        try {
            Path path = Paths.get(url.toURI());
            if (scanDirectory && Files.isDirectory(path)) {
                processDirectory(path, result, predicate, includeInnerClass, path.toString());
            }
            else if (scanArchive && Files.isRegularFile(path, new LinkOption[0])) {
                processArchive(path.toFile(), result, predicate, includeInnerClass);
            }
            else {
                debug("skipping " + url);
            }
        }
        catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

    public Set<String[]> findClasses(Predicate predicate) throws IOException {
        return findClasses(predicate, true, true, false);
    }

    public Set<String[]> findClasses(Predicate predicate, boolean scanArchive, boolean scanDirectory,
            boolean includeInnerClass) throws IOException {
        URL[] urls = null;
        Set<String[]> result = new TreeSet<>(new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return a[0].compareTo(b[0]);
            }
        });
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof URLClassLoader) {
            urls = ((URLClassLoader) cl).getURLs();
        }
        else {
            String classpath = System.getProperty("java.class.path");
            if (classpath != null) {
                String[] splitted = classpath.split(File.pathSeparator);
                urls = new URL[splitted.length];
                for (int i = 0; i < splitted.length; i++) {
                    String s = splitted[i].trim();
                    if (s.length() > 0) {
                        urls[i] = new File(s).toURI().toURL();
                    }
                }
            }
        }

        if (urls != null) {
            for (URL url : urls) {
                processURL(url, result, predicate, scanArchive, scanDirectory, includeInnerClass);
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("SubTypePredicate(\"java.sql.Driver\")");
        for (String[] s : new FindClasses().findClasses(new SubTypePredicate("java.sql.Driver"))) {
            System.out.println(s[0] + " (" + s[1] + ")");
        }
        System.out.println();
        System.out.println("PackagePredicate(\"oracle\", true) and ClassnamePredicate(\"\\\\d+$\")");
        for (String[] s : new FindClasses().findClasses(new PackagePredicate("oracle", true)
                .and(new ClassnamePredicate("\\d+$")))) {
            System.out.println(s[0] + " (" + s[1] + ")");
        }
        System.out.println();
        System.out.println("PackagePredicate(\"org.h2\")");
        for (String[] s : new FindClasses().findClasses(new PackagePredicate("org.h2.tools", true))) {
            System.out.println(s[0] + " (" + s[1] + ")");
        }
        System.out.println();
        System.out.println("PackagePredicate(null)");
        for (String[] s : new FindClasses().findClasses(new PackagePredicate(null, false))) {
            System.out.println(s[0] + " (" + s[1] + ")");
        }
        System.out.println();
        System.out.println("PackagePredicate(foo.bar)");
        for (String[] s : new FindClasses().findClasses(new PackagePredicate("foo.bar", false))) {
            System.out.println(s[0] + " (" + s[1] + ")");
        }
    }
}
