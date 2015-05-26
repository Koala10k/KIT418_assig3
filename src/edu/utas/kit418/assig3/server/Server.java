package edu.utas.kit418.assig3.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONObject;

import edu.utas.kit418.assig3.common.JCloudsNova;
import edu.utas.kit418.assig3.common.Message;

public class Server {
	public static List<Message> runningTaskList = new ArrayList<Message>();
	public static List<Message> inMsgList = new ArrayList<Message>();
	public static List<Message> outMsgList = new ArrayList<Message>();
	private static List<ClientService> clientList = new ArrayList<ClientService>();
	private static List<NodeService> nodeList = new ArrayList<NodeService>();
	private static ServerSocket serverSocket;
	public static int nextClientId, nextNodeId;
	public static int runningThreadNum = 1;
	private static boolean looping = true;
	private static int taskNum;
	private static int startingNodeNum;

	public static void main(String[] args) {
		try {
			serverSocket = new ServerSocket(4444);
			log("IP: " + InetAddress.getLocalHost().getHostAddress() + " Port:" + serverSocket.getLocalPort());
		} catch (BindException b) {
			log("Server could only run at single instance mode");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		startThread(new Thread(new Runnable() {

			Scanner sc = new Scanner(System.in);

			@Override
			public void run() {
				while (looping) {
					String cmd = sc.nextLine();
					if (cmd.equalsIgnoreCase("quit")) {
						log("Server quiting...");
						Server.looping = false;
					}
				}
				dispose();
				threadExit("Scanner");
			}

		}));
		while (looping) {
			try {
				Socket client = serverSocket.accept();
				int id = client.getInputStream().read();
				if (id == 1) { // 1: Client
					ClientService c = new ClientService(client);
					c.id = nextClientId;
					addClient(c);
					startThread(new Thread(c));
				} else if (id == 2) { // 2: Node
					NodeService w = new NodeService(client);
					w.id = nextNodeId++;
					startingNumDown1();
					addNode(w);
					startThread(new Thread(w));
				} else {
					DataOutputStream dos = new DataOutputStream(client.getOutputStream());
					dos.writeUTF("server refused");
					dos.flush();
					client.close();
				}
			} catch (SocketException e) {
			} catch (IOException e) {
			}
		}

		if (runningThreadNum > 1) {
			synchronized (Server.class) {
				try {
					Server.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		threadExit("Main Thread");
		System.exit(0);
	}

	public static void dispose() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (ClientService c : clientList) {
			try {
				c.c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (NodeService n : nodeList) {
			try {
				n.c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized static void threadExit(String name) {
		log(Thread.currentThread().getName() + "(" + name + ")" + " has exited");
		runningThreadNum--;
		if (runningThreadNum == 1) {
			synchronized (Server.class) {
				Server.class.notify();
			}
		}
	}

	private static void log(String msg) {
		System.out.println("Server: " + msg);
	}

	public static void startThread(Thread thread) {
		runningThreadNum++;
		thread.start();
	}

	private synchronized static void addClient(ClientService clientService) {
		clientList.add(clientService);
	}

	private synchronized static void addNode(NodeService nodeService) {
		nodeList.add(nodeService);
	}

	@SuppressWarnings("unchecked")
	public synchronized static void getTaskStatus(Message msg) {
		JSONObject json = new JSONObject();
		synchronized (inMsgList) {
			for (int i = 0; i < inMsgList.size(); i++) {
				Message m = inMsgList.get(i);
				if (m.type == 1 && m.owner.equals(msg.owner)) {
					json.put(m.content + "(" + m.msgID + ")", "waiting");
				}
			}
		}
		synchronized (outMsgList) {
			for (int i = 0; i < outMsgList.size(); i++) {
				Message m = outMsgList.get(i);
				if (m.type == 2 && m.owner.equals(msg.owner)) {
					json.put(m.content + "(" + m.msgID + ")", "ready");
				}
			}
		}
		synchronized (runningTaskList) {
			for (int i = 0; i < runningTaskList.size(); i++) {
				Message m = runningTaskList.get(i);
				if (m.owner.equals(msg.owner)) {
					json.put(m.content + "(" + m.msgID + ")", "running");
				}
			}
		}
		msg.content = json.toJSONString();
	}

	// Earliest Deadline First Algorithm
	public synchronized static void addTaskMsgByPriority(Message msg) {
		taskNum++;
		int pos = 0;
		boolean hit = false;
		double expired = msg.expiredIn;
		synchronized (inMsgList) {
			for (int i = 0; i < inMsgList.size(); i++) {
				Message m = inMsgList.get(i);
				if (m.type == 1) {
					pos = i;
					if (m.expiredIn >= expired) {
						hit = true;
						break;
					}
				}
			}
			if (pos == inMsgList.size() - 1 && !hit)
				pos++;
			inMsgList.add(pos, msg);
		}
		evulatePressure();
	}

	private static void evulatePressure() {
		boolean newNode;
		synchronized (nodeList) {
			newNode = (taskNum > (startingNodeNum + nodeList.size()) * 10);
		}
		if (newNode) {
			log("taskNum:" + taskNum);
			startNewNode();
		}
	}

	private static void startNewNode() {
		startingNodeNum++;
		boolean localNode = true;
		synchronized (nodeList) {
			localNode = (nodeList.size() == 0);
		}
		//DEBUG
		localNode = true;
		if (localNode) {
			log("starting a local node...");
			try {
				new ProcessBuilder("cmd", "/c", "start", "StartNode.cmd").start();
			} catch (IOException e) {
				log("fail to start a local node");
				startingNumDown1();
				localNode = false;
				log("try to start a remote node");
			}
		}
		if (!localNode) {
			log("starting a remote node...");
			try {
				JCloudsNova.createNewNode();
			} catch (IOException e) {
				log("fail to start a remote node");
				startingNumDown1();
			}
		}

	}

	private static void startingNumDown1() {
		if(startingNodeNum > 0)
		startingNodeNum--;
	}

	@SuppressWarnings("unchecked")
	public synchronized static void getSysInfo(Message msg) {
		JSONObject json = new JSONObject();
		json.put("taskNum", taskNum);
		JSONObject jsonTask = new JSONObject();
		synchronized (inMsgList) {
			for (int i = 0; i < inMsgList.size(); i++) {
				Message m = inMsgList.get(i);
				if (m.type == 1) {
					jsonTask.put(m.content + "(" + m.msgID + ")", "waiting");
				}
			}
		}
		synchronized (outMsgList) {
			for (int i = 0; i < outMsgList.size(); i++) {
				Message m = outMsgList.get(i);
				if (m.type == 2) {
					jsonTask.put(m.content + "(" + m.msgID + ")", "ready");
				}
			}
		}
		synchronized (runningTaskList) {
			for (int i = 0; i < runningTaskList.size(); i++) {
				Message m = runningTaskList.get(i);
				if (m.type == 1) {
					jsonTask.put(m.content + "(" + m.msgID + ")", "running");
				}
			}
		}
		json.put("taskInfo", jsonTask.toJSONString());
		JSONObject jsonNode = new JSONObject();
		synchronized (nodeList) {
			for (NodeService node : nodeList) {
				JSONObject jsonN = new JSONObject();
				jsonN.put("status", node.nodeStatus);
				jsonN.put("cpu%", node.info == null ? "Unknown" : node.info.jsonCpuInfo());
				jsonN.put("working on", node.jsonWorkingTask());
				jsonNode.put(node.id, jsonN.toJSONString());
			}
		}
		json.put("nodeInfo", jsonNode.toJSONString());
		JSONObject jsonClient = new JSONObject();
		synchronized (clientList) {
			for (ClientService client : clientList) {
				jsonClient.put(client.remoteSocketAddr, client.clientStatus);
			}
		}
		json.put("clientInfo", jsonClient.toJSONString());
		msg.content = json.toJSONString();
	}

	public synchronized static void removeTask(Message msg) {
		synchronized (runningTaskList) {
			for (Message m : runningTaskList) {
				if (m.msgID.toString().equals(msg.content)) {
					msg.content = "Task(" + msg.content + ")" + " is running, cannot be stopped";
					msg.answer = "fail";
					return;
				}
			}
		}
		synchronized (inMsgList) {
			for (Message m : inMsgList) {
				if (m.msgID.toString().equals(msg.content)) {
					inMsgList.remove(m);
					msg.answer = msg.content;
					msg.content = "Task(" + msg.content + ")" + " has been removed";
					return;
				}
			}
		}
		synchronized (outMsgList) {
			for (Message m : outMsgList) {
				if (m.msgID.toString().equals(msg.content)) {
					msg.content = "Task(" + msg.content + ")" + " has been processed. Waiting to be sent back";
					msg.answer = "fail";
					return;
				}
			}
		}
		msg.answer = "fail";
		msg.content = "Task(" + msg.content + ")" + " is not found";
	}

	public synchronized static void removeClient(ClientService clientService) {
		clientList.remove(clientService);
	}

	public synchronized static void removeNode(NodeService nodeService) {
		nodeList.remove(nodeService);
		evulatePressure();
	}

	public synchronized static void removeATask() {
		taskNum--;
	}

	public synchronized static NodeService findRelaxingNode(NodeService def) {
		NodeService target = def;
		synchronized (nodeList) {
			for (NodeService node : nodeList) {
				if (node.idleNodeThread > 0) {
					if (target == null) {
						target = node;
						continue;
					}
					if (target.presIdx > node.presIdx) {
						target = node;
					}
				}
			}
		}
		return target;
	}

}