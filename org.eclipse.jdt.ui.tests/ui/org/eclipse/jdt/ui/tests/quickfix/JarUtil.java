/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nina Rinskaya
 *     		Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=172820.
 *     Stephan Herrmann - Contribution for
 *								[null] "Annotate" proposals for adding external null annotations to library classes - https://bugs.eclipse.org/458200
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

/**
 * This is a reduced and marginally adjusted copy from org.eclipse.jdt.core.tests.util.Util
 */
public class JarUtil {

	// Trace for delete operation
    /*
     * Maximum time wasted repeating delete operations while running JDT/Core tests.
     */
    private static int DELETE_MAX_TIME = 0;
    /**
     * Trace deletion operations while running JDT/Core tests.
     */
    public static boolean DELETE_DEBUG = false;
    /**
     * Maximum of time in ms to wait in deletion operation while running JDT/Core tests.
     * Default is 10 seconds. This number cannot exceed 1 minute (ie. 60000).
     * <br>
     * To avoid too many loops while waiting, the ten first ones are done waiting
     * 10ms before repeating, the ten loops after are done waiting 100ms and
     * the other loops are done waiting 1s...
     */
    public static int DELETE_MAX_WAIT = 10000;

    /**
     * Initially, output directory was located in System.getProperty("java.io.tmpdir")+"\comptest".
     * To allow user to run several compiler tests at the same time, main output directory
     * is now located in a sub-directory of "comptest" which name is "run."+<code>System.currentMilliseconds</code>.
     *
     * @see #DELAY_BEFORE_CLEAN_PREVIOUS
     */
    private final static String OUTPUT_DIRECTORY;
    /**
     * Let user specify the delay in hours before output directories are removed from file system
     * while starting a new test run. Default value is 2 hours.
     * <p>
     * Note that this value may be a float and so have time less than one hour.
     * If value is 0 or negative, then all previous run directories will be removed...
     *
     * @see #OUTPUT_DIRECTORY
     */
    private final static String DELAY_BEFORE_CLEAN_PREVIOUS = System.getProperty("delay");
    /*
     * Static initializer to clean directories created while running previous test suites.
     */
    static {
        // Get delay for cleaning sub-directories
        long millisecondsPerHour = 1000L * 3600L;
        long delay = millisecondsPerHour * 2; // default is to keep previous run directories for 2 hours
        try {
            if (DELAY_BEFORE_CLEAN_PREVIOUS != null) {
                float hours = Float.parseFloat(DELAY_BEFORE_CLEAN_PREVIOUS);
                delay = (int) (millisecondsPerHour * hours);
            }
        }
        catch (NumberFormatException nfe) {
            // use default
        }

        // Get output directory root from system properties
        String container = System.getProperty("jdt.test.output_directory");
        if (container == null){
            container = System.getProperty("java.io.tmpdir");
        }
        if (container == null) {
            container = "."; // use current directory
        }

        // Get file for root directory
        if (Character.isLowerCase(container.charAt(0)) && container.charAt(1) == ':') {
            container = Character.toUpperCase(container.charAt(0)) + container.substring(1); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        }
        File dir = new File(new File(container), "comptest");

        // If root directory already exists, clean it
        if (dir.exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
            long now = System.currentTimeMillis();
            if ((now - dir.lastModified()) > delay) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                // remove all directory content
                flushDirectoryContent(dir);
            } else {
                // remove only old sub-dirs
                for (File testDir : dir.listFiles()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP //FLLDCIM: MARK — M31 M3FILE_IO CALL
                    if (testDir.isDirectory()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                        if ((now - testDir.lastModified()) > delay) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                            delete(testDir);
                        }
                    }
                }
            }
        }

        // Computed test run directory name based on current time
        File dateDir = new File(dir, "run."+System.currentTimeMillis()); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        String pathDir = null;
        try {
        	pathDir = dateDir.getCanonicalPath(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
		} catch (IOException e) {
			pathDir = dateDir.getAbsolutePath();
		}
		OUTPUT_DIRECTORY = pathDir;
   }


