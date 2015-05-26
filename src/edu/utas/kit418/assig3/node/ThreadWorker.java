package edu.utas.kit418.assig3.node;

import edu.utas.kit418.assig3.common.Message;

public class ThreadWorker implements Runnable {

	private ThreadManager mgr;

	public ThreadWorker(ThreadManager threadManager) {
		mgr = threadManager;
	}

	@Override
	public void run() {
		boolean sendReady = true;
		while (Node.looping) {
			if(sendReady) workerReady();
			Message msg = seekMyMsg();
			if (msg == null) {
				synchronized (mgr.workerSync) {
					try {
						mgr.workerSync.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (!Node.looping)
				break;
			if (msg == null){
				sendReady = false;
				continue;
			}
			sendReady = true;
			log("is working for task:" + msg.msgID);
			mgr.runningThreadNum++;
			String rst = null;
			switch (msg.type) {
			case 0: // info
				break;
			case 1: // task
				String[] parms = msg.content.split(" ");
				switch (parms[0]) {
				case "sqrt":
					double value;
					try {
						value = Double.parseDouble(parms[1]);
						rst = String.valueOf(Math.sqrt(value));
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException e1) {
						rst = "Wrong Value Format";
					}
					break;
				default:
					rst = "Unknown function";
					break;
				}
				break;
			}
			msg.type = 2;
			msg.answer = rst;

			long sleepTime = (long) (1000 + Math.random() * 4000);
			log("will sleep for " + sleepTime + "ms");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (mgr.outMsgList) {
				mgr.outMsgList.add(msg);
			}
			synchronized (mgr.senderSync) {
				mgr.senderSync.notify();
			}
			mgr.runningThreadNum--;
		}
		Node.threadExit("Node ThreadWorker " + Thread.currentThread().getName());
	}

	private void log(String msg) {
		System.out.println("Worker"+Thread.currentThread().getName()+": "+msg);
	}

	private Message seekMyMsg() {
		synchronized (mgr.inMsgList) {
			for (Message msg : mgr.inMsgList) {
				mgr.inMsgList.remove(msg);
				return msg;
			}
		}
		return null;
	}

	private void workerReady() {
		Message msg = new Message();
		msg.type = 4;
		msg.content = Thread.currentThread().getName();
		synchronized (mgr.outMsgList) {
			mgr.outMsgList.add(msg);
		}
		synchronized (mgr.senderSync) {
			mgr.senderSync.notify();
		}
	}
}
