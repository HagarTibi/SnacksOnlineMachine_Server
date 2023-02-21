package gui;



import common.ConnectToClients;
import common.Console;
import common.GenerateReports;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import server.MySQLConnection;
import server.Server;
import server.ServerUI;


import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

import java.util.ResourceBundle;

public class ServerGUIController implements Initializable {
	public static ServerGUIController controller;
    @FXML
    private TableView<ConnectToClients> connectToClients;
    @FXML
    private TableColumn<ConnectToClients, String> Host;

    @FXML
    private TableColumn<ConnectToClients, String> IP;

    @FXML
    private TableColumn<ConnectToClients, String> Status;
    @FXML
    private javafx.scene.control.TextArea Console;


    @FXML
    private Button btConnect;

    @FXML
    private Button btDisconnect;

    @FXML
    private Button btImportData;

    @FXML
    private Label lbConnectToclient;
    @FXML
    private Text txtError;
    @FXML
    private Label lbDBPassword;

    @FXML
    private Label lbDBname;

    @FXML
    private Label lbIP;

    @FXML
    private Label lbPort;

    @FXML
    private Label lbServerconfig;

    @FXML
    private Label lbUsername;

    @FXML
    private Button Xbt;
    @FXML
    private Text txtConsole;

    @FXML
    private TextField txtDBname;

    @FXML
    private TextField txtIP;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPort;

    @FXML
    private TextField txtUsername;
    private PrintStream replaceConsole;

    public String getLocalIp() {
        String ip = null;
        try {
            ip = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {

            e.printStackTrace();
        }
        return ip;
    }

    @FXML
    void Connection(javafx.event.ActionEvent event)  {

        if (txtPassword.getText().equals("")){
            txtError.setVisible(true);
            txtError.setText("You must enter a password to connect to the DB");
            return;
        }
        ServerUI.runServer(txtIP.getText(), txtPort.getText(), txtDBname.getText(), txtUsername.getText(), txtPassword.getText());
        btConnect.setDisable(true);
        btDisconnect.setDisable(false);
        disableDataInput(true);
        txtError.setVisible(false);
    }

        @FXML
    void Disconnection(ActionEvent event) {
        ServerUI.disconnect();
        this.connectToClients.setItems(Server.getClientList());
        btDisconnect.setDisable(true);
        btConnect.setDisable(false);
        disableDataInput(false);
        btImportData.setVisible(true);

    }
    // This functions turns off the option to pass data in the TextArea

    void disableDataInput(boolean Condi) {
        txtIP.setDisable(Condi);
        txtPort.setDisable(Condi);
        txtDBname.setDisable(Condi);
        txtUsername.setDisable(Condi);
        txtPassword.setDisable(Condi);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        this.connectToClients.setItems(Server.getClientList());
        setTableColumns();
        consoleStreamIntoGUI();
        this.txtIP.setText(getLocalIp());
        this.txtPort.setText("5555");
        this.txtDBname.setText("jdbc:mysql://localhost/ekrut?serverTimezone=IST");
        this.txtUsername.setText("root");
        this.txtPassword.setText("");
        this.btDisconnect.setDisable(true);
    }



    private void setTableColumns() {
        this.IP.setCellValueFactory(new PropertyValueFactory<ConnectToClients, String>("ip"));
        this.Host.setCellValueFactory(new PropertyValueFactory<ConnectToClients, String>("host"));
        this.Status.setCellValueFactory(new PropertyValueFactory<ConnectToClients, String>("Status"));
    }


    @FXML
    /**
     * consoleStreamIntoGUI replace the System stream output console into our  FXML component
     */
    void consoleStreamIntoGUI() {

        replaceConsole = new PrintStream(new Console(Console));
        System.setOut(replaceConsole);
        System.setErr(replaceConsole);
    }

    @FXML
    void X(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    void ImportData(ActionEvent event) throws SQLException {

        Connection conImport = null;
        String targetSchema = "ekrut";
        String externalDBSchemeName = "importsimulation";
        String queryUsers = "INSERT INTO " + targetSchema + "." + "users" + " SELECT * FROM " + externalDBSchemeName + "." + "users";
        String deleteUsersQuery = "DELETE FROM " + targetSchema + ".users";
        try {
            if (txtPassword.getText().equals("")){
                txtError.setVisible(true);
                txtError.setText("You must enter a password to connect to the DB");
                txtError.setFill(Color.RED);
                return;
            }
            conImport = DriverManager.getConnection("jdbc:mysql://localhost/importsimulation?serverTimezone=IST", "root", txtPassword.getText());
            Statement stmtDeleteUsers = conImport.createStatement();
            stmtDeleteUsers.executeUpdate(deleteUsersQuery);

            Statement stmtUsers = conImport.createStatement();
            stmtUsers.executeUpdate(queryUsers);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if(conImport != null) {
                try {
                    conImport.close();
                    conImport = null;
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
         }
        System.out.println("Import Simulation Users Succeed");
        btImportData.setVisible(false);
        txtError.setText("*You can Import Data at least once!*");
        txtError.setFill(Color.BLACK);

    }

}
