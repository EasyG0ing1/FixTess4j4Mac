package com.simtechdata;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;

class Run implements Runnable {

	private final String[]           args;
	private final Timer              timer = new Timer();
	private final LinkedList<String> responseList;

	public static String findBash() {
		File     shell    = new File("");
		String   paths    = System.getenv("PATH");
		String[] pathList = paths.split(":");
		for (String path : pathList) {
			shell = new File(path, "bash");
			if (shell.exists()) {break;}
			shell = new File(path, "zsh");
			if (shell.exists()) {break;}
		}
		if (!shell.exists()) {
			shell = new File("/usr/local/bin/bash");
		}
		if (shell.exists()) {
			return shell.getAbsolutePath();
		}
		else {
			throw new RuntimeException("Could not find bash or zsh shell. Please make sure the path is in your PATH environment variable.");
		}
	}

	public Run(LinkedList<String> responseList, String command) {
		this.args = new String[3];
		String bash = findBash();
		if (bash.isEmpty()) {
			throw new RuntimeException("Could not find bash or zsh in your path, please make sure that one of those shells are in your PATH");
		}
		args[0] = bash;
		args[1] = "-c";
		args[2] = command;
				  this.responseList = responseList;
	}

	@Override public void run() {
		try {
			ProcessBuilder pb      = new ProcessBuilder(args);
			Process        process = pb.start();
			Scanner        scanner = new Scanner(process.getInputStream());
			while (scanner.hasNext()) {
				responseList.addLast(scanner.nextLine());
			}
			responseList.addLast("RunDone!");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
