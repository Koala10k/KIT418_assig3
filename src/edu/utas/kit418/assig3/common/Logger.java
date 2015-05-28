package edu.utas.kit418.assig3.common;

import java.util.Calendar;

public class Logger {

	private static int logInLineLen;
	private static boolean logNeedANewline;

	public static void logInline(String msg) {
		synchronized (Logger.class) {
			System.out.print("\r");
			String log = msg + " --" + Calendar.getInstance().getTime();
			int newlogLen = log.length();
			int diff = newlogLen - logInLineLen;
			System.out.print(log);
			while (diff > 0) {
				System.out.print(" ");
				diff--;
			}
			logInLineLen = newlogLen;
			logNeedANewline = true;
		}
	}

	public synchronized static void log(String msg) {
		synchronized (Logger.class) {
			if (logNeedANewline) {
				System.out.println(" ");
				logNeedANewline = false;
			}
			System.out.println(msg);
		}
	}

	public static void log(String msg, boolean inline) {
		if (inline)
			logInline(msg);
		else
			log(msg);
	}
}
