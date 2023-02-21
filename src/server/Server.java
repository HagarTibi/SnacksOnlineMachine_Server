package server;
// This file contains material supporting section 3.7 of the textbook:

import java.util.ArrayList;

// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import common.*;
import gui.ServerGUIController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.util.ArrayList;


import java.util.ResourceBundle;


/*
 * 
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000
 */
public class Server extends AbstractServer {
	// Class variables *************************************************
	String passwordSQL;
	/**
	 * The default port to listen on.
	 */
	final public static int DEFAULT_PORT = 5555;
	// list of connected clients
	private static ObservableList<ConnectToClients> clientList = FXCollections.observableArrayList();

	//lists of Monthly Reports
	public static ArrayList<Object> orderReportData = new ArrayList<Object>();
	public static ArrayList<Object> machineOrderReportData = new ArrayList<Object>();
	public static ArrayList<Object> activityReportData = new ArrayList<Object>();
	Thread generateReports;

	// Constructors ****************************************************

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port The port number to connect on.
	 */
	public Server(int port, String passwordSQL) {
		super(port);
		this.passwordSQL = passwordSQL;


	}

	// Instance methods ************************************************

	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg    The message received from the client.
	 * @param client The connection from which the message originated.
	 */
	@SuppressWarnings("unchecked")
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if ((msg instanceof MsgHandler)) {
			MsgHandler<Object> clientMsg = (MsgHandler) msg;
			System.out.println("Message received: " + clientMsg.getType() + " from " + client);
			try {
				switch (clientMsg.getType()) {
				 case Request_connect:
					 	updateClientList(client, "Connected");
						 client.sendToClient(new MsgHandler(TypeMsg.Connected, null));
				 		break;
				 case Request_disconnected:
					 if (clientMsg.getMsg() != null)
					 	MySQLConnection.UpdateUserIsLoginStatus((String) clientMsg.getMsg().get(0));
					 updateClientList(client, "Disconnected");
					 client.sendToClient(new MsgHandler<>(TypeMsg.Disconnected, null));
					 break;
				case Request_Login:
					ArrayList<Object> send = new ArrayList<>();
					send = MySQLConnection.getUser(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Logged_Successful,send));
					break;
				case Set_isLogin:
					MySQLConnection.updateIsLogin((String) clientMsg.getMsg().get(0));
					client.sendToClient(new MsgHandler<>(TypeMsg.IsLogin_user,null));
					break;
				case Fast_LogIn:
					User user = new User("customer6","123456","Zohar","Shazar", Roles.Subscriber, "6", "0521236555", "ekrutbraude-G10@gmail.com");
					client.sendToClient(new MsgHandler<>(TypeMsg.Fast_LogIn,MySQLConnection.loadSubscriber(user)));
					break;
				case Request_logout:
						MySQLConnection.UpdateUserIsLoginStatus((String) clientMsg.getMsg().get(0));
						client.sendToClient(new MsgHandler<>(TypeMsg.Request_logout_successfuly,null));
						break;
					case Present_subscribers:
					updateClientList(client, "Connected");
					//client.sendToClient(new MsgHandler<>(TypeMsg.subscribers_data_imported, MySQLConnection.printSubscribers()));
					break;
				case Update_subscribe:
					// Execute the update
					//MySQLConnection.updateSubscriberInfo(clientMsg.getMsg());
					// Send message to the client that everything went well
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_subscriber_successfully, null));
					break;
				case Update_ThresholdLevel:
					MySQLConnection.UpdateThresholdLevel(clientMsg.getMsg());
					// Send message to the client that everything went well
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_Threshold_successfully,null));
					break;

				case Import_Catalog: // gilad
					client.sendToClient(new MsgHandler<>(TypeMsg.Catalog_Imported, (new MySQLConnection()).importCatalog()));
					break;
				case Import_User_Credit_Data:    //gilad
					ArrayList<String> customerCardInfo= new ArrayList<String>();
					customerCardInfo = MySQLConnection.getCustomerCardInfoFormCustomersTable(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.User_Credit_Data_Imported, customerCardInfo));
					break;
				case Update_Machine_Data:		// raz
					Order orderForUpdate = (Order)clientMsg.getMsg().get(0); // only one order: local order or remote order
					ArrayList<String> itemsInAlert = new ArrayList<>();
					itemsInAlert = MySQLConnection.updateItemsInLoacalMachine(orderForUpdate);
					System.out.println("Local Machine: "+ orderForUpdate.getMachine_id()+"has been successfuly updated");
					client.sendToClient(new MsgHandler<>(TypeMsg.Machine_Data_Updated,itemsInAlert));
					break;
				case Save_Order_To_DB:			// raz
					Order orderToSave = (Order)clientMsg.getMsg().get(0);
					ArrayList<String> orderCode = new ArrayList<>();
					orderCode = MySQLConnection.saveOrderToDB(orderToSave);
					System.out.println("Order: "+orderCode.get(0)+" has been successfuly saved in data base");
					client.sendToClient(new MsgHandler<>(TypeMsg.Order_Saved_To_DB,orderCode));
					break;
				case Import_Machines_Locations:
					ArrayList<String> area_location_list = MySQLConnection.getAreaLocationsList();
					client.sendToClient(new MsgHandler<>(TypeMsg.Machines_Locations_Imported, area_location_list));
					break;

				case import_machine_monthly_order_report_data:
					machineOrderReportData = MySQLConnection.getOrdersReportsDataOf_SpecificMachine(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.import_machine_monthly_order_report_data_successfully,machineOrderReportData));
					break;
				case import_order_monthly_report_data:
					orderReportData = MySQLConnection.getOrdersReportsDataOf_SeveralMachines(clientMsg.getMsg()); //the method got array list
					client.sendToClient(new MsgHandler<>(TypeMsg.import_order_monthly_report_data_successfully, orderReportData));

					break;
				case import_customer_Activity_report_data:
					activityReportData = MySQLConnection.getCustomerActivityLevelByOrders(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.import_customer_Activity_report_data, activityReportData));
					break;
				
				case Import_Excisting_Sales:
					// Send message to the client that everything went well
					client.sendToClient(new MsgHandler<>(TypeMsg.Sales_Data_Imported,(new MySQLConnection()).PresentSales()));
					break;
				case Update_Sale_To_Worker:
					MySQLConnection.updateSaleStatusArea(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Sales_Sent_To_Worker,null));
					break;
				case Present_all_sales_in_specific_area:
					System.out.println(clientMsg.getType().toString()+clientMsg.getMsg()+"area");
					//take only the sales that not define 0 to this area
					//Take the sale from sql by function present sale (return the all sales)
					ArrayList<Sale> arrarea=ArrAfterRemoveToSpecificArea((String)clientMsg.getMsg().get(0),(new MySQLConnection()).PresentSales());
					
					client.sendToClient(new MsgHandler<>(TypeMsg.Sales_Of_Specific_Area,arrarea));
					//need add send to mysqlconnection and array
					break;
				case Update_Active_Sale_InArea:	

					MySQLConnection.updateSaleStatusArea(clientMsg.getMsg());//update the activity status in specific area
			
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_Sale_Active_Success,null));

					break;
				
				case Create_New_Sale:
					

					MySQLConnection.saveNewSaleIntoDB(clientMsg.getMsg());//update the activity status in specific area
					ArrayList<Object> s=new ArrayList<>();
					
					client.sendToClient(new MsgHandler<>(TypeMsg.Create_New_Sale_Successfully,null));
					break;

					
				case import_user_detail:

					User userimported=MySQLConnection.getUserDetail(clientMsg.getMsg());
					if(userimported.getFirst_name()==null||userimported.getRole().toString()!="NULL"){
						client.sendToClient(new MsgHandler<>(TypeMsg.Import_UserDetail_Fail,null));
					}
					else {
						ArrayList<Object> arruser=new ArrayList<>();
						arruser.add((Object)userimported);
						client.sendToClient(new MsgHandler<>(TypeMsg.Import_UserDetail_success,arruser));
					}
					break;
				case addToCustomerRequest:
					ArrayList<Object> arrmsg=new ArrayList<>();
					arrmsg.add((Object)MySQLConnection.addUserToCustomerRequest(clientMsg.getMsg()));
					client.sendToClient(new MsgHandler<>(TypeMsg.addToCustomerRequest_success,arrmsg));
					break;
				case update_customer_to_Sub:
					MySQLConnection.updateCustomerToSubscriber(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Customer_updated_ToSubscriber_Successfuly,null));
					break;
				case Insert_customer_to_Subscribers_tab:
					MySQLConnection.saveNewSubscriberIntoDB(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.New_Subscriber_Added_Successfuly,null));
					break;
				case Import_customer_detail:

					client.sendToClient(new MsgHandler<>(TypeMsg.Imported_Customer_Details_Success,MySQLConnection.importCostumer(clientMsg.getMsg())));
					break;
				case Get_Inventory_report:
					ArrayList<Object> inventoryReport = MySQLConnection.getInventoryReport(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Show_Inventory_report,inventoryReport));
					break;
				case GetMachineList:
					ArrayList<String> machineList = MySQLConnection.getMachineList(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Show_Machine,machineList));
					break;

				case ImportUserRequestToSpecficArea:
					//
					ArrayList<String> arruserdetail=MySQLConnection.UserRequestInSpecificArea(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.ImportUserRequestToSpecficAreaSuccess,arruserdetail));
					break;
				case ApprovedUserCustomer:
					MySQLConnection.UpdateUserToCustomer(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.ApproveUserToCustomerSuccess,null));
					break;
				case Get_Delivery_Orders:
					ArrayList<Object> deliveryOrders = MySQLConnection.getDeliveryOrders(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Show_Delivery,deliveryOrders));
					break;
				case Import_real_time_amounts:
					client.sendToClient(new MsgHandler<>(TypeMsg.Real_time_amounts_received,
							MySQLConnection.getItemsInMachine((String)clientMsg.getMsg().get(0))));
					break;
				case Add_alert_to_DB:
					MySQLConnection.addAlertToDB((ItemsAlert)clientMsg.getMsg().get(0));
					client.sendToClient(new MsgHandler<>(TypeMsg.Alert_added_to_DB, null));
					break;
				case Check_sub_first_order:
					ArrayList<Boolean> made_first_order = new ArrayList<>();
					made_first_order.add(MySQLConnection.hasMadeFirstOrder((String)clientMsg.getMsg().get(0))); 
					client.sendToClient(new MsgHandler<>(TypeMsg.Answer_sub_first_order, made_first_order));
					break;
				case Import_sales_for_order:
					client.sendToClient(new MsgHandler<>(TypeMsg.Sales_Of_Specific_Area,(new MySQLConnection()).PresentSales()));
					break;
				case Update_sub_first_order: 
					MySQLConnection.updateMadeFirstOrder((String)clientMsg.getMsg().get(0));
					client.sendToClient(new MsgHandler<>(TypeMsg.Sub_first_order_updated, null));
					break;
				case Check_delay_payments:
					ArrayList<Boolean> is_delay_payment = new ArrayList<>();
					is_delay_payment.add(MySQLConnection.hasDelayPayment((String)clientMsg.getMsg().get(0))); 
					client.sendToClient(new MsgHandler<>(TypeMsg.Delay_payments_checked, is_delay_payment));
					break;
				case Change_delay_payments:
					MySQLConnection.updateDelayPayment((String)clientMsg.getMsg().get(0));
					client.sendToClient(new MsgHandler<>(TypeMsg.Delay_payments_changed, null));
					break;
				case Update_ManageAlertsManagerScreen:    //hen - for initialize function of the screen managerAlertManagerController
					ArrayList<String> alertsTableInfo= new ArrayList<String>();
					alertsTableInfo = MySQLConnection.getAlertsTableForManager(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_ManageAlertsManagerScreen, alertsTableInfo));
					break;
				case Update_AfterManagerPressedSendButton:    //hen for send button in managerAlertManagerController screen
					ArrayList<String> updateWentSuccessfully = new ArrayList<String>();
					updateWentSuccessfully = MySQLConnection.updateAfterManagerPressedSendBtn(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_AfterManagerPressedSendButton, updateWentSuccessfully));
					break;
				case Check_exist_remote_order:
					ArrayList<String> remote_orders_info = new ArrayList<>();
					remote_orders_info = MySQLConnection.importUserRemoteOrders
							((String)clientMsg.getMsg().get(0), (String)clientMsg.getMsg().get(1));
					client.sendToClient(new MsgHandler<>(TypeMsg.Remote_order_checkd, remote_orders_info));
					break;
				case Update_remote_order:
					MySQLConnection.updateRemoteOrderReceiveTime((String)clientMsg.getMsg().get(0),(String)clientMsg.getMsg().get(1));
					client.sendToClient(new MsgHandler<>(TypeMsg.Remote_order_updated, null));
					break;
				case Get_Alert_For_ISW:
					String username = (String)clientMsg.getMsg().get(0);
					ArrayList<ItemsAlert> iswAlerts = MySQLConnection.getISWalerts(username);
					System.out.println("Alerts for "+username+" has been successfuly imported");
					client.sendToClient(new MsgHandler<>(TypeMsg.Alerts_For_ISW_Imported,iswAlerts));
					break;	
				case Get_Items_In_Machine:
					String machine_id = (String)clientMsg.getMsg().get(0);
					ArrayList<ItemInMachine> items = MySQLConnection.getItemsInMachine(machine_id);
					System.out.println("Items from "+machine_id+" have been successfuly imported");
					client.sendToClient(new MsgHandler<>(TypeMsg.Items_Machine_Imported,items));
					break;	
				case Update_Items_Amount:
					ArrayList<ItemInMachine> update = (ArrayList<ItemInMachine>)clientMsg.getMsg().get(1);
					String machine = (String)clientMsg.getMsg().get(0);
					int threshold = (int)clientMsg.getMsg().get(2);
					MySQLConnection.ISWUpdate(update,machine,threshold);
					client.sendToClient(new MsgHandler<>(TypeMsg.ISW_Updated_Machine,null));
					break;			
				case Update_Alrt_Status_To_Done:
					machine = (String)clientMsg.getMsg().get(0);
					MySQLConnection.ISWUpdateAlertStatus(machine);
					client.sendToClient(new MsgHandler<>(TypeMsg.Alert_Status_Updated_To_Done,null));
					break;
				case Get_Threshold:
					threshold = MySQLConnection.getThreshold((String)clientMsg.getMsg().get(0));
					ArrayList<Integer> t = new ArrayList<>();
					t.add(threshold);
					client.sendToClient(new MsgHandler<>(TypeMsg.Threshold_Imported,t));
					break;

				case Update_Alrt_Status_To_CONFIRM:
					ArrayList<RemoteOrder> updateRO = new ArrayList<>();
					for(int i=0;i<clientMsg.getMsg().size();i++) {
						updateRO.add((RemoteOrder)clientMsg.getMsg().get(i));
					}
					MySQLConnection.updateOrdersRemote(updateRO);
					client.sendToClient(new MsgHandler<>(TypeMsg.Orders_Remote_Imported,null));
					break;

				case Import_PickUps_Orders:	
					client.sendToClient(new MsgHandler<>(TypeMsg.Imported_PickUps_Success,MySQLConnection.getPickUps(clientMsg.getMsg())));
					break;
				case Impotrt_Delivery_Orders:
					client.sendToClient(new MsgHandler<>(TypeMsg.Imported_Delivery_Success,MySQLConnection.getDelivery(clientMsg.getMsg())));
					break;
				case Update_order_Status:
					MySQLConnection.updateDeliveryOrderStatus(clientMsg.getMsg());
					client.sendToClient(new MsgHandler<>(TypeMsg.Update_order_Status_Success,null));
					break;
				case Import_machine_tresholds:
					client.sendToClient(new MsgHandler<>(TypeMsg.Machine_tresholds_imported,
							MySQLConnection.importMachinesThresholds(clientMsg.getMsg())));
					break;
				default:
					break;
				

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method updates the Connected\Disconnect clients to our Server Each time
	 * a user Connect\disconnects we update the Connect Users in our Server GUI if a
	 * user connects his status is "Connected" and if he disconnects status changes
	 * to "Disconnected"
	 * 
	 * @param client
	 * @param connectionStatus
	 */
	static void updateClientList(ConnectionToClient client, String connectionStatus)  {

		for (int i = 0; i < clientList.size(); i++) {
			/* Comparing clients by IP addresses */
			try {
				if (clientList.get(i).getIp().equals(client.getInetAddress().getHostAddress())) {
					clientList.get(i).setStatus(connectionStatus);
					clientList.remove(i);
				}
			} catch (NullPointerException ex) {
				System.out.println("Error! Client not found");
			}
		}

		 //In both cases of Connect and Disconnected we will need to add Client into the
		 //list so this function covers both of them simultaneously
		try{
		clientList.add(new ConnectToClients(client.getInetAddress().getHostAddress(),
				client.getInetAddress().getHostName(), connectionStatus));
		} catch (NullPointerException ex) {
			System.out.println("Error! Client not found");
	}
	}
	public ArrayList<Sale> ArrAfterRemoveToSpecificArea(String area,ArrayList<Sale> s) {//stay only the sale that define in specific area
		ArrayList<Sale> arrsaleinarea=new ArrayList<>();
		for(int i=0;i<s.size();i++) {
			String statusofsale=s.get(i).getAreaSale().get(area);
			if(!statusofsale.equals("0")) {//if the sale define to specific area
				arrsaleinarea.add(s.get(i));
			}
		}
		return arrsaleinarea;
	}

    
	/**
	 * This method updates the Connected\Disconnect clients to our Server Each time
	 * a user Connect\disconnects we update the Connect Users in our Server GUI if a
	 * user connects his status is "Connected" and if he disconnects status changes
	 * to "Disconnected"
	 */
	static void removeClientList()  {
		for (int i = 0; i < clientList.size(); i++) 
			clientList.remove(i);
	}
	
	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());

		try {
			MySQLConnection.connectToDB(this.passwordSQL);
			generateReports = new Thread(new GenerateReports());
			generateReports.start();
		} catch (Exception ex) {
			System.out.println("Error! DataBase Connection Failed");
		}
	}

	/**
	 * This method overrides the one in the superclass. Called when the server stops
	 * listening for connections.
	 */
	protected void serverStopped() {
		if (generateReports != null && generateReports.isAlive()) {
			generateReports.interrupt();
		}
		removeClientList();
		stopListening();
	}

	public static ObservableList<ConnectToClients> getClientList() {
		return clientList;
	}

	public static void setClientList(ObservableList<ConnectToClients> clientList) {
		Server.clientList = clientList;
	}
	@Override
	protected void clientConnected(ConnectionToClient client) {

	}

}
