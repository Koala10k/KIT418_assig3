package edu.utas.kit418.assig3.node;

import java.util.Timer;
import java.util.TimerTask;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import edu.utas.kit418.assig3.common.Message;
import edu.utas.kit418.assig3.common.NodeInfo;

public class SysMonitor extends TimerTask {
	// http://www.coderpanda.com/java-socket-programming-transferring-java-object-through-socket-using-udp/
	private Timer timer;
	private Sigar sigar;
	private ThreadManager mgr;
	private static SysMonitor instance;

	private SysMonitor(ThreadManager threadManager, Sigar sigar) {
		mgr = threadManager;
		timer = new Timer();
		this.sigar = sigar;
	}

	public void start() {
		timer.scheduleAtFixedRate(this, 1000, 5000);
	}

	@Override
	public void run() {
		NodeInfo sysInfo = retrieveSysInfo();
		Message msg = new Message();
		msg.type = 0;
		msg.content = "sysinfo";
		msg.nodeInfo = sysInfo;
		synchronized (mgr.outMsgList) {
			mgr.outMsgList.add(msg);
		}
		synchronized (mgr.senderSync) {
			mgr.senderSync.notify();
		}
	}

	private NodeInfo retrieveSysInfo() {
		NodeInfo info = new NodeInfo();
		info.status = mgr.getStatus();
		try {
			Mem mem = sigar.getMem();
			info.memTotal = mem.getTotal();
			info.memPerc = mem.getUsedPercent();
			CpuPerc[] cpuPercs = sigar.getCpuPercList();
			info.cpusPerc = new double[cpuPercs.length];
			for (int i = 0; i < cpuPercs.length; i++) {
				info.cpusPerc[i] = cpuPercs[i].getCombined();
			}
		} catch (SigarException e) {
			e.printStackTrace();
		}

		info.calcPresIndex();
		return info;
	}

	public void stop() {
		timer.cancel();
		System.out.println("Node SysMonitor stopped");
	}

	public static SysMonitor getInstance(ThreadManager mgr, Sigar sigar) {
		if (instance == null)
			instance = new SysMonitor(mgr, sigar);
		return instance;
	}
}
