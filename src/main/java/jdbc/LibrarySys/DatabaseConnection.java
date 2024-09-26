package jdbc.LibrarySys;

import java.awt.GridLayout;
import java.awt.TextField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class DatabaseConnection {

    private Connection conn = null;
    private Session proxySession = null;
    private String jdbcHost;
    private int jdbcPort;
    private final String databaseHost = "dbhost.example.com";
    private final int databasePort = 1521;
    private final String database = "db.name";
    private final String proxyHost = "proxy.example.com";
    private final int proxyPort = 22;
    private final String forwardHost = "localhost";
    private int forwardPort; // Dynamically assign in loginProxy()
    
	public DatabaseConnection() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            LoggerUtil.getLogger().info("Oracle JDBC driver loaded.");
        } catch (ClassNotFoundException e) {
            LoggerUtil.getLogger().error("Oracle JDBC driver not found", e);
        }
	}

    /**
     * Login the proxy. Do not change this function.
     *
     * @return boolean
     */
    public boolean loginProxy() {
        if (getYESorNO("Using ssh tunnel or not?")) {
            String[] namePwd = getUsernamePassword("Login ssh proxy");
            String sshUser = namePwd[0];
            String sshPwd = namePwd[1];

            try {
            	LoggerUtil.getLogger().debug("Attempting SSH login for user: " + sshUser);
            	LoggerUtil.getLogger().debug("Setting up SSH Proxy session");
                proxySession = new JSch().getSession(sshUser, proxyHost, proxyPort);
                proxySession.setPassword(sshPwd); 
                LoggerUtil.getLogger().info("SSH Proxy session created");
                
                // Additional configuration - avoid host key checking
                LoggerUtil.getLogger().debug("Setting up SSH Proxy configuration");
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                proxySession.setConfig(config);
                
                proxySession.connect();
                LoggerUtil.getLogger().info("SSH Proxy login successful");
                
                // Port forwarding
                LoggerUtil.getLogger().debug("Setting up port forwarding");
                proxySession.setPortForwardingL(forwardHost, 0, databaseHost, databasePort); // Forward to database host
                forwardPort = Integer.parseInt(proxySession.getPortForwardingL()[0].split(":")[0]); // Get dynamically assigned port
                LoggerUtil.getLogger().info("Port forwarding established: " + forwardHost + ":" + forwardPort + " -> " + databaseHost + ":" + databasePort);
            } catch (JSchException e) {
                LoggerUtil.getLogger().fatal("SSH Proxy login failed", e);
                return false;
            }
            jdbcHost = forwardHost;
            jdbcPort = forwardPort;
            LoggerUtil.getLogger().info("Database connection stablished via SSH tunnel");
            LoggerUtil.getLogger().info("Database host set to forward host: " + jdbcHost + ", port: " + jdbcPort);
        } else {
            jdbcHost = databaseHost;
            jdbcPort = databasePort;
        	LoggerUtil.getLogger().info("Database connection established without SSH tunnel");
            LoggerUtil.getLogger().info("Database host set to: " + jdbcHost + ", port: " + jdbcPort);
        }
        return true;
    }

    /**
     * Login the oracle system. Change this function under instruction.
     *
     * @return boolean
     */
    public boolean loginDB() {
        String[] namePwd = getUsernamePassword("Login database");
        String dbUsername = namePwd[0];
        String dbPwd = namePwd[1];
        
        String url = "jdbc:oracle:thin:@" + jdbcHost + ":" + jdbcPort + "/" + database;
        try {
            LoggerUtil.getLogger().debug("Logging in to: " + url);
            conn = DriverManager.getConnection(url, dbUsername, dbPwd);
            LoggerUtil.getLogger().info("Database login successful");
            return true;
        } catch (SQLException e) {
            LoggerUtil.getLogger().fatal("Database login failed", e);
            return false;
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                LoggerUtil.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                LoggerUtil.getLogger().error("Error while closing connection", e);
            }
        }
    }
    

    // Helper methods
    
    /**
     * Get YES or NO. Do not change this function.
     *
     * @return boolean
     */
    boolean getYESorNO(String message) {
    	LoggerUtil.getLogger().trace("Creating prompt message: " + message);
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = pane.createDialog(null, "Question");
        dialog.setVisible(true);
        boolean result = JOptionPane.YES_OPTION == (int) pane.getValue();
        dialog.dispose();
        LoggerUtil.getLogger().info("User connecting " + (result ? "with SSH" : "without SSH"));
        return result;
    }
    
    /**
     * Get username & password. Do not change this function.
     *
     * @return username & password
     */
    String[] getUsernamePassword(String Login) {
    	LoggerUtil.getLogger().trace("Creating login prompt for user credentials: " + Login);
        JPanel panel = new JPanel();
        final TextField usernameField = new TextField();
        final JPasswordField passwordField = new JPasswordField();
        panel.setLayout(new GridLayout(2, 2));
        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            private static final long serialVersionUID = 1L; 

            @Override
            public void selectInitialValue() {
                usernameField.requestFocusInWindow();
            }
        };
        JDialog dialog = pane.createDialog(null, Login);
        dialog.setVisible(true);
        LoggerUtil.getLogger().info("User entered username: " + usernameField.getText());
        LoggerUtil.getLogger().info("User entered password: " + new String(passwordField.getPassword()));
        dialog.dispose();
        return new String[] { usernameField.getText(), new String(passwordField.getPassword()) };
    }

}
