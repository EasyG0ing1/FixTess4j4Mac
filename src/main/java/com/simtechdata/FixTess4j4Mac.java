package com.simtechdata;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class FixTess4j4Mac {

	private static String tess4jVersion       = "";
	private static String pathToTess4jJarFile = "";

	/**
	 * Use this method to set the full path to your tess4j jar file.
	 * If using Maven, this is not necessary.
	 * @param path - String of full path to jar file
	 */
	public static void setPathToTess4jJarFile(String path) {
		pathToTess4jJarFile = path;
	}

	/**
	 * This patches the tess4j jar file with the proper C library from tesseract
	 */
	public static void addCLibraryToJarFile() {
		try {
			String cLibraryName = getCLibrary().getName();
			Path   tess4jJar    = getTess4jJarPath();
			File   darwinFolder = new File(tess4jJar.toFile().getParent(), "darwin");
			File   scriptFile   = new File(tess4jJar.toFile().getParent(), "update.sh");
			if (scriptFile.exists()) {
				FileUtils.delete(scriptFile);
			}
			String script = getScript(tess4jJar.toFile().getParent(), tess4jJar.toFile().getName(), getCLibrary().getAbsolutePath(), cLibraryName);
			FileUtils.writeStringToFile(scriptFile, script, StandardCharsets.UTF_8);
			if (executeScript(scriptFile)) {
				FileUtils.delete(scriptFile);
				FileUtils.deleteDirectory(darwinFolder);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Use this to see if the tess4j jar file is already patched
	 * @return boolean - true or false
	 */
	public static boolean jarHasCLibrary() {
		Path               jarFilePath  = getTess4jJarPath();
		LinkedList<String> responseList = new LinkedList<>();
		String             command      = "jar tf " + jarFilePath;
		new Thread(new Run(responseList, command)).start();
		long start = Instant.now().toEpochMilli();
		while (!responseList.contains("RunDone!")) {
			sleep(100);
			long end = Instant.now().toEpochMilli();
			if ((end - start) > 6500) {throw new RuntimeException("There was a problem running the command " + command + " FixTess4j4Mac cannot continue");}
		}
		String libPath = "darwin/" + getCLibrary().getName();
		for (String line : responseList) {
			if (line.contains(libPath)) {return true;}
		}
		return false;
	}

	/**
	 * Use this to get the data path for an instantiated tess4j library like this<BR><BR>
	 *     Tesseract tesseract = new Tesseract();<BR>
	 *     tesseract.setDatapath(FixTess4j4Mac.getDataPath());<BR>
	 * @return String - the string of the correct path.
	 */
	public static String getDataPath() {
		return Paths.get(getTesseractFolder(), "share", "tessdata").toString();
	}

	/**
	 * This method will pull down the latest version of tess4j and install it into your
	 * local Maven repository.
	 */
	public static void pullLatestMavenTess4JLibrary() {
		LinkedList<String> responseList = new LinkedList<>();
		String             command      = "mvn dependency:get -DgroupId=net.sourceforge.tess4j -DartifactId=tess4j -Dversion=LATEST";
		new Thread(new Run(responseList, command)).start();
		while (!responseList.contains("RunDone!")) {
			sleep(50);
		}
		boolean success = false;
		for (String line : responseList) {
			if (line.contains("BUILD SUCCESS")) {
				success = true;
				break;
			}
		}
		if (!success) {
			throw new RuntimeException("There was a problem executing the command:\n\n" + command + "\n\nPlease make sure that the mvn command is in your PATH. You can check this by opening a terminal window and typing in:\n\ncd ~ <return>\nmvn <return>\n\n and if anything comes back other than 'command not found' then your environment is set up properly.");
		}
	}

	private static boolean executeScript(File script) {
		if (script.exists()) {
			String             command      = "chmod 777 " + script.getAbsolutePath();
			LinkedList<String> responseList = new LinkedList<>();
			new Thread(new Run(responseList, command)).start();
			long start = Instant.now().toEpochMilli();
			while (!responseList.contains("RunDone!")) {
				sleep(100);
				long end = Instant.now().toEpochMilli();
				if ((end - start) > 6500) {
					throw new RuntimeException("There was a problem running commend:\n\n" + command + "\n\nFixTess4j4Mac cannot continue");
				}
			}
			responseList = new LinkedList<>();
			command      = script.getAbsolutePath();
			new Thread(new Run(responseList, command)).start();
			start = Instant.now().toEpochMilli();
			while (!responseList.contains("RunDone!")) {
				sleep(100);
				long end = Instant.now().toEpochMilli();
				if ((end - start) > 6500) {
					throw new RuntimeException("There was a problem running commend:\n\n" + command + "\n\nFixTess4j4Mac cannot continue");
				}
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
		String base = sb.toString();
		return String.format(base, jarPath, cLibrarySource, jarFile, jarFile, cLibraryName);
	}

	private static String getTesseractFolder() {
		String tessFolder      = "/usr/local/Cellar/tesseract";
		File   tesseractFolder = new File(tessFolder);
		if (!tesseractFolder.exists()) {
			throw new RuntimeException("Tesseract does not exist at\n\n" + tessFolder + "\n\nPlease run:\n\nbrew update\nbrew install tesseract\n\nThen try again.");
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

	private static void getTess4JMavenVersion() {
		if (!tess4jVersion.isEmpty()) {return;}
		int    top       = 0;
		int    mid       = 0;
		int    last      = 0;
		String home      = System.getProperty("user.home");
		String maven     = ".m2/repository/net/sourceforge/tess4j/tess4j";
		File   mFile     = new File(home, maven);
		File[] mavenSubs = mFile.listFiles();
		if (mavenSubs != null) {
			LinkedList<String> subs = new LinkedList<>();
			for (File folder : mavenSubs) {
				if (folder.isDirectory()) {subs.addLast(folder.getName());}
			}
			for (String ver : subs) {
				String[] each    = ver.split("\\.");
				int      thisTop = Integer.parseInt(each[0]);
				if (thisTop > top) {top = thisTop;}
			}
			for (String ver : subs) {
				String[] each    = ver.split("\\.");
				int      thisTop = Integer.parseInt(each[0]);
				int      thisMid = Integer.parseInt(each[1]);
				if (thisTop == top) {
					if (thisMid > mid) {mid = thisMid;}
				}
			}
			for (String ver : subs) {
				String[] each     = ver.split("\\.");
				int      thisTop  = Integer.parseInt(each[0]);
				int      thisMid  = Integer.parseInt(each[1]);
				int      thisLast = Integer.parseInt(each[2]);
				if (thisTop == top && thisMid == mid) {
					if (thisLast > last) {
						last = thisLast;
					}
				}
			}
		}
		tess4jVersion = String.format("%s.%s.%s", top, mid, last);
	}

	private static Path getTess4jJarPath() {
		if (pathToTess4jJarFile.isEmpty()) {
			getTess4JMavenVersion();
			String tess4jJarFilename = "tess4j-" + tess4jVersion + ".jar";
			String home              = System.getProperty("user.home");
			String maven             = ".m2/repository/net/sourceforge/tess4j/tess4j";
			pathToTess4jJarFile = Paths.get(home, maven, tess4jVersion, tess4jJarFilename).toString();
		}
		return Paths.get(pathToTess4jJarFile);
	}

	private static void sleep(long time) {
		try {
			TimeUnit.MILLISECONDS.sleep(time);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
