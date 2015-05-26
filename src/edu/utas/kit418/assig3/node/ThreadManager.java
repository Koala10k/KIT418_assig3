package edu.utas.kit418.assig3.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import edu.utas.kit418.assig3.common.Message;

public class ThreadManager {

	private static ThreadManager instance;
	private ThreadWorker[] workers;
	public List<Message> inMsgList = new ArrayList<Message>();
	public List<Message> outMsgList = new ArrayList<Message>();
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	public Object[] senderSync = new Object[0];
	public Object[] workerSync = new Object[0];
	public int runningThreadNum;
	private Socket c;
	private String remoteSocketAddr;
	private String localSocketAddr;

	private ThreadManager(int totalThreads, Socket c) throws Exception {
		System.out.println("Node total worker: " + totalThreads);
		this.c = c;
		getAddr();
		c.getOutputStream().write(2); // 2: Node
		c.getOutputStream().flush();
		oos = new ObjectOutputStream(c.getOutputStream());
		oos.flush();
		ois = new ObjectInputStream(c.getInputStream());

		workers = new ThreadWorker[totalThreads];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new ThreadWorker(this);
		}
	}

	private void getAddr() {
		remoteSocketAddr = c.getRemoteSocketAddress().toString();
		localSocketAddr = c.getLocalSocketAddress().toString();
		System.out.println("Remote addr: " + remoteSocketAddr);
		System.out.println("Local addr: " + localSocketAddr);
	}

	public void start() {
		nodeStartup();
		startWorkers();

		new Thread(new Runnable() { // Sender
					@Override
					public void run() {
						while (Node.looping) {
							Message msg = seekMsg();
							if (msg == null) {
								synchronized (senderSync) {
									try {
										senderSync.wait();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
							if (!Node.looping)
								break;
							if (msg == null)
								continue;
							if (msg.type == 4 || msg.type == 3 || msg.type == 0 || msg.type == 2) {
								if (msg.type == 4)
									System.out.println("Sent msg 4 for " + msg.content);
								sendMsg(msg);
								outMsgList.remove(0);
							} else {
								System.err.println("Node" + localSocketAddr + " received (Queue) an invlid msg " + msg.type);
							}
						}
						Node.threadExit("Node Sender");
					}

					private Message seekMsg() {
						synchronized (outMsgList) {
							for (Message msg : outMsgList) {
								return msg;
							}
						}
						return null;
					}

					private void sendMsg(Message msg) {
						msg.from = localSocketAddr;
						try {
							oos.writeObject(msg);
							oos.flush();
						} catch (IOException e) {
							goToDie();
						}
					}
				}).start();
		while (Node.looping) {// Receiver
			Message msg = null;
			try {
				msg = (Message) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				goToDie();
				break;
			}
			if (msg == null) {
				System.err.println("receive a null msg");
				continue;
			}
			if (msg.type == 1) {
				synchronized (inMsgList) {
					inMsgList.add(msg);
				}
				synchronized (workerSync) {
					workerSync.notifyAll();
				}
			} else {
				System.err.println("Node" + localSocketAddr + " received (Network) an invlid msg " + msg.type);
			}
		}

		try {
			oos.close();
			ois.close();
		} catch (IOException e) {
		}
	}

	private void goToDie() {
		System.out.println("Error: Connection lost");
		Node.looping = false;
	}

	private void nodeStartup() {
		Message msg = new Message();
		msg.type = 3;
		synchronized (outMsgList) {
			outMsgList.add(msg);
		}
		synchronized (senderSync) {
			senderSync.notify();
		}
	}

	private void startWorkers() {
		for (ThreadWorker worker : workers) {
			new Thread(worker).start();
		}
	}

	public String getStatus() {
		return runningThreadNum == workers.length ? "running" : "active(" + (workers.length - runningThreadNum) + "/" + workers.length + ")";
	}

	public static ThreadManager getInstance(int totalWorker, Socket c) throws Exception {
		if (instance == null)
			instance = new ThreadManager(totalWorker, c);
		return instance;
	}
}
