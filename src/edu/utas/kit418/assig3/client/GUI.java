package edu.utas.kit418.assig3.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import edu.utas.kit418.assig3.common.Message;

public class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea logPane;
	private JScrollPane jScrollLog;
	private JPanel controlPane;
	private JPanel leftPane;
	private JButton jBtnClearLog;
	private JButton jBtnCancel;
	private JButton jBtnTs;
	private JTextField jTxtCmd;
	private JButton jBtnSs;
	private JList<Message> jLstTask;
	private JComponent jScrollTask;
	private DefaultListModel<Message> lstModel = new DefaultListModel<Message>();
	private JTextField jTxtTimeExpired;

	public GUI() {
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setTitle("Cluster Computing GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage("res" + File.separatorChar + "icon.png"));
		createComponents();
		performLayout();
	}

	private void createComponents() {

		logPane = new JTextArea();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		logPane.setLineWrap(false);
		logPane.setWrapStyleWord(true);
		logPane.setEditable(false);
		jLstTask = new JList<Message>();
		jLstTask.setModel(lstModel);
		jLstTask.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		jScrollLog = new JScrollPane(logPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollLog.setBorder(BorderFactory.createTitledBorder("Log"));
		jScrollTask = new JScrollPane(jLstTask, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollTask.setBorder(BorderFactory.createTitledBorder("Task List"));

		controlPane = new JPanel();
		controlPane.setBorder(BorderFactory.createTitledBorder("Options"));
		leftPane = new JPanel();
		jBtnClearLog = new JButton("Clear Log");
		jBtnCancel = new JButton("Cancel");
		jBtnTs = new JButton("Task Status");
		jBtnSs = new JButton("Server Info");
		jTxtCmd = new JTextField();
		jTxtCmd.setToolTipText("input command here");
		jTxtTimeExpired = new JTextField();
		jTxtTimeExpired.setToolTipText("set time limit (s)");

		jTxtCmd.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (e.equals(""))
					return;
				Message msg = Client.addTaskMsg(cmd, jTxtTimeExpired.getText());
				addTaskGUI(msg);
				jTxtCmd.setText("");
				log("sent a task: " + cmd);
			}
		});

		jBtnClearLog.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logPane.setText("");
			}
		});

		jBtnCancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selectedIdx = jLstTask.getSelectedIndices();
				if (selectedIdx.length == 0) {
					log("select one or muptile item");
				} else {
					for (int idx : selectedIdx) {
						Message msg = lstModel.getElementAt(idx);
						Client.addStopTaskMsg(msg);
						log("A cancel request for task" + msg.content + " has been sent");
					}
				}
			}
		});

		jBtnTs.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Client.addTaskStatusMsg();
			}
		});

		jBtnSs.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Client.addSysStatusMsg();
			}
		});

	}

	private void performLayout() {
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int) screenSize.getWidth() / 4, (int) screenSize.getHeight() / 4);

		GroupLayout optLayout = new GroupLayout(controlPane);
		controlPane.setLayout(optLayout);
		optLayout.setAutoCreateContainerGaps(false);
		optLayout.setAutoCreateGaps(false);

		optLayout.setHorizontalGroup(optLayout.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(jBtnClearLog, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(jBtnCancel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(jBtnTs, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(jBtnSs, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

		optLayout.setVerticalGroup(optLayout.createSequentialGroup().addComponent(jBtnClearLog).addComponent(jBtnCancel).addComponent(jBtnTs).addComponent(jBtnSs));

		GroupLayout leftLayout = new GroupLayout(leftPane);
		leftPane.setLayout(leftLayout);
		leftLayout.setAutoCreateContainerGaps(true);
		leftLayout.setAutoCreateGaps(true);

		leftLayout.setHorizontalGroup(leftLayout
				.createParallelGroup(GroupLayout.Alignment.CENTER)
				.addGroup(
						leftLayout.createSequentialGroup().addComponent(jTxtCmd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
								.addComponent(jTxtTimeExpired, 100, 100, 100)).addComponent(jScrollTask, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				.addComponent(jScrollLog, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

		leftLayout.setVerticalGroup(leftLayout
				.createSequentialGroup()
				.addGroup(
						leftLayout.createParallelGroup().addComponent(jTxtCmd, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(jTxtTimeExpired, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)).addComponent(jScrollTask).addComponent(jScrollLog));

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(false);

		layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(leftPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addComponent(controlPane, 300, 300, 300));

		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(leftPane).addComponent(controlPane));
		pack();
	}

	public void log(String info) {
		logPane.append(info);
		logPane.append("\n");
	}

	private void addTaskGUI(Message msg) {
		lstModel.addElement(msg);
	}

	public void backCancelTaskResult(Message msg) {
		if (msg.answer.equals("fail")) {
			log(msg.content);
		} else {
			for (int i = 0; i < lstModel.size(); i++) {
				Message m = lstModel.getElementAt(i);
				if (m.msgID.toString().equals(msg.answer)) {
					lstModel.removeElement(m);
					break;
				}
			}
			log(msg.content);
		}
	}

	public void backTaskResult(Message msg) {
		for (int i = 0; i < lstModel.size(); i++) {
			Message m = lstModel.getElementAt(i);
			if (m.msgID.toString().equals(msg.msgID.toString())) {
				lstModel.removeElement(m);
				break;
			}
		}
		log("Client received a task result: " + msg.content + ": " + msg.answer);
	}

	public void backTaskStatus(String msg) {
		log(msg);
	}

	public void backSysInfo(String msg) {
		log(msg);
	}

}
