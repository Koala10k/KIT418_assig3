package edu.utas.kit418.assig3.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import edu.utas.kit418.assig3.common.Message;
import edu.utas.kit418.assig3.common.NodeInfo;

public class NodeService implements Runnable {

	public int id;
	public String nodeStatus = "offline"; // active(#/total), running, dead
	public NodeInfo info;
	public Socket c;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private String remoteSocketAddr;
	private String localSocketAddr;
	public int idleNodeThread;
	private List<Message> workingList = new ArrayList<Message>();
	private boolean looping = true;
	public int presIdx; // 1~100 higher value higher pressure

	public static Object[] sync = new Object[0];

	public NodeService(Socket client) {
		c = client;
		getAddr();
		try {
			oos = new ObjectOutputStream(c.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(c.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getAddr() {
		remoteSocketAddr = c.getRemoteSocketAddress().toString();
		localSocketAddr = c.getLocalSocketAddress().toString();
		log("Remote addr: " + remoteSocketAddr);
		log("Local addr: " + localSocketAddr);
	}

	@Override
	public void run() {
		Server.startThread(new Thread(new Runnable() { // Receiver
					@Override
					public void run() {
						while (looping) {
							try {
								Message msg = (Message) ois.readObject();
								if (msg.type == 2) {
									synchronized (Server.runningTaskList) {
										Server.runningTaskList.remove(msg);
									}
									synchronized (workingList) {
										workingList.remove(msg);
									}
									synchronized (Server.outMsgList) {
										Server.outMsgList.add(msg);
									}
									synchronized (ClientService.sync) {
										ClientService.sync.notifyAll();
									}
								} else if (msg.type == 3) {
									synchronized (Server.inMsgList) {
										Server.inMsgList.add(msg);
									}
									synchronized (sync) {
										sync.notifyAll();
									}
								} else if (msg.type == 4 || msg.type == 0) {
									synchronized (Server.inMsgList) {
										Server.inMsgList.add(0, msg);
									}
									synchronized (sync) {
										sync.notifyAll();
									}
								} else {
									log("received (Network) an invlid msg " + msg.type);
								}
							} catch (IOException e) {
								goToDie();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
						Server.threadExit("NodeService" + id + "'s Receiver");
					}

				}));

		while (looping) { // Sender
			Message msg = seekMyMsg();
			if (msg == null) {
				try {
					synchronized (sync) {
						sync.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!looping)
				break;
			if (msg == null)
				continue;
			if (msg.type == 0) {
				nodeStatus = msg.nodeInfo.status;
				info = msg.nodeInfo;
				presIdx = info.presIdx;
				log("PerfInfo: " + msg.nodeInfo.toJsonString());
			} else if (msg.type == 3) {
				nodeStatus = "startup";
			} else if (msg.type == 4) {
				idleNodeThread++;
				log("received NodeReadyToWork idle=" + idleNodeThread);
			} else if (msg.type == 1) {
				assignTask(msg);
			} else {
				log("received (Queue) an invlid msg " + msg.type);
			}
		}
		die();
	}

	private void die() {
		// DEBUG
		long sleepTime = (long) (1000 + Math.random() * 4000);
		log("will sleep for " + sleepTime + "ms");
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Server.threadExit("NodeService" + id + "'s Sender");
	}

	private Message seekMyMsg() {
		// DEBUG
		if (id == 0)
			presIdx = 1000;
		NodeService node = Server.findRelaxingNode(idleNodeThread > 0 ? this : null);
		Message toOtherMsg = null;
		synchronized (Server.inMsgList) {
			for (Message msg : Server.inMsgList) {
				if (msg.type == 1) {
					// DEBUG
					if (node == null) {
						log("all nodes are busy!!!");
						break;
					} else if (node == this) {
						Server.removeATask();
						Server.inMsgList.remove(msg);
						log("assign a task to itself");
						return msg;
					} else {
						Server.removeATask();
						Server.inMsgList.remove(msg);
						log("assign a task to Node" + node.id + "(pressure: " + node.presIdx + ")");
						toOtherMsg = msg;
						break;
					}
				} else if (msg.type != 1 && msg.from.equals(remoteSocketAddr)) {
					Server.inMsgList.remove(msg);
					return msg;
				}
			}
		}
		if (toOtherMsg != null)
			node.assignTask(toOtherMsg);
		return null;
	}

	private void assignTask(Message msg) {
		idleNodeThread--;
		synchronized (Server.runningTaskList) {
			Server.runningTaskList.add(msg);
		}
		synchronized (workingList) {
			workingList.add(msg);
		}
		sendMsg(msg);
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

	private void goToDie() {
		log("Node " + remoteSocketAddr + " has been disconnected");
		rescheduleTask();
		looping = false;
		synchronized (sync) {
			sync.notifyAll();
		}
		nodeStatus = "dead";
		Server.removeNode(this);
	}

	private void rescheduleTask() {
		List<Message> tmp = new ArrayList<Message>();
		synchronized (workingList) {
			for (Message msg : workingList)
				tmp.add(msg);
			workingList.clear();
		}
		if (tmp.size() > 0) {
			for (Message task : tmp) {
				synchronized (Server.runningTaskList) {
					Server.runningTaskList.remove(task);
				}
				Server.addTaskMsgByPriority(task);
				log("task(" + task.msgID + ") has been re-scheduled");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public String jsonWorkingTask() {
		JSONObject json = new JSONObject();
		synchronized (workingList) {
			for (int i = 0; i < workingList.size(); i++) {
				json.put("task" + i, workingList.get(i).msgID);
			}

		}
		return json.toJSONString();
	}

	private void log(String msg) {
		System.out.println("NodeService" + id + ": " + msg);
	}
}