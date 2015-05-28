package edu.utas.kit418.assig3.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Date;

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
import javax.swing.text.DefaultCaret;

import edu.utas.kit418.assig3.common.Message;

public class GUI extends JFrame implements WindowListener {
	private static final long serialVersionUID = 1L;
	private JTextArea jLogArea;
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
	private LoginDialog loginDiag;
	public boolean authenticating;

	public GUI() {
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setTitle("Cluster Computing GUI");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage("res" + File.separatorChar + "icon.png"));
		setMinimumSize(new Dimension(800, 600));
		createComponents();
		performLayout();
		loginDiag = new LoginDialog(this);
		loginDiag.addWindowListener(this);
	}

	private void createComponents() {

		jLogArea = new JTextArea();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		jLogArea.setLineWrap(false);
		jLogArea.setWrapStyleWord(true);
		jLogArea.setEditable(false);
		((DefaultCaret) jLogArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		jLstTask = new JList<Message>();
		jLstTask.setModel(lstModel);
		jLstTask.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		jScrollLog = new JScrollPane(jLogArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollLog.setBorder(BorderFactory.createTitledBorder("Result"));
		jScrollTask = new JScrollPane(jLstTask, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollTask.setBorder(BorderFactory.createTitledBorder("Task List"));

		controlPane = new JPanel();
		controlPane.setBorder(BorderFactory.createTitledBorder("Options"));
		leftPane = new JPanel();
		jBtnClearLog = new JButton("Clear Log");
		jBtnCancel = new JButton("Cancel Task");
		jBtnTs = new JButton("Task Status");
		jBtnSs = new JButton("Server Info");
		jTxtCmd = new JTextField();
		jTxtCmd.setToolTipText("input command here");
		jTxtCmd.setBorder(BorderFactory.createTitledBorder("Command"));
		jTxtTimeExpired = new JTextField();
		jTxtTimeExpired.setToolTipText("set time limit (s)");
		jTxtTimeExpired.setBorder(BorderFactory.createTitledBorder("Time Limit"));

		jTxtCmd.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.equals("test")) {
					doTest();
					return;
				}
				if (cmd.equals(""))
					return;
				Message msg = Client.addTaskMsg(cmd, jTxtTimeExpired.getText());
				addTaskGUI(msg);
				jTxtCmd.setText("");
			}

			private void doTest() {
				double[] expTime = new double[]{Double.MAX_VALUE, 1000,800,600,500,400,200,100,0,Double.MAX_VALUE};
				for(int i=1;i<=10;i++){
					Message msg = Client.addTaskMsg("task"+i, String.valueOf(expTime[i-1]));
					addTaskGUI(msg);
				}
				jTxtCmd.setText("");
			}
		});

		jBtnClearLog.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				jLogArea.setText("");
			}
		});

		jBtnCancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selectedIdx = jLstTask.getSelectedIndices();
				if (selectedIdx.length == 0) {
					log("select one or muptile tasks");
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
						leftLayout.createSequentialGroup().addComponent(jTxtCmd, 200, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
								.addComponent(jTxtTimeExpired, GroupLayout.DEFAULT_SIZE, 100, 100)).addComponent(jScrollTask, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
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

		layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(leftPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addComponent(controlPane, 150, 150, 200));

		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(leftPane).addComponent(controlPane));
		pack();
	}

	public void log(String info) {
		jLogArea.append(info);
		jLogArea.append("\n");
	}

	private void addTaskGUI(Message msg) {
		lstModel.addElement(msg);
		jLstTask.ensureIndexIsVisible(lstModel.indexOf(msg));
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
		long time = new Date().getTime();
		log("Client received a task result: " + msg.content + ": " + msg.answer);
		log("time elapsed: " + (time - msg.buildTime) / 1000 + "s");
	}

	public void backTaskStatus(String msg) {
		log(msg);
	}

	public void backSysInfo(String msg) {
		log(msg);
	}

	public void promptErr(String msg) {
		loginDiag.invalidArgs(msg);
		setEnabled(true);
		loginDiag.setVisible(true);
		setEnabled(false);
	}

	public void authenticated(boolean auth) {
		loginDiag.login(auth);
		if (auth)
			setEnabled(true);
	}

	@Override
	public void windowOpened(WindowEvent e) {

	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == GUI.this || (!authenticating && e.getSource() == loginDiag)) {
			Client.goToDie();
			setEnabled(false);
			Client.guiRunning = false;
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {

	}

	@Override
	public void windowDeiconified(WindowEvent e) {

	}

	@Override
	public void windowActivated(WindowEvent e) {

	}

	@Override
	public void windowDeactivated(WindowEvent e) {

	}

}
