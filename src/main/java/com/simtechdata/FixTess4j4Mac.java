package com.simtechdata;

import com.simtechdata.process.ProcBuilder;
import com.simtechdata.process.ProcResult;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains methods to manage jar files and run shell commands in relation to Tess4J library.
 * Tess4J is a Java JNA wrapper for Tesseract OCR API.
 * It provides features to install, set datapath, check if jar files contain necessary C libraries, and execute scripts related to Tess4J.
 */

public class FixTess4j4Mac {

    private static final String MAVEN = ".m2/repository/net/sourceforge/tess4j/tess4j";
    private static final String TESS = "/usr/local/Cellar/tesseract";
    private static final Path TESS_PATH = Paths.get(TESS);
    private static final Path MAVEN_PATH = Paths.get(System.getProperty("user.home"), MAVEN);
    private static final String TESS_JAR = "tess4j-%s.jar";

    /**
     * Use this method to check the environment to make sure that mvn is installed and accessible, as well as the jar program and that the Homebrew version of tesseract is installed.
     * @return - String indicating status, and it will specify what needs to be addressed if anything is not right in the environment.
     */
    public static String checkEnvironment() {
        StringBuilder sb = new StringBuilder();
        String tessFolder = "/usr/local/Cellar/tesseract";
        File folder = new File(tessFolder);
        boolean allClear = true;
        boolean tesseractInstalled = false;
        if(folder.exists()) {
            for(File file : folder.listFiles()) {
                if(file.isDirectory() && file.getName().matches("[0-9.]+")) {
                    sb.append("\n - Tesseract appears to be installed via Homebrew and appears to be version: ").append(file.getName()).append("\n");
                    tesseractInstalled = true;
                }
            }
        }
        else {
            sb.append("This machine is missing the Homebrew installed tesseract. Please run `brew update` then `brew install tesseract`").append("\n");
            allClear = false;
        }
        if(!tesseractInstalled) {
            sb.append("\n- Tesseract does not seem to be installed via Homebrew. Please run `brew update` then `brew install tesseract`").append("\n");
        }
        String mvn = "mvn";
        String jar = "jar";
        ProcBuilder pb = new ProcBuilder(mvn).withArg("--version").ignoreExitStatus().withNoTimeout();
        ProcResult result = pb.run();
        if(result.getExitValue() == 0) {
            String text = result.getOutputString();
            Matcher m = Pattern.compile("(Apache Maven)(\\s+)([0-9.]+)").matcher(text);
            if(m.find()) {
                sb.append(" - Maven is installed and reports being version ").append(m.group(3)).append("\n");
            }
            else {
                sb.append(" *** Maven is not installed. *** Please run brew install maven. The output from the mvn command is:").append("\n").append(text).append("\n\n");
                allClear = false;
            }
        }
        else {
            sb.append("*** There was a problem running this command. It returned a non 0 result: ***\n\t").append(pb.getCommandLine()).append("\n\n");
            allClear = false;
        }

        ProcBuilder pb2 = new ProcBuilder(jar).withArg("--help").withNoTimeout().ignoreExitStatus();
        ProcResult result2 = pb2.run();
        if(result.getExitValue() == 0) {
            String text = result2.getOutputString();
            if(text.toLowerCase().contains("usage:")) {
                sb.append(" - The jar program exists.").append("\n");
            }
            else {
                sb.append(" ** The jar program does not exist. Please install the JDK or JRE and try again.").append("\n");
                allClear = false;
            }
        }
        else {
            sb.append(" ** There was a problem running this command. It returned a non 0 result:\n\n\t").append(pb2.getCommandLine()).append("\n\n");
            allClear = false;
        }
        if(allClear) {
            sb.append("*".repeat(69)).append("\n* Your environment is setup correctly to patch the Tess4J Jar file. *\n").append("*".repeat(69)).append("\nOK:TRUE");
        }
        else {
            sb.append("\n *** Your environment is NOT OK and you cannot patch the Tess4J jar file until you address the issues mentioned above. ***\nOK:FALSE");
        }
        return sb.toString();
    }

    /**
     * This method patches the Tess4J jar file with a specific C library from Tesseract.
     * This is to ensure the required C libraries for Tess4J functionality are present in the jar file.
     *
     * Note: This method might throw a RuntimeException if the IO operations fail. Always wrap the invocation of this method within a try-catch block.
     * @return - true if the patch ran successfully, false if it failed.
     */
    public static boolean addCLibraryToJarFile() throws IOException {
        Path tess4jJar = getTess4jJarPath();
        return patchJar(tess4jJar);
    }

    /**
     * This is a relay method for addCLibraryToJarFile() because the name makes more sense,
     * and we need to maintain backward library compatibility.
     * @return - boolean - true if it succeeded, false if it did not
     */
    public static boolean patchJar() throws IOException {
        return addCLibraryToJarFile();
    }

