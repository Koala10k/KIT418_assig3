package edu.utas.kit418.assig3.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import edu.utas.kit418.assig3.common.Logger;
import edu.utas.kit418.assig3.common.Message;

public class ClientService implements Runnable {

	public int id;
	public String clientStatus = "offline";
	// active(unauthorized)
	// active(authorized)
	// dead
	public Socket c;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	public String remoteSocketAddr;
	private String localSocketAddr;
	private boolean authenticated;
	public static Object[] sync = new Object[0];
	private boolean looping = true;
	private String clientId;

	public ClientService(Socket client) {
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
		clientStatus = "active(unauthorized)";
		try {
			authenticate();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			goToDie();
		}
		if (clientStatus.equals("dead")) {
			die();
		}
		clientStatus = "active(authorized)";
		log("ClientId: " + clientId + " has been authenticated");
		Server.startThread(new Thread(new Runnable() { // Receiver
					@Override
					public void run() {
						while (looping) {
							try {
								Message msg = null;
								try {
									msg = (Message) ois.readObject();
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
								if (msg.type == 1) {
									if (msg.expiredIn < 0)
										msg.expiredIn = Double.MAX_VALUE;
									Server.addTaskMsgByPriority(msg);
									synchronized (NodeService.sync) {
										NodeService.sync.notify();
									}
								} else if (msg.type == 5 || msg.type == 9 || msg.type == 11) {
									synchronized (Server.outMsgList) {
										Server.outMsgList.add(msg);
									}
									synchronized (sync) {
										sync.notifyAll();
									}
								} else {
									log("received (Network) an invlid msg " + msg.type);
								}
							} catch (IOException e) {
								goToDie();
							}
						}
						Server.threadExit("ClientService" + id + "'s Receiver");
					}
				}));

		while (looping) {// Sender
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
			if (msg.type == 2) {
				sendMsg(msg);
			} else if (msg.type == 5) {
				Server.getTaskStatus(msg);
				msg.type = 8;
				sendMsg(msg);
			} else if (msg.type == 9) {
				Server.getSysInfo(msg);
				msg.type = 10;
				sendMsg(msg);
			} else if (msg.type == 11) {
				Server.stopTask(msg);
				msg.type = 12;
				sendMsg(msg);
			} else {
				log("received (Queue) an invlid msg " + msg.type);
			}
		}
		die();
	}

	private void die() {
		long sleepTime = (long) (1000 + Math.random() * 4000);
		log("will sleep for " + sleepTime + "ms");
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Server.threadExit("ClientService" + id + "'s Sender");
	}

	private Message seekMyMsg() {
		synchronized (Server.outMsgList) {
			for (Message msg : Server.outMsgList) {
				if (msg.owner.equals(clientId)) {
					Server.outMsgList.remove(msg);
					return msg;
				}
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

	private void authenticate() throws ClassNotFoundException, IOException {
		Message msg = new Message();
		msg.type = 6;
		sendMsg(msg);
		while (looping) {
			msg = (Message) ois.readObject();
			if (msg.type == 7 && msg.content.equals("admin/admin")) {
				msg.content = "Success";
				clientId = msg.answer;
				authenticated = true;
			} else {
				msg.content = "Failed";
				authenticated = false;
			}
			msg.type = 13;
			sendMsg(msg);
			if (authenticated)
				break;
		}
	}

	private void goToDie() {
		log("Client " + remoteSocketAddr + " has been disconnected");
		looping = false;
		synchronized (sync) {
			sync.notifyAll();
		}
		clientStatus = "dead";
		Server.removeClient(this);
		clearClientTasks();
	}

	private void clearClientTasks() {
		List<Message> tmp = new ArrayList<Message>();
		synchronized (Server.inMsgList) {
			for (Message msg : Server.inMsgList) {
				if (msg.type == 1 && msg.owner.equals(clientId)) {
					tmp.add(msg);
					log("will rmeove Client(" + clientId + ")'s waiting Task(" + msg.msgID + ")");
				}
			}
			Server.inMsgList.removeAll(tmp);
			Server.removeTasks(tmp.size());
			log("removed " + tmp.size() + " waiting tasks");
		}
		tmp.clear();
		synchronized (Server.outMsgList) {
			for (Message msg : Server.outMsgList) {
				if (msg.type == 2 && msg.owner.equals(clientId)) {
					tmp.add(msg);
					log("will rmeove Client(" + clientId + ")'s resulting Task(" + msg.msgID + ")");
				}
			}
			Server.outMsgList.removeAll(tmp);
			log("removed " + tmp.size() + " resulting tasks");
		}
		tmp.clear();
		synchronized (Server.runningTaskList) {
			for (Message msg : Server.runningTaskList) {
				if (msg.type == 1 && msg.owner.equals(clientId)) {
					tmp.add(msg);
					log("will remove Client(" + clientId + ")'s running Task(" + msg.msgID + ")");
				}
			}
			Server.runningTaskList.removeAll(tmp);
			log("removed " + tmp.size() + " running tasks");
		}
	}

	private void log(String msg) {
		Logger.log("ClientService" + id + ": " + msg);
	}
}
