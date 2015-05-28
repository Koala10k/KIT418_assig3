package edu.utas.kit418.assig3.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

public class LoginDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JTextField tfIP;
	private JTextField tfPort;
	private JTextField tfUsername;
	private JPasswordField pfPassword;
	private JLabel lbIp;
	private JLabel lbPort;
	private JLabel lbUsername;
	private JLabel lbPassword;
	private JButton btnLogin;
	private boolean succeeded;
	public JLabel jInfo;
	public GUI parentFrame;

	public LoginDialog(Frame parent) {
		super(parent, "Login", true);
		parentFrame = (GUI) parent;
		//
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints cs = new GridBagConstraints();

		jInfo = new JLabel();
		cs.fill = GridBagConstraints.HORIZONTAL;

		lbUsername = new JLabel("Username: ");
		cs.gridx = 0;
		cs.gridy = 0;
		cs.gridwidth = 1;
		panel.add(lbUsername, cs);

		tfUsername = new JTextField(20);
		cs.gridx = 1;
		cs.gridy = 0;
		cs.gridwidth = 2;
		panel.add(tfUsername, cs);

		lbPassword = new JLabel("Password: ");
		cs.gridx = 0;
		cs.gridy = 1;
		cs.gridwidth = 1;
		panel.add(lbPassword, cs);

		pfPassword = new JPasswordField(20);
		cs.gridx = 1;
		cs.gridy = 1;
		cs.gridwidth = 2;
		panel.add(pfPassword, cs);

		lbIp = new JLabel("Server IP: ");
		cs.gridx = 0;
		cs.gridy = 2;
		cs.gridwidth = 1;
		panel.add(lbIp, cs);

		tfIP = new JTextField(20);
		cs.gridx = 1;
		cs.gridy = 2;
		cs.gridwidth = 2;
		panel.add(tfIP, cs);

		lbPort = new JLabel("Server Port: ");
		cs.gridx = 0;
		cs.gridy = 3;
		cs.gridwidth = 1;
		panel.add(lbPort, cs);

		tfPort = new JTextField(20);
		cs.gridx = 1;
		cs.gridy = 3;
		cs.gridwidth = 2;
		panel.add(tfPort, cs);

		// DEBUG
		tfUsername.setText("admin");
		pfPassword.setText("admin");
		tfIP.setText("144.6.225.0");
		tfPort.setText("4444");
		panel.setBorder(new LineBorder(Color.GRAY));

		btnLogin = new JButton("Login");

		btnLogin.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String[] args = new String[4];
				args[0] = tfIP.getText().trim();
				args[1] = tfPort.getText().trim();
				args[2] = tfUsername.getText().trim();
				args[3] = new String(pfPassword.getPassword()).trim();
				Client.setArgs(args);
				btnLogin.setText("Logining...");
				parentFrame.authenticating = true;
				btnLogin.setEnabled(false);
				LoginDialog.this.setVisible(false);
			}
		});
		JPanel bp = new JPanel();
		bp.add(jInfo, SpringLayout.WEST);
		bp.add(btnLogin, SpringLayout.EAST);

		getContentPane().add(panel, BorderLayout.CENTER);
		getContentPane().add(bp, BorderLayout.SOUTH);

		pack();
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(parent);
	}

	@Override
	public void setVisible(boolean b) {
		if (b)
			parentFrame.authenticating = false;
		super.setVisible(b);
	}

	public String getUsername() {
		return tfUsername.getText().trim();
	}

	public String getPassword() {
		return new String(pfPassword.getPassword());
	}

	public boolean isSucceeded() {
		return succeeded;
	}

	public void invalidArgs(String msg) {
		jInfo.setText(msg);
		btnLogin.setText("Login");
		btnLogin.setEnabled(true);
	}

	public void login(boolean b) {
		tfIP.setEnabled(false);
		tfPort.setEnabled(false);
		if (b) {
			dispose();
		} else {
			jInfo.setText("Login Failed");
			btnLogin.setText("Login");
			btnLogin.setEnabled(true);
			setVisible(true);
		}
	}
}