    /**
     * This method patches the Tess4J jar file at the specific maven path that you pass in as argument.
     * You can get a list of maven paths (versions of Tess4J) by calling the getMavenVersions() method
     * which will return a list of paths. Simply passing one of those paths to this method will get that
     * Tess4J jar file version patched.
     *
     * Note: This method might throw a RuntimeException if the IO operations fail. Always wrap the invocation of this method within a try-catch block.
     * @return - true if the patch ran successfully, false if it failed.
     */
    public static boolean patchJar(Path mavenPath) throws IOException {
        String cLibraryName = getCLibrary().getName();
        String jarVersion = mavenPath.toFile().getName();
        String jarFile = String.format(TESS_JAR, jarVersion);
        if(mavenPath.toFile().isFile()) {
            jarFile = mavenPath.toFile().getName();
            mavenPath = mavenPath.getParent();
        }
        Path jarFilePath = mavenPath.resolve(jarFile);
        File darwinFolder = new File(jarFilePath.toFile().getParent(), "darwin");
        File scriptFile = new File(jarFilePath.toFile().getParent(), "update.sh");
        if (scriptFile.exists()) {
            FileUtils.delete(scriptFile);
        }
        String script = getScript(jarFilePath.toFile().getParent(), jarFilePath.toFile().getName(), getCLibrary().getAbsolutePath(), cLibraryName);
        FileUtils.writeStringToFile(scriptFile, script, Charset.defaultCharset());
        if (executeScript(scriptFile)) {
            FileUtils.delete(scriptFile);
            FileUtils.deleteDirectory(darwinFolder);
            return true;
        }
        return false;
    }