public static CompilationUnit[] compilationUnits(String[] testFiles) {
    int length = testFiles.length / 2;
    CompilationUnit[] result = new CompilationUnit[length];
    int index = 0;
    for (int i = 0; i < length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        result[i] = new CompilationUnit(testFiles[index + 1].toCharArray(), testFiles[index], null);
        index += 2;
    }
    return result;
}

// should eventually be replaced by use of java.util.function.Predicate<CompilationResult>
public interface ClassFileFilter {
	boolean include(CompilationResult unitResult);
}
/* inlined and simplified / modified for JDT/UI */
private static class Requestor implements ICompilerRequestor {
	public boolean hasErrors = false;
	public String outputPath;
	public String problemLog = "";
	private ClassFileFilter classFileFilter= null;

	public Requestor(ClassFileFilter classFileFilter) {
		if (classFileFilter != null) {
			this.classFileFilter = classFileFilter;
		} else {
			// default: all without errors
			this.classFileFilter = unitResult -> (unitResult != null) && !unitResult.hasErrors();
		}
	}

	@Override
	public void acceptResult(CompilationResult compilationResult) {
		this.hasErrors |= compilationResult.hasErrors();
		this.problemLog += compilationResult.toString();
		outputClassFiles(compilationResult);
	}
	protected void outputClassFiles(CompilationResult unitResult) {
		if (this.classFileFilter.include(unitResult)) {
			if (this.outputPath != null) {
				for (ClassFile classFile : unitResult.getClassFiles()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					String relativeName =
						new String(classFile.fileName()).replace('/', File.separatorChar) + ".class"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
					try {
						org.eclipse.jdt.internal.compiler.util.Util.writeToDisk(true, this.outputPath, relativeName, classFile);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
public static void compile(String[] pathsAndContents, Map<String, String> options, String[] classpath, String outputPath, ClassFileFilter classFileFilter) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());
        Requestor requestor = new Requestor(classFileFilter);
        requestor.outputPath = outputPath.endsWith(File.separator) ? outputPath : outputPath + File.separator; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

        String[] classLibs = getJavaClassLibs();
        if (classpath != null) {
        	int length = classpath.length;
        	int classLibsLength = classLibs.length;
        	System.arraycopy(classpath, 0, classpath = new String[length + classLibsLength], 0, length);
        	System.arraycopy(classLibs, 0, classpath, length, classLibsLength);
        } else {
        	classpath = classLibs;
        }

        INameEnvironment nameEnvironment = new FileSystem(classpath, new String[] {}, null);
        IErrorHandlingPolicy errorHandlingPolicy =
            new IErrorHandlingPolicy() {
                @Override
				public boolean proceedOnErrors() {
                    return true;
                }
                @Override
				public boolean stopOnFirstError() {
                    return false;
                }
				@Override
				public boolean ignoreAllErrors() {
					return false;
				}
            };
        CompilerOptions compilerOptions = new CompilerOptions(options);
        compilerOptions.performMethodsFullRecovery = false;
        compilerOptions.performStatementsRecovery = false;
        Compiler batchCompiler =
            new Compiler(
                nameEnvironment,
                errorHandlingPolicy,
                compilerOptions,
                requestor,
                problemFactory);
        batchCompiler.options.produceReferenceInfo = true;
        batchCompiler.compile(compilationUnits(pathsAndContents)); // compile all files together
        // cleanup
    	nameEnvironment.cleanup();
        if (requestor.hasErrors)
	        System.err.print(requestor.problemLog); // problem log empty if no problems
}
public static void createFile(String path, String contents) throws IOException {
    try (FileOutputStream output = new FileOutputStream(path)) { //FLLDCIM: MARK — M31 M3FILE_IO NEW
        output.write(contents.getBytes());
    }
}
public static void createJar(String[] pathsAndContents, String[] extraPathsAndContents, Map<String, String> options, ClassFileFilter classFileFilter, String[] classpath, String jarPath) throws IOException {
    String classesPath = getOutputDirectory() + File.separator + "classes"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
    File classesDir = new File(classesPath);
    flushDirectoryContent(classesDir);
	if (pathsAndContents != null) {
		compile(pathsAndContents, options, classpath, classesPath, classFileFilter);
	}
	if (extraPathsAndContents != null) {
		for (int i = 0, l = extraPathsAndContents.length; i < l; /* inc in loop */) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			File  outputFile = new File(classesPath, extraPathsAndContents[i++]);
			outputFile.getParentFile().mkdirs(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
			Files.writeString(outputFile.getAbsoluteFile().toPath(), extraPathsAndContents[i++], Charset.defaultCharset());
		}
	}
    zip(classesDir, jarPath);
}
public static void createJar(String[] javaPathsAndContents, String[] extraPathsAndContents, String jarPath, String[] classpath, String compliance, Map<String, String> options, ClassFileFilter classFileFilter) throws IOException {
	Map<String, String> compileOptions = getCompileOptions(compliance);
	if (options != null) {
		compileOptions.putAll(options);
	}
	createJar(javaPathsAndContents, extraPathsAndContents, compileOptions, classFileFilter, classpath, jarPath);
}
public static void createSourceZip(String[] pathsAndContents, String zipPath) throws IOException {
    String sourcesPath = getOutputDirectory() + File.separator + "sources"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
    createSourceDir(pathsAndContents, sourcesPath);
    zip(new File(sourcesPath), zipPath);
}

public static void createSourceDir(String[] pathsAndContents, String sourcesPath) throws IOException {
	flushDirectoryContent(new File(sourcesPath));
    for (int i = 0, length = pathsAndContents.length; i < length; i+=2) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        String sourcePath = sourcesPath + File.separator + pathsAndContents[i]; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        File sourceFile = new File(sourcePath);
        sourceFile.getParentFile().mkdirs(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
        createFile(sourcePath, pathsAndContents[i+1]);
    }
}
/**
 * Delete a file or directory and insure that the file is no longer present
 * on file system. In case of directory, delete all the hierarchy underneath.
 *
 * @param file The file or directory to delete
 * @return true iff the file was really delete, false otherwise
 */
public static boolean delete(File file) {
	// flush all directory content
	if (file.isDirectory()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
		flushDirectoryContent(file);
	}
	// remove file
	file.delete(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
	if (isFileDeleted(file)) {
		return true;
	}
	return waitUntilFileDeleted(file);
}
/**
 * Flush content of a given directory (leaving it empty),
 * no-op if not a directory.
 */
public static void flushDirectoryContent(File dir) {
    File[] files = dir.listFiles(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
    if (files == null) return;
    for (File file : files) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        delete(file);
    }
}
private static Map<String, String> getCompileOptions(String compliance) {
    Map<String, String> options = new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
    options.put(CompilerOptions.OPTION_Compliance, compliance);
    options.put(CompilerOptions.OPTION_Source, compliance);
    options.put(CompilerOptions.OPTION_TargetPlatform, compliance);
    return options;
}
/**
 * Search the user hard-drive for a Java class library.
 * Returns null if none could be found.
*/
public static String[] getJavaClassLibs() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	// check bootclasspath properties for Sun, JRockit and Harmony VMs
	String bootclasspathProperty = System.getProperty("sun.boot.class.path"); //$NON-NLS-1$
	if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
		// IBM J9 VMs
		bootclasspathProperty = System.getProperty("vm.boot.class.path"); //$NON-NLS-1$
		if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
			// Harmony using IBM VME
			bootclasspathProperty = System.getProperty("org.apache.harmony.boot.class.path"); //$NON-NLS-1$
		}
	}
	String[] jars = null;
	if ((bootclasspathProperty != null) && (bootclasspathProperty.length() != 0)) {
		StringTokenizer tokenizer = new StringTokenizer(bootclasspathProperty, File.pathSeparator);
		final int size = tokenizer.countTokens();
		jars = new String[size];
		int i = 0;
		while (tokenizer.hasMoreTokens()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			final String fileName = toNativePath(tokenizer.nextToken());
			if (new File(fileName).exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
				jars[i] = fileName;
				i++;
			}
		}
		if (size != i) {
			// resize
			System.arraycopy(jars, 0, (jars = new String[i]), 0, i);
		}
	} else {
		String jreDir = getJREDirectory();
		final String osName = System.getProperty("os.name");
		if (jreDir == null) {
			return new String[] {};
		}
		if (osName.startsWith("Mac")) {
			return new String[] {
					toNativePath(jreDir + "/../Classes/classes.jar") //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			};
		}
		final String vmName = System.getProperty("java.vm.name");
		if ("J9".equals(vmName)) {
			return new String[] {
					toNativePath(jreDir + "/lib/jclMax/classes.zip") //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			};
		}
		String[] jarsNames = null;
		ArrayList<String> paths = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		if ("DRLVM".equals(vmName)) {
			FilenameFilter jarFilter = (unused, name) -> name.endsWith(".jar") & !name.endsWith("-src.jar");
			jarsNames = new File(jreDir + "/lib/boot/").list(jarFilter); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT //FLLDCIM: MARK — M31 M3FILE_IO CALL
			addJarEntries(jreDir + "/lib/boot/", jarsNames, paths); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		} else {
			jarsNames = new String[] {
					"/lib/vm.jar",
					"/lib/rt.jar",
					"/lib/core.jar",
					"/lib/security.jar",
					"/lib/xml.jar",
					"/lib/graphics.jar"
			};
			addJarEntries(jreDir, jarsNames, paths);
		}
		jars = new String[paths.size()];
		paths.toArray(jars);
	}
	return jars;
}
private static void addJarEntries(String jreDir, String[] jarNames, ArrayList<String> paths) {
	for (String jarName : jarNames) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
		final String currentName = jreDir + jarName; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		File f = new File(currentName);
		if (f.exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
			paths.add(toNativePath(currentName));
		}
	}
}
/**
 * Returns the JRE directory this tests are running on.
 * Returns null if none could be found.
 *
 * Example of use: [org.eclipse.jdt.core.tests.util.Util.getJREDirectory()]
 */
public static String getJREDirectory() {
    return System.getProperty("java.home");
}
/**
 * Search the user hard-drive for a possible output directory.
 * Returns null if none could be found.
 *
 * Example of use: [org.eclipse.jdt.core.tests.util.Util.getOutputDirectory()]
 */
public static String getOutputDirectory() {
    return OUTPUT_DIRECTORY;
}
/**
 * Returns the parent's child file matching the given file or null if not found.
 *
 * @param file The searched file in parent
 * @return The parent's child matching the given file or null if not found.
 */
private static File getParentChildFile(File file) {
    File parent = file.getParentFile();
    if (parent == null || !parent.exists()) return null; //FLLDCIM: MARK — M31 M3FILE_IO CALL
    File[] files = parent.listFiles(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
    int length = files==null ? 0 : files.length;
    if (length > 0) {
        for (int i=0; i<length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
            if (files[i] == file
                    || files[i].equals(file)
                    || files[i].getPath().equals(file.getPath())) {
                return files[i];
            }
        }
    }
    return null;
}
/**
 * Returns the test name from stack elements info.
 *
 * @return The name of the test currently running
 */
private static String getTestName() {
    StackTraceElement[] elements = new Exception().getStackTrace();
    int idx = 0, length=elements.length;
    while (idx<length && !elements[idx++].getClassName().startsWith("org.eclipse.jdt")) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        // loop until JDT/Core class appears in the stack
    }
    if (idx<length) {
        StackTraceElement testElement = null;
        while (idx<length && elements[idx].getClassName().startsWith("org.eclipse.jdt")) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
            testElement = elements[idx++];
        }
        if (testElement != null) {
            return testElement.getClassName() + " - " + testElement.getMethodName(); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        }
    }
    return "?";
}
/**
 * Returns whether a file is really deleted or not.
 * Does not only rely on {@link File#exists()} method but also
 * look if it's not in its parent children {@link #getParentChildFile(File)}.
 *
 * @param file The file to test if deleted
 * @return true if the file does not exist and was not found in its parent children.
 */
public static boolean isFileDeleted(File file) {
    return !file.exists() && getParentChildFile(file) == null; //FLLDCIM: MARK — M31 M3FILE_IO CALL
}
/**
 * Print given file information with specified indentation.
 * These information are:<ul>
 * 	<li>read {@link File#canRead()}</li>
 * 	<li>write {@link File#canWrite()}</li>
 * 	<li>exists {@link File#exists()}</li>
 * 	<li>is file {@link File#isFile()}</li>
 * 	<li>is directory {@link File#isDirectory()}</li>
 * 	<li>is hidden {@link File#isHidden()}</li>
 * </ul>
 * May recurse several level in parents hierarchy.
 * May also display children, but then will not recusre in parent
 * hierarchy to avoid infinite loop...
 *
 * @param file The file to display information
 * @param indent Number of tab to print before the information
 * @param recurse Display also information on <code>recurse</code>th parents in hierarchy.
 * 	If negative then display children information instead.
 */
private static void printFileInfo(File file, int indent, int recurse) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
    String tab = "";
    for (int i=0; i<indent; i++) tab+="\t"; //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
    System.out.print(tab+"- "+file.getName()+" file info: "); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
    String sep = "";
    if (file.canRead()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
        System.out.print("read");
        sep = ", ";
    }
    if (file.canWrite()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
        System.out.print(sep+"write"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        sep = ", ";
    }
    if (file.exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
        System.out.print(sep+"exist"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        sep = ", ";
    }
    if (file.isDirectory()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
        System.out.print(sep+"dir"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        sep = ", ";
    }
    if (file.isFile()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
        System.out.print(sep+"file"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        sep = ", ";
    }
    if (file.isHidden()) {
        System.out.print(sep+"hidden"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        sep = ", ";
    }
    System.out.println();
    File[] files = file.listFiles(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
    int length = files==null ? 0 : files.length;
    if (length > 0) {
        boolean children = recurse < 0;
        System.out.print(tab+"	+ children: "); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        if (children) System.out.println();
        for (int i=0; i<length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
            if (children) { // display children
                printFileInfo(files[i], indent+2, -1);
            } else {
                if (i>0) System.out.print(", ");
                System.out.print(files[i].getName());
                if (files[i].isDirectory()) System.out.print("[dir]"); //FLLDCIM: MARK — M31 M3FILE_IO CALL
                else if (files[i].isFile()) System.out.print("[file]"); //FLLDCIM: MARK — M31 M3FILE_IO CALL
                else System.out.print("[?]");
            }
        }
        if (!children) System.out.println();
    }
    if (recurse > 0) {
        File parent = file.getParentFile();
        if (parent != null) printFileInfo(parent, indent+1, recurse-1);
    }
}
/**
 * Print stack trace with only JDT elements.
 *
 * @param exception Exception of the stack trace. May be null, then a fake exception is used.
 * @param indent Number of tab to display before the stack elements to display.
 */
private static void printJdtStackTrace(Exception exception, int indent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
    String tab = "";
    for (int i=0; i<indent; i++) tab+="\t"; //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
    StackTraceElement[] elements = (exception==null?new Exception():exception).getStackTrace();
    int idx = 0, length=elements.length;
    while (idx<length && !elements[idx++].getClassName().startsWith("org.eclipse.jdt")) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        // loop until JDT/Core class appears in the stack
    }
    if (idx<length) {
        System.out.print(tab+"- stack trace"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        if (exception == null)
            System.out.println(":");
        else
            System.out.println(" for exception "+exception+":"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        while (idx<length && elements[idx].getClassName().startsWith("org.eclipse.jdt")) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
            StackTraceElement testElement = elements[idx++];
            System.out.println(tab+"	-> "+testElement); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        }
    } else {
        exception.printStackTrace(System.out);
    }
}
/**
 * Makes the given path a path using native path separators as returned by File.getPath()
 * and trimming any extra slash.
 */
public static String toNativePath(String path) {
    String nativePath = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    return
        nativePath.endsWith("/") || nativePath.endsWith("\\") ?
            nativePath.substring(0, nativePath.length() - 1) :
            nativePath;
}
public static void waitAtLeast(int time) {
	long start = System.currentTimeMillis();
	do { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	} while ((System.currentTimeMillis() - start) < time);
}

/**
 * Wait until the file is _really_ deleted on file system.
 *
 * @param file Deleted file
 * @return true if the file was finally deleted, false otherwise
 */
private static boolean waitUntilFileDeleted(File file) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
    if (DELETE_DEBUG) {
        System.out.println();
        System.out.println("WARNING in test: "+getTestName()); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        System.out.println("	- problems occured while deleting "+file); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        printJdtStackTrace(null, 1);
        printFileInfo(file.getParentFile(), 1, -1); // display parent with its children
        System.out.print("	- wait for ("+DELETE_MAX_WAIT+"ms max): "); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
    }
    int count = 0;
    int delay = 10; // ms
    int maxRetry = DELETE_MAX_WAIT / delay;
    int time = 0;
    while (count < maxRetry) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
        try {
            count++;
            Thread.sleep(delay);
            time += delay;
            if (time > DELETE_MAX_TIME) DELETE_MAX_TIME = time;
            if (DELETE_DEBUG) System.out.print('.');
            if (file.exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                if (file.delete()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                    // SUCCESS
                    if (DELETE_DEBUG) {
                        System.out.println();
                        System.out.println("	=> file really removed after "+time+"ms (max="+DELETE_MAX_TIME+"ms)"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
                        System.out.println();
                    }
                    return true;
                }
            }
            if (isFileDeleted(file)) {
                // SUCCESS
                if (DELETE_DEBUG) {
                    System.out.println();
                    System.out.println("	=> file disappeared after "+time+"ms (max="+DELETE_MAX_TIME+"ms)"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
                    System.out.println();
                }
                return true;
            }
            // Increment waiting delay exponentially
            if (count >= 10 && delay <= 100) {
                count = 1;
                delay *= 10;
                maxRetry = DELETE_MAX_WAIT / delay;
                if ((DELETE_MAX_WAIT%delay) != 0) {
                    maxRetry++;
                }
            }
        }
        catch (InterruptedException ie) {
            break; // end loop
        }
    }
    if (!DELETE_DEBUG) {
        System.out.println();
        System.out.println("WARNING in test: "+getTestName()); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        System.out.println("	- problems occured while deleting "+file); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
        printJdtStackTrace(null, 1);
        printFileInfo(file.getParentFile(), 1, -1); // display parent with its children
    }
    System.out.println();
    System.out.println("	!!! ERROR: "+file+" was never deleted even after having waited "+DELETE_MAX_TIME+"ms!!!"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
    System.out.println();
    return false;
}
public static void zip(File rootDir, String zipPath) throws IOException {
	File zipFile= new File(zipPath);
	if (zipFile.exists()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
		if (!delete(zipFile))
			throw new IOException("Could not delete " + zipPath); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		// ensure the new zip file has a different timestamp than the previous one
		int timeToWait= 1000; // some platform (like Linux) have a 1s granularity)
		waitAtLeast(timeToWait);
	} else {
		zipFile.getParentFile().mkdirs(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
	}
	try (ZipOutputStream zip= new ZipOutputStream(new FileOutputStream(zipFile))) { //FLLDCIM: MARK — M31 M3FILE_IO NEW
		zip(rootDir, zip, rootDir.getPath().length() + 1); // 1 for last slash
	}
}
private static void zip(File dir, ZipOutputStream zip, int rootPathLength) throws IOException {
    File[] files = dir.listFiles(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
    if (files != null) {
        for (File file : files) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
            if (file.isFile()) { //FLLDCIM: MARK — M31 M3FILE_IO CALL
                String path = file.getPath();
                path = path.substring(rootPathLength);
                ZipEntry entry = new ZipEntry(path.replace('\\', '/'));
                zip.putNextEntry(entry);
                zip.write(org.eclipse.jdt.internal.compiler.util.Util.getFileByteContent(file));
                zip.closeEntry();
            } else {
                zip(file, zip, rootPathLength);
            }
        }
    }
}

	private JarUtil() {
	}
}
