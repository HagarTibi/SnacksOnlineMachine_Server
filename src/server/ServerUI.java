package server;

import common.MsgHandler;
import common.TypeMsg;
import gui.ServerGUIController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class ServerUI extends Application {
	static Server EKrutserver;

	public static void main(String[] args) {

		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		AnchorPane pane;
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/gui/EKrutServerGUI.fxml"));
			pane = loader.load();
			ServerGUIController.controller = loader.getController();
			
		} 
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.show();
	}
	public static void runServer(String IP, String port,String DBName, String username, String password) {
		int Port = 0; // Port to listen on

		try {
			Port = Integer.parseInt(port); // Set port to 5555

		} catch (Throwable t) {
			System.out.println("ERROR - Could not connect!");
			return;
		}
		if (EKrutserver == null)
			EKrutserver = new Server(Port, password);
		else {
			EKrutserver.setPort(Port);
			EKrutserver.passwordSQL = password;
		}

		try {
			EKrutserver.listen(); // Start listening for connections
		} catch (IOException e) {
			System.out.println("ERROR - Could not listen for clients!");
		}
		
	    	}
	public static void disconnect() {
			try {
				EKrutserver.serverStopped();
				EKrutserver.close();
				MySQLConnection.con1.close();
				System.out.println("SQL Disconnected");
				System.out.println("Server Disconnected");
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}