    /**
     * Use this to see if the tess4j jar file is already patched
     *
     * @return boolean -  Returns 'true' if the jar file contains the C library, 'false' otherwise.
     */
    public static boolean jarHasCLibrary() {
        Path jarFilePath = getTess4jJarPath();
        String command = "jar";
        String[] args = {"tf", jarFilePath.toAbsolutePath().toString()};
        ProcBuilder pb = new ProcBuilder(command).withArgs(args).ignoreExitStatus().withNoTimeout();
        ProcResult result = pb.run();
        if (result.getExitValue() != 0) {
            throw new RuntimeException("There was a problem running the command " + pb.getCommandLine() + " FixTess4j4Mac cannot continue");
        }
        String libPath = "darwin/" + getCLibrary().getName();
        for (String line : result.getOutputString().split("\\n")) {
            if (line.contains(libPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use this to get the data path for an instantiated tess4j library like this<BR><BR>
     * Tesseract tesseract = new Tesseract();<BR>
     * tesseract.setDatapath(FixTess4j4Mac.getDataPath());<BR>
     *
     * @return String - the string of the correct path.
     */
    public static String getDataPath() {
        return Paths.get(getTesseractFolder(), "share", "tessdata").toString();
    }

    /**
     * This method will pull down the latest version of Tess4J and install it into your
     * local Maven repository. This works by running a mvn command that retrieves the latest Tess4J artifact.
     *
     * Note: This method may throw a RuntimeException if there's an issue executing the mvn command or if the command does not complete successfully. Always ensure your environment is correctly set up for Maven.
     */
    public static void pullLatestMavenTess4JLibrary() {
        String command = "mvn";
        String[] args = {"dependency:get", "-DgroupId=net.sourceforge.tess4j", "-DartifactId=tess4j", "-Dversion=LATEST"};
        ProcBuilder pb = new ProcBuilder(command).withArgs(args).withNoTimeout().ignoreExitStatus();
        ProcResult result = pb.run();
        boolean procSuccess = result.getExitValue() == 0;
        boolean buildSuccess = false;
        if (procSuccess) {
            for (String line : result.getOutputString().split("\\n")) {
                if (line.contains("BUILD SUCCESS")) {
                    buildSuccess = true;
                    break;
                }
            }
        }
        if (!procSuccess) {
            throw new RuntimeException("There was a problem executing the command:\n\n" + pb.getCommandLine() + "\n\nPlease make sure that the mvn command is in your PATH. You can check this by opening a terminal window and typing in:\n\ncd ~ <return>\nmvn <return>\n\n and if anything comes back other than 'command not found' then your environment is set up properly.\n\nHere is the output from the command:\n\n" + result.getErrorString());
        }
        if (!buildSuccess) {
            throw new RuntimeException("The process ran successfully, but did not detect that maven downloaded the update successfully. Here is the output from the update command: \n\n" + result.getOutputString());
        }
    }

    /**
     * This method gives you a list of Path objects which are the subfolders in the local maven repo. Each folder is a different version of the tess4j library.
     * @return - LinkedList containing Paths
     */
    public static LinkedList<Path> getMavenVersions() {
        LinkedList<Path> list = new LinkedList<>();
        File[] files = MAVEN_PATH.toFile().listFiles();
        if (files != null) {
            for(File file : files) {
                if(file.isDirectory())
                    list.addLast(file.toPath());
            }
        }
        return list;
    }

    private static boolean executeScript(File script) {
        if (script.exists()) {
            String command = "chmod";
            String[] args = {"777", script.getAbsolutePath()};
            if(!script.exists()) {
                throw new RuntimeException("The patch script file does not exist, cannot complete task.");
            }
            ProcBuilder pb1 = new ProcBuilder(command).withArgs(args).ignoreExitStatus().withTimeoutMillis(7000);
            ProcResult result1 = pb1.run(); //run chmod
            if (result1.getExitValue() != 0) {
                throw new RuntimeException("There was a problem running commend:\n\n" + pb1.getCommandLine() + "\n\nFixTess4j4Mac cannot continue");
            }
            ProcBuilder pb2 = new ProcBuilder(script.getAbsolutePath()).withTimeoutMillis(7000).ignoreExitStatus();
            ProcResult result2 = pb2.run(); // run script
            if (result2.getExitValue() != 0) {
                throw new RuntimeException("There was a problem running commend:\n\n" + pb2.getCommandLine() + "\n\nFixTess4j4Mac cannot continue");
            }
        }
        return true;
    }

    private static String getScript(String jarPath, String jarFile, String cLibrarySource, String cLibraryName) {
        StringBuilder sb = new StringBuilder();
        sb.append("cd ").append(jarPath).append("\n");
        sb.append("mkdir darwin\n");
        sb.append("cp ").append(cLibrarySource).append(" darwin").append("\n");
        sb.append("jar uf ").append(jarFile).append(" darwin").append("\n");
        sb.append("jar uf ").append(jarFile).append(" darwin/").append(cLibraryName).append("\n");
        return sb.toString();
    }

    private static String getTesseractFolder() {
        File tesseractFolder = TESS_PATH.toFile();
        if (!tesseractFolder.exists()) {
            throw new RuntimeException("Tesseract does not exist at\n\n" + TESS_PATH + "\n\nPlease run:\n\nbrew update\nbrew install tesseract\n\nThen try again.");
        }
        File[] list = tesseractFolder.listFiles();
        if (list != null) {
            if (list.length > 1) {
                throw new RuntimeException("You have more than one version of Tesseract installed on your system. Please run\n\nbrew uninstall tesseract\nbrew update\nbrew install tesseract\n\nto resolve the issue and get the latest version of tesseract, then try again.");
            }
            return list[0].getAbsolutePath();
        }
        return "";
    }

    private static File getCLibrary() {
        return Paths.get(getTesseractFolder(), "lib", "libtesseract.dylib").toFile();
    }

    private static String getTess4JMavenVersion() {
        int top = 0;
        int mid = 0;
        int last = 0;
        String home = System.getProperty("user.home");
        String maven = ".m2/repository/net/sourceforge/tess4j/tess4j";
        File mFile = new File(home, maven);
        File[] mavenSubs = mFile.listFiles();
        if (mavenSubs != null) {
            LinkedList<String> subs = new LinkedList<>();
            for (File folder : mavenSubs) {
                if (folder.isDirectory()) {
                    subs.addLast(folder.getName());
                }
            }
            for (String ver : subs) {
                String[] each = ver.split("\\.");
                int thisTop = Integer.parseInt(each[0]);
                if (thisTop > top) {
                    top = thisTop;
                }
            }
            for (String ver : subs) {
                String[] each = ver.split("\\.");
                int thisTop = Integer.parseInt(each[0]);
                int thisMid = Integer.parseInt(each[1]);
                if (thisTop == top) {
                    if (thisMid > mid) {
                        mid = thisMid;
                    }
                }
            }
            for (String ver : subs) {
                String[] each = ver.split("\\.");
                int thisTop = Integer.parseInt(each[0]);
                int thisMid = Integer.parseInt(each[1]);
                int thisLast = Integer.parseInt(each[2]);
                if (thisTop == top && thisMid == mid) {
                    if (thisLast > last) {
                        last = thisLast;
                    }
                }
            }
        }
        return String.format("%s.%s.%s", top, mid, last);
    }

    private static Path getTess4jJarPath() {
        String tess4JVersion = getTess4JMavenVersion();
        String tess4jJarFilename = String.format(TESS_JAR, tess4JVersion);
        Path filePath = Paths.get(tess4JVersion, tess4jJarFilename);
        return MAVEN_PATH.resolve(filePath);
    }

    private static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
