package edu.utas.kit418.assig3.node;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;

public class Node {

	private static ThreadManager mgr;
	private static Socket c;
	private static SysMonitor sysMonitor;
	public static Object[] sSync = new Object[0];
	public static boolean looping = true;
	private static int runningThreadNum;

	public static void main(String[] args) throws Exception {
		// ip port
		// DEBUG:
//		args = new String[] { "127.0.0.1", "4444" };
//		args = new String[] { "104.19.134.113", "4444" };
		if (args.length != 2) {
			System.err.println("Invaid args");
			System.exit(1);
		}

		int serverPort = 0;
		try {
			serverPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Invaid args");
			System.exit(1);
		}
		String serverIP = args[0];

		OperatingSystem os = OperatingSystem.getInstance();
		if (os.getName().equals("Win32")) {
			System.load(new File("lib", "sigar-amd64-winnt.dll").getAbsolutePath());
		} else {
			System.load(new File("lib", "libsigar-amd64-linux.so").getAbsolutePath());
		}

		Sigar sigar = new Sigar();
		CpuInfo cpuInfo = sigar.getCpuInfoList()[0];
		int totalCore = cpuInfo.getCoresPerSocket() * cpuInfo.getTotalSockets();
		int totalWorker = totalCore < 2 ? 2 : totalCore;
		//DEBUG 
		totalWorker = 2;
		runningThreadNum = totalWorker + 2;

		try {
			c = new Socket(serverIP, serverPort);
		} catch (IOException e) {
			System.err.println("Cannot connect to " + serverIP + ":" + serverPort);
			System.exit(1);
		}

		mgr = ThreadManager.getInstance(totalWorker, c);
		sysMonitor = SysMonitor.getInstance(mgr, sigar);
		sysMonitor.start();
		mgr.start();

		dispose();
		if (runningThreadNum > 1) {
			synchronized (sSync) {
				sSync.wait();
			}
		}
		threadExit("Node Receiver(MainThread)");
		System.exit(0);
	}

	private static void dispose() {
		if (sysMonitor != null) {
			sysMonitor.stop();
		}
		synchronized (mgr.workerSync) {
			mgr.workerSync.notifyAll();
		}
		synchronized (mgr.senderSync) {
			mgr.senderSync.notify();
		}
		try {
			c.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(String msg) {
		System.out.println(Thread.currentThread().getId() + " " + msg);
	}

	public static synchronized void threadExit(String name) {
		System.out.println(Thread.currentThread().getName() + " has exited");
//		System.out.println(name + " has exited");
		runningThreadNum--;
		if (runningThreadNum == 1) {
			synchronized (Node.sSync) {
				Node.sSync.notify();
			}
		}
	}
}
