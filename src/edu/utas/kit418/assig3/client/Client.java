package edu.utas.kit418.assig3.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.utas.kit418.assig3.common.Message;

public class Client {

	private static String localSocketAddr;
	private static String remoteSocketAddr;
	private static List<Message> msgList = new ArrayList<Message>();
	private static Object[] sync = new Object[0];
	private static Socket s;
	private static ObjectInputStream ois;
	private static ObjectOutputStream oos;
	private static boolean looping = true;
	private static Scanner sc = new Scanner(System.in);
	private static int runningThreadNum = 3;
	private static GUI gui;

	public static void main(String[] args) {
		// ip port username password
		// DEBUG: n1: "144.6.225.0"
//		 args = new String[] { "127.0.0.1", "4444", "admin", "admin" };
		args = new String[] { "104.199.134.113", "4444", "admin", "admin" };

		if (args.length != 4) {
			System.err.println("Invaid args");
			System.exit(1);
		}

		int serverPort = 0;
		try {
			serverPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e3) {
			System.err.println("Invaid args");
			System.exit(1);
		}
		String serverIP = args[0];
		String authStr = args[2] + "/" + args[3];

		try {
			s = new Socket(serverIP, serverPort);
		} catch (IOException e1) {
			System.err.println("Cannot connect to " + serverIP + ":" + serverPort);
			System.exit(1);
		}
		getAddr();
		try {
			s.getOutputStream().write(1);// 1: Client
			s.getOutputStream().flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			oos = new ObjectOutputStream(s.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(s.getInputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		new Thread(new Runnable() {

			@Override
			public void run() { // Network Producer
				while (looping) {
					Message msg = null;
					try {
						msg = (Message) ois.readObject();
					} catch (IOException e) {
						System.out.println("Error: Connection lost");
						looping = false;
						threadExit("Client Network Producer");
						synchronized (sync) {
							sync.notify();
						}
						return;
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					if (msg.type == 2 || msg.type == 6 || msg.type == 7 || msg.type == 8 || msg.type == 10 || msg.type == 12) {
						synchronized (msgList) {
							msgList.add(msg);
						}
						synchronized (sync) {
							sync.notify();
						}
					} else {
						System.err.println("Client received(Network) an invlid msg " + msg.type);
					}
				}
			}
		}).start();

		Thread cmdListner = new Thread(new Runnable() {

			@Override
			public void run() { // Command Producer
				while (looping) {
					String cmd = sc.nextLine();
					if (cmd.length() == 0)
						continue;
					Message msg = new Message();
					String[] cmds = cmd.split(" ");
					if (cmd.equalsIgnoreCase("ts")) {
						msg.type = 5;
					} else if (cmd.equalsIgnoreCase("ss")) {
						msg.type = 9;
					} else if (cmds[0].equalsIgnoreCase("stop") && cmds.length == 2) {
						msg.type = 11;
						msg.content = cmds[1];
					} else {
						msg.type = 1;
						msg.content = cmd;
					}
					msg.owner = localSocketAddr;
					synchronized (msgList) {
						msgList.add(msg);
					}
					synchronized (sync) {
						sync.notify();
					}
				}
			}
		});
		cmdListner.start();

		gui = new GUI();
		gui.setVisible(true);
		while (looping) { // Consumer
			Message msg = seekMsg();
			if (msg == null) {
				synchronized (sync) {
					try {
						sync.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (!looping)
				break;
			if (msg == null)
				continue;
			if (msg.type == 2) {
				printTaskResult(msg);
			} else if (msg.type == 5 || msg.type == 1 || msg.type == 9 || msg.type == 11) {
				sendMsg(msg);
			} else if (msg.type == 8) {
				printTaskStatus(msg.content);
			} else if (msg.type == 6) {
				msg.content = authStr;
				msg.type = 7;
				msg.owner = localSocketAddr;
				sendMsg(msg);
			} else if (msg.type == 7) {
				System.out.println("Login " + msg.content);
			} else if (msg.type == 10) {
				printSysInfo(msg.content);
			} else if (msg.type == 12) {
				printStopTaskInfo(msg);
			} else {
				System.err.println("Client received(Queue) an invlid msg " + msg.type);
			}
		}

		try {
			// sc.close();
			ois.close();
			oos.close();
			s.close();
		} catch (IOException e) {
		}
		if (runningThreadNum != 2) {
			synchronized (Client.class) {
				try {
					Client.class.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		threadExit("Client Consumer(Main Thread)");
		System.exit(0);
	}

	private static Message seekMsg() {
		synchronized (msgList) {
			for (Message msg : msgList) {
				msgList.remove(msg);
				return msg;
			}
		}
		return null;
	}

	private static void getAddr() {
		localSocketAddr = s.getLocalSocketAddress().toString();
		remoteSocketAddr = s.getRemoteSocketAddress().toString();
		System.out.println("Remote addr: " + remoteSocketAddr);
		System.out.println("My Socket Addr: " + localSocketAddr);
	}

	public static synchronized void threadExit(String name) {
		System.out.println(Thread.currentThread().getName() + " has exited");
		// System.out.println(name + " has exited");
		runningThreadNum--;
		if (runningThreadNum == 2) { // Main and Sc
			synchronized (Client.class) {
				Client.class.notify();
			}
		}
	}

	private static void printStopTaskInfo(Message msg) {
		System.out.println(msg.answer);
		System.out.println(msg.content);
		gui.backCancelTaskResult(msg);
	}

	private static void printSysInfo(String json) {
		String msg = formatSysInfo(json);
		gui.backSysInfo(msg);
	}

	private static String formatSysInfo(String json) {
		StringBuilder sb = new StringBuilder();
		JSONObject obj = (JSONObject) JSONValue.parse(json);
		sb.append("----[waiting task num]----");
		sb.append("\n");
		sb.append(obj.get("taskNum"));
		sb.append("\n");
		sb.append("----[task status]----");
		sb.append("\n");
		sb.append(formatTaskStatus(obj.get("taskInfo").toString()));
		sb.append("\n");
		sb.append("\n");
		sb.append("----[node status]----");
		sb.append("\n");
		JSONObject objNode = (JSONObject) JSONValue.parse(obj.get("nodeInfo").toString());
		for (Object node : objNode.keySet()) {
			sb.append("--Node" + node.toString() + "--");
			sb.append("\n");
			JSONObject objNode1 = (JSONObject) JSONValue.parse(objNode.get(node).toString());
			for (Object node1 : objNode1.keySet()) {
				sb.append(node1.toString() + ": " + objNode1.get(node1).toString());
				sb.append("\n");
			}
		}
		sb.append("\n");
		sb.append("----[client status]----");
		sb.append("\n");
		JSONObject objClient = (JSONObject) JSONValue.parse(obj.get("clientInfo").toString());
		for (Object client : objClient.keySet()) {
			sb.append(client.toString() + ": " + objClient.get(client));
			sb.append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}

	private static void sendMsg(Message msg) {
		msg.from = localSocketAddr;
		try {
			oos.writeObject(msg);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printTaskStatus(String json) {
		String msg = formatTaskStatus(json);
		gui.backTaskStatus(msg);
		System.out.println(msg);
		
	}

	private static String formatTaskStatus(String json) {
		StringBuilder sb = new StringBuilder();
		JSONObject obj = (JSONObject) JSONValue.parse(json);
		List<String> waitingLst = new ArrayList<String>();
		List<String> runningLst = new ArrayList<String>();
		List<String> resultingLst = new ArrayList<String>();

		for (Object key : obj.keySet()) {
			String status = ((String) obj.get(key));
			if (status.equals("waiting")) {
				waitingLst.add((String) key);
			} else if (status.equals("running")) {
				runningLst.add((String) key);
			} else if (status.equals("resulting")) {
				resultingLst.add((String) key);
			}
		}
		sb.append("--running tasks--");
		sb.append("\n");
		for (String str : runningLst) {
			sb.append(str);
			sb.append("\n");
		}
		sb.append("\n");
		sb.append("--waiting tasks--");
		sb.append("\n");
		for (String str : waitingLst) {
			sb.append(str);
			sb.append("\n");
		}
		sb.append("\n");
		sb.append("--resulting tasks--");
		sb.append("\n");
		for (String str : resultingLst) {
			sb.append(str);
			sb.append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}

	private static void printTaskResult(Message msg) {
		gui.backTaskResult(msg);
		System.out.println("Client received a task result: " + msg.content + ": " + msg.answer);
	}

	public static Message addTaskMsg(String cmd, String timeLimit) {
		double timel;
		try {
			timel = Double.parseDouble(timeLimit);
		} catch (NumberFormatException e) {
			timel = Double.MAX_VALUE;
		}
		Message msg = new Message();
		msg.type = 1;
		msg.content = cmd;
		msg.expiredIn = timel;
		msg.owner = localSocketAddr;

		synchronized (msgList) {
			msgList.add(msg);
		}
		synchronized (sync) {
			sync.notify();
		}
		return msg;
	}

	public static void addStopTaskMsg(Message m) {
		Message msg = new Message();
		msg.type = 11;
		msg.content = m.msgID.toString();
		msg.owner = localSocketAddr;

		synchronized (msgList) {
			msgList.add(msg);
		}
		synchronized (sync) {
			sync.notify();
		}
	}

	public static void addTaskStatusMsg() {
		Message msg = new Message();
		msg.type = 5;
		msg.owner = localSocketAddr;

		synchronized (msgList) {
			msgList.add(msg);
		}
		synchronized (sync) {
			sync.notify();
		}
	}

	public static void addSysStatusMsg() {
		Message msg = new Message();
		msg.type = 9;
		msg.owner = localSocketAddr;

		synchronized (msgList) {
			msgList.add(msg);
		}
		synchronized (sync) {
			sync.notify();
		}
	}
}