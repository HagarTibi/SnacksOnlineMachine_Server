
package server;



import common.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class MySQLConnection {
	public static Connection con1;

	/**
	 * Connects to a MySQL database with the provided password.
	 * 
	 * @param password The password to use when connecting to the database.
	 */
	@SuppressWarnings("deprecation")
	public static void connectToDB(String password) 
	{
		try 
		{
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            System.out.println("Driver definition succeed");
        } catch (Exception ex) {
        	/* handle the error*/
        	 System.out.println("Driver definition failed");
        	 }
        
        try 
        {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/ekrut?serverTimezone=IST&useSSL=false","root",password);
            //Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.3.68/test","root","Root");
            System.out.println("SQL connection succeed");
            con1 = conn;
     	} catch (SQLException ex) 
     	    {/* handle any errors*/
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            }
   	}
	
	/**
	 * Saves an ArrayList of strings to the 'subscriber' table in the MySQL database.
	 *
	 * @param arr The ArrayList of strings to be saved to the database.
	 */
	public static void saveToUserDB(ArrayList<String> arr) {
		PreparedStatement stmt;
	
		try {
			stmt = con1.prepareStatement("INSERT INTO subscriber VALUES (?,?,?,?,?,?,?)");
			for (int i = 0; i < arr.size(); i++) {
				stmt.setString(i + 1, arr.get(i));
			}
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Calls the saveToUserDB method.
	 *
	 * @param arr The ArrayList of strings to be saved to the database.
	 */
	public static void parsing(ArrayList<String> arr) {
		saveToUserDB(arr);
	}

	/**
	 * Check if the data stored in ArrayList of Objects is not exist in the subscriber table in the database.
	 *
	 * @param arr The ArrayList of objects to be check in the database.
	 * @return true if the data is not exist in the database, false otherwise.
	 */
	public static boolean checkIfNotExist(ArrayList<Object> arr) {

		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM subscriber WHERE id='" + ((Subscribe)arr.get(0)).getUser_id() + "'";
			ResultSet rs = stmt.executeQuery(queryCheck); 

			if (rs.absolute(1)) {
				return true;
			}

			rs.close();
		}catch (SQLException e) {System.out.println("exception");};
		return false;
	}

	/**
	 * This Method Import existing Orders report details from database (segmentation by machine)
	 * 
	 * @param msg  - that include month,year,machine area
	 */
	public static ArrayList<Object> getOrdersReportsDataOf_SeveralMachines(ArrayList<Object> msg)  {     //Hagar

		PreparedStatement stmt;
		//General month,year & Area of several machines
		ArrayList<Object> orderReportData = new ArrayList<>();
		String month = (String) msg.get(0);
		String year = (String) msg.get(1);
		String Area = (String) msg.get(2);
		ArrayList<String> MachinesLocation = new ArrayList<>();
		HashMap<String, String> map_ofItems = new HashMap<String, String>(); //HashMap for "item_in_report" column
		ArrayList<MonthlyOrdersReportReturns> ordersReportReturn = new ArrayList<>(); //ArrayList to save several of reports
		ArrayList<MonthlyOrdersReport> ordersReport = new ArrayList<>(); //ArrayList to save several of reports
		int mostWantedMaxAmount = 0;
		String mostWantedItemName = null;
		int numberOfTotalOrders = 0;
		String machine_id=null;
		Double calculatePrecentage = 0.0;
		ArrayList<Double> percentageOfMachineOrders = null;
		String date = null;
		try {
			stmt = con1.prepareStatement("SELECT * FROM ekrut.monthly_orders_reports WHERE month = ? AND year = ? AND Area = ?");
			stmt.setString(1, month);
			stmt.setString(2, year);
			stmt.setString(3, Area);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				ArrayList<String> ordersReportDetails = new ArrayList<>();
				for (int i = 0; i < 9; i++) {
					ordersReportDetails.add(rs.getString(i + 1));
				}

				//build hash map witch contain item & Quantity that ordered from him
				String[] strbuild = null;
				strbuild = ordersReportDetails.get(2).split(",");
				for (int i = 0; i < strbuild.length; i = i + 2) {
					map_ofItems.put(strbuild[i], strbuild[i + 1]);
				}
				//find and save the most purchased item
				for (Map.Entry<String, String> entry : map_ofItems.entrySet()) {
					String itemName = entry.getKey();
					int count = 0;
					count = Integer.parseInt(map_ofItems.get(itemName));
					map_ofItems.put(itemName, String.valueOf(count));
					if (Integer.parseInt(map_ofItems.get(itemName)) > mostWantedMaxAmount) {
						mostWantedMaxAmount = Integer.parseInt(map_ofItems.get(itemName));
						mostWantedItemName = itemName;
					}
				}
				date = month + "/" + year;
				ordersReport.add(new MonthlyOrdersReport(ordersReportDetails.get(1), map_ofItems, ordersReportDetails.get(3), ordersReportDetails.get(5), ordersReportDetails.get(6), ordersReportDetails.get(7), ordersReportDetails.get(8)));

				//sum of total orders in the same Area
				calculatePrecentage = 0.0;
				percentageOfMachineOrders = new ArrayList<>();
				numberOfTotalOrders = 0;
				for (int i = 0; i < ordersReport.size(); i++) {
					numberOfTotalOrders += Integer.parseInt(ordersReport.get(i).getTotalOrdersCount());
				}
				//calculate & save the percentage from total number orders
				for (int j = 0; j < ordersReport.size(); j++) {
					calculatePrecentage = (Double.parseDouble(ordersReport.get(j).getTotalOrdersCount()) / Double.parseDouble(String.valueOf(numberOfTotalOrders))) * 100;
					percentageOfMachineOrders.add(calculatePrecentage);
				}
				MachinesLocation.add(ordersReportDetails.get(1));

			}
			//rs.close();
		} catch (SQLException e) {
			System.out.println("Importing Orders Report has failed!");
		}
		orderReportData.add(new MonthlyOrdersReportReturns(MachinesLocation, date, percentageOfMachineOrders, Area, mostWantedItemName, String.valueOf(numberOfTotalOrders)));
		return orderReportData;

	}

	/**
	 * Returns a MachineOrdersReport object based on the given location. The report contains data from the
	 * "monthly_orders_reports" table in the database, including the location, a map of the items in the report
	 * and their counts, the total number of orders, the number of canceled orders, the date, and the name of
	 * the most sold item.
	 *
	 * @param msg A list containing the location to get the report for.
	 * @return A MachineOrdersReport object with the data for the given location.
	 */
	public static ArrayList<Object> getOrdersReportsDataOf_SpecificMachine(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<Object> machine_report=new ArrayList<>();
		String mostWantedItemName = null, items;
		String[] itemsList = null;
		ArrayList<MonthlyOrdersReport> ordersReport = new ArrayList<>();
		ArrayList<MachineOrdersReport> machineOrdersReport = new ArrayList<>();
		HashMap<String, String> mapOfItems = new HashMap<String, String>();
		String numOfTotalOrders = null, numOfOrdersThatCanceled = null;


		String location = (String) msg.get(0);
		String date =  ((String) msg.get(1));
		String[] temp = date.split("/");
		String month = temp[0];
		String year = temp[1];
		try {
			stmt = con1.prepareStatement("SELECT * FROM ekrut.monthly_orders_reports WHERE month = ? AND year = ? AND location= ?");
			stmt.setString(1, month);
			stmt.setString(2, year);
			stmt.setString(3, location);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				items = rs.getString("items_in_report");
				month = rs.getString("month");
				year = rs.getString("year");
				location = rs.getString("location");
				numOfTotalOrders = String.valueOf(rs.getInt("total_orders"));
				//numOfOrdersThatCanceled = String.valueOf(rs.getInt("canceled_orders"));
				mostWantedItemName = rs.getString("most_sold");
				date = month + "/" + year;

			} else
				return null;

			itemsList = items.split(",");
			for (int i = 0; i < (itemsList.length); i = i + 2) {
				mapOfItems.put(itemsList[i], itemsList[i + 1]);
			}

		} catch (SQLException e) {
			System.out.println("Import Machine Orders Report has failed!");
		}
		System.out.println("Import Machine Orders Report has Succeeded!");
		machine_report.add( new MachineOrdersReport(location, mapOfItems, numOfTotalOrders, date, mostWantedItemName));
		return machine_report;
	}
	
	/**
	 * Update the "is_logged_in" field of the user with the provided userId in the "users" table in the database.
	 *
	 * @param userId The user id of the user to update the "is_logged_in" field for.
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection
	 */
	public static void updateIsLogin(String userId) throws SQLException {
		String query = "UPDATE users SET is_logged_in= ? WHERE user_id='" + userId + "'";
		PreparedStatement preparedStatement = con1.prepareStatement(query);
		preparedStatement.setString(1, "1");
		preparedStatement.executeUpdate();
	}
	
	/**
	 * Retrieve a user from the "users" table in the database based on the provided username and password.
	 *
	 * @param msg ArrayList of objects that contains the username and password of the user to retrieve.
	 * @return ArrayList of Objects that contains the user details 
	 * and the area if the user is DeliveryMan, AreaManager or MarketingWorker.
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 */
	public static ArrayList<Object> getUser(ArrayList<Object> msg) throws SQLException {
		Statement stmt;
		ArrayList<String> details = new ArrayList<>();
		details.add("users");
		details.add((String) msg.get(0));
		details.add((String) msg.get(1));
		ArrayList<Object> user = new ArrayList<>();
		String area = "";
		try {
			//
			if (checkIfExist(details)) {
				stmt = con1.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE username='" + details.get(1) + "' AND password='" + details.get(2) + "';");
				while (rs.next()) {
					String role = rs.getString(5);
					//
					if (rs.getString(5) == null)
						role = "NULL";
					else if (rs.getString(5) != null && (rs.getString(5).contains("AreaManager")||rs.getString(5).contains("MarketingWorker") || rs.getString(5).contains("DeliveryMan"))){

						String str = rs.getString(5);
						String[] parts = str.split("-");
						String part1 = parts[0];
						area = parts[1];
						role = parts[0];
					}

					User temp = new User(rs.getString(1), rs.getString(2),
							rs.getString(3), rs.getString(4), Roles.valueOf(role),
							rs.getString(6), rs.getString(7), rs.getString(8),
							Integer.parseInt(rs.getString(9)));
					user.add(temp);

				}
				rs.close();
			}
			else return null;
		} catch (SQLException e) {
			System.out.println("Importing user from has failed!");
		}
		user.add(area);
		//User temp = (User)user.get(0);
//		if (!(temp.getRole().toString().equals("NULL")))
//			updateIsLogin(temp.getUser_id());
		return user;
	}

	/**
	 * Loads subscriber information from the "subscribers" table in the database based on the provided user.
	 *
	 * @param user The user to load subscriber information for.
	 * @return ArrayList of Subscribe objects that contains the subscriber details.
	 * @throws SQLException if a database access error occurs or this method is called on a closed connection.
	 */
	public static ArrayList<Subscribe> loadSubscriber(User user) throws SQLException {
		Statement stmt;
		ArrayList<Subscribe> subscriber = new ArrayList<>();
		Subscribe sub= new Subscribe(user);
		try {
			stmt = con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM subscribers WHERE customer_id='" + sub.getUser_id() + "';");
			while (rs.next()){
				sub = new Subscribe(user,rs.getString(2),rs.getString(3));
			}
			rs.close();
		}catch (SQLException e) {
			System.out.println("Importing user from has failed!");
		}
		updateIsLogin(user.getUser_id());
		subscriber.add(sub);
		return subscriber;
	}

	/**
	 * Check if the data stored in ArrayList of Objects is exist in the current 
	 * table in the database.
	 *
	 * @param arr The ArrayList of objects to be check in the database.
	 * @return true if the data is not exist in the database, false otherwise.
	 */
	public static boolean checkIfExist(ArrayList<String> arr) {
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM "+ arr.get(0) +" WHERE username='" + arr.get(1) + "'";
			ResultSet rs = stmt.executeQuery(queryCheck);
			if (rs.absolute(1)) {
				return true;
			}
			rs.close();
		}catch (SQLException e) {System.out.println("exception");};
		return false;
	}
	
	/**
	 * This Method import user details to upgrade to customer
	 * 
	 * @param msg  -msg from client with id of user 
	 * @return User user object to be upgraded to customer
	 */
	public static User getUserDetail(ArrayList<Object> msg) {
	//if we need take one row from DB need use PrepareStatement class for stmt
		PreparedStatement stmt;
	    User userreturned =new User(null,null,null,null,null,null,null,null);
	    try {
	    	////////////
	        String query = "SELECT * FROM users WHERE user_id = ? ;";
	    	////
	        stmt = con1.prepareStatement(query);
	        stmt.setString(1, (String) msg.get(0));
	        ResultSet rs = stmt.executeQuery();
	        if(rs.next()) {
	        	String username = rs.getString("username");
	        	String password = rs.getString("password");
	        	String first_name = rs.getString("first_name");
	        	String last_name = rs.getString("last_name");
	        	String role = rs.getString("role");
				if (role == null)
					role = "NULL";
				else if (role != null && (role.contains("AreaManager")||role.contains("MarketingWorker") ||role.contains("DeliveryMan"))){
					String str = role;
					String[] parts = str.split("-");
					String part1 = parts[0];
					role = parts[0];
				}
	        	String user_id = rs.getString("user_id");
	        	String phone_number = rs.getString("phone_number");
	        	String email_address = rs.getString("email_address");
			    boolean isLoggedIn;
			    if(rs.getString("is_logged_in").equals("1")) {
			    	 	userreturned.setIsLoggedIn(true);}
			    else
			    { userreturned.setIsLoggedIn(false);}
			     
			    userreturned=new User(username,password, first_name, last_name, Roles.valueOf(role), user_id, phone_number, email_address);

	        }
	        	rs.close();
	 
		}catch(SQLException e) {
			System.out.println("Importing user info from has failed!");
			e.printStackTrace();
		}
		return userreturned;
	}
	
	/**
	 * This Method adds user to customer request table by customer service worker
	 * @param msg  -msg from client with customer 
	 * @return true if customer was not in the table before, false otherwise
	 */
	public static boolean addUserToCustomerRequest(ArrayList<Object> msg) {
		//user want to make customer add to table customer request
		PreparedStatement stmt;
		boolean flag=false;
		Customers customerrequest = (Customers)msg.get(0);
		try {
			if(!checkIfExistCustomerRequestForUser(msg)) {
				stmt = con1.prepareStatement("INSERT INTO customers_request VALUES (?,?,?,?,?,?)");
				stmt.setString(1, customerrequest.getUser_id()); 
				stmt.setString(2, customerrequest.getFirst_name()+ " "+customerrequest.getLast_name());
				stmt.setString(3, (String)msg.get(1)); 
				stmt.setString(4, customerrequest.getCredit_card_num()); 
				stmt.setString(5, customerrequest.getCredit_card_exp()); 
				stmt.setString(6, customerrequest.getCvv()); 
				stmt.executeUpdate();
				flag=true;
			}
		}catch(SQLException e) {e.printStackTrace();}
		return flag;
	}
	
	/**
	 * This method checks if user exists already in customer request table 
	 * @param arr ArrayList with user object to check
	 * @return true if customer exist in the table before, false otherwise
	 */
	public static boolean checkIfExistCustomerRequestForUser(ArrayList<Object> arr) {
		//check if have to this user customer request
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM customers_request WHERE user_id='" +((Customers)arr.get(0)).getUser_id() + "';";
			ResultSet rs = stmt.executeQuery(queryCheck); 
			if (rs.absolute(1)) {
				System.out.println("this user already exist in list request" );  
				return true;
			}
		}catch (SQLException e) {System.out.println("exception not exist customer");};
		return false;
	}
	
	/**
	 * This method upgrades customer to subscriber in DB
	 * adding to subscriber table and set in customer table is_subscribe = 1
	 * @param customer msg from client with customer 
	 */
	public static void updateCustomerToSubscriber(ArrayList<Object> customer) {
		for(int i=0;i<customer.size();i++) {
			ArrayList<Object> arrtemp=new ArrayList<>();
			arrtemp.add((Object)customer.get(i));
			if(checkIfNotExistCustomer(arrtemp)) {
			Customers editCustomer = (Customers)customer.get(i);
			String id = editCustomer.getCustomer_id();
			String query = "UPDATE customers SET is_subscriber=?  WHERE customer_id='" + id + "';";
			String query2 = "UPDATE users SET role=?  WHERE user_id='" + id + "';";
			try {
				PreparedStatement ps = con1.prepareStatement(query);
				PreparedStatement pss=con1.prepareStatement(query2);
				ps.setString(1, "1");
				ps.executeUpdate();
				pss.setString(1, "Subscriber");
				pss.executeUpdate();	
			} catch (SQLException e) {
				System.out.println("exception can not update sale");
				e.printStackTrace();
			}
		}
		else 
			System.out.println("the customer not found");
		}
	}
	
	/**
	 * This method checks if customer already exist in customers table
	 * @param arr ArrayList with user object to check
	 * @return true if customer exist in the table before, false otherwise
	 */
	public static boolean checkIfNotExistCustomer(ArrayList<Object> arr) {
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM customers WHERE customer_id='" +((Customers)arr.get(0)).getCustomer_id() + "';";
			ResultSet rs = stmt.executeQuery(queryCheck); 
			if (rs.absolute(1)) {
				System.out.println("the customer exist");
				return true;
			}
		}catch (SQLException e) {System.out.println("exception not exist customer");};
		return false;
	}
	
	/**
	 * This Method generate the new subscriber number
	 * @return String new subscriber number in DB
	 */
	public static String ReturnTheNextSubscriberNumber() {
		Statement stmt;
		int count=0;
		try {
			stmt=con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM subscribers;");
			while(rs.next()) {

				count++;
				}
		}
		catch(SQLException e) {
		System.out.println("Importing count from table has failed!");
		}
		if(count<10) {
			return "00"+Integer.toString(count+1);
		}
		else {
			if(count<100)
				return "0"+Integer.toString(count+1);
			else
				return Integer.toString(count+1);
		}	
	}
	
	/**
	 * This method saves customer into subscribe table after 
	 * customer service worker treatment
	 * @param arr - msg from client with customer object
	 */
	public static void saveNewSubscriberIntoDB(ArrayList<Object> arr) {
		PreparedStatement stmt;
		Subscribe newSub = (Subscribe)arr.get(0);
		newSub.setSub_num(ReturnTheNextSubscriberNumber());
		try {
			stmt = con1.prepareStatement("INSERT INTO subscribers VALUES (?,?,?,?)");
			stmt.setString(1, newSub.getCustomerID()); 
			stmt.setString(2, newSub.getSub_num()); 
			stmt.setString(3, newSub.getFirst_order()); 
			stmt.setString(4, newSub.getDelay_pay());
			stmt.executeUpdate();
		}catch(SQLException e) {e.printStackTrace();}
	}
	

	/**
	 * This method runs a SELECT * FROM ekrut.items_catalog query to the DB and returns an
	 * 
	 * @return ArrayList of the items in our catalog of our System
	 */
	public  ArrayList<ItemInCatalog> importCatalog() {	
		Statement stmt;
		ArrayList<ItemInCatalog> items_catalog = new ArrayList<>();
		try {
			stmt = con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM items_catalog;");
			while(rs.next()) {
				String productName = rs.getString("item_name");
				InputStream input = null;
				Object obj = new Object();
				try {
					input = this.getClass().getResourceAsStream("/images/" + productName +".png");
				} catch (Exception e) {
					//input = new FileInputStream("/styles/defaultProductImage.png");
					System.out.println(e);
					input = this.getClass().getResourceAsStream("/images/defaultProduct.png");

				}
				if(input == null)
				{
					input =  this.getClass().getResourceAsStream("/images/defaultProduct.png");
				}

				ByteArrayOutputStream output = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int n = 0;
				while (-1 != (n = input.read(buffer))) {
					output.write(buffer, 0, n);
				}
				byte[] imageBytes = output.toByteArray();

				items_catalog.add(new ItemInCatalog(rs.getString("item_code"), 
						rs.getString("item_name"), rs.getString("description"), rs.getFloat("item_price") , imageBytes));
			}	
		}catch(SQLException e) {
			System.out.println("Importing items catalog from DB has failed!");
			e.printStackTrace();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return items_catalog;
	}
	
	/**
	 * This method runs a SELECT * FROM ekrut.customers query to the DB and returns an
	 * 
	 * @return ArrayList of the card information according to a specific customer
	 */
	public static ArrayList<String> getCustomerCardInfoFormCustomersTable(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<String> CreditDetails = new ArrayList<>();
		CreditDetails.add((String) msg.get(0));     // id from user
		try {
			stmt=con1.prepareStatement("SELECT * FROM customers WHERE customer_id= ? ");
			stmt.setString(1, (String)msg.get(0));
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
					CreditDetails.add(rs.getString("credit_card_num"));
					CreditDetails.add(rs.getString("credit_card_exp"));
					CreditDetails.add(rs.getString("cvv"));
			}
			
		}catch(SQLException e) {
			System.out.println("Importing CustomerCardInfo from has failed!");
			e.printStackTrace();
		}
		return CreditDetails;
	}
	
	/**
	 * Updates the amount of items in the local machine specified in the order object.
	 * Also updates the is_under_alert, count_under_threshold, and count_unavailable fields in the 
	 * items_in_machines table based on the threshold level and the new amount of items in the machine.
	 * 
	 * @param order the order object containing the details of the items to be updated
	 * @return ArrayList of items that are under alert due to changes in their amount
	 */
	public static ArrayList<String> updateItemsInLoacalMachine(Order order) { 
		ArrayList<String> itemsUnderAlert = new ArrayList<>();
		ArrayList<ItemInCatalog> items_in_order = order.getItems_in_order(); 
		String machine_id = order.getMachine_id();
		int thresholdLvl;
		Statement stmt;
		PreparedStatement prstmt;
		try {
			stmt = con1.createStatement();
			String queryCheck = "SELECT threshold_level FROM ekrut.local_machines WHERE machine_id='"+machine_id+"'";
			ResultSet rs = stmt.executeQuery(queryCheck);
			rs.next();
			thresholdLvl = rs.getInt(1);
			
			prstmt = con1.prepareStatement("UPDATE ekrut.items_in_machines SET amount = amount-?, "
			        + "is_under_alert = IF(amount<"+thresholdLvl+",'1',is_under_alert), "
			        + "count_under_threshold = count_under_threshold + IF((amount + ?) >= ? AND amount< ?,1,0), "
			        + "count_unavailable = count_unavailable + IF(amount=0,1,0) "
			        + "WHERE machine_id ='"+machine_id+"' and item_code=?");
			for(ItemInCatalog item : items_in_order) {
				prstmt.setInt(1, item.getAmount_in_cart());
				prstmt.setInt(2, item.getAmount_in_cart());
				prstmt.setInt(3, thresholdLvl);
				prstmt.setInt(4, thresholdLvl);
				prstmt.setString(5, item.getItem_code()); 
				prstmt.executeUpdate();
				
				PreparedStatement selectStmt = con1.prepareStatement(
				        "SELECT item_name, amount FROM ekrut.items_in_machines WHERE machine_id ='"+machine_id+"' AND item_code =? AND is_under_alert <> IF(amount+?>'"+thresholdLvl+"',0,is_under_alert)");
				selectStmt.setString(1, item.getItem_code());
				String item_amount = String.valueOf(item.getAmount_in_cart());
				selectStmt.setString(2, item_amount);
				rs = selectStmt.executeQuery();
				while (rs.next()) {
				 itemsUnderAlert.add(rs.getString("item_name"));
				 itemsUnderAlert.add(rs.getString("amount"));
				}
			}
		}catch(SQLException e) {e.printStackTrace();}
		return itemsUnderAlert;
	}
	
	/**
	 * Saves the details of the order to the orders_local table or orders_remote table in a database.
	 * 
	 * @param order the order object containing the details of the order to be saved
	 * @return ArrayList that contains the new order code for this entry
	 */
	public static ArrayList<String> saveOrderToDB(Order order) {
		// need to figure out which kind of order this is
		String table;
		String query = null;
		if (order instanceof RemoteOrder) {
			RemoteOrder remoteOrder = (RemoteOrder) order;
			table = "orders_remote";
			switch (remoteOrder.getOrder_type()) {
			case "REMOTE_DELIVERY":
				query = "INSERT INTO ekrut.orders_remote (order_code, customer_id, "
						+ "order_type, machine_area, items, final_price, status, order_confirmed_date, "
						+ "delivery_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
				break;
			case "REMOTE_PICKUP":
				query = "INSERT INTO ekrut.orders_remote (order_code, customer_id, "
						+ "order_type, machine_id, machine_area, items, final_price, status, order_confirmed_date"
						+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
				break;
			default:
				break;
			}
		}
		else
			table = "orders_local";
		
		
		// making items --> long string
		ArrayList<String> newOrderCode = new ArrayList<>();
		PreparedStatement stmt;
		Statement stmt1;
		String items="";
		int i;
		ItemInCatalog item;
		
		for(i=0;i<order.getItems_in_order().size()-1;i++) {
			item = order.getItems_in_order().get(i);
			items+=item.getItem_name()+","+item.getAmount_in_cart()+","; 
		}
		item = order.getItems_in_order().get(i);
		items+=item.getItem_name()+","+item.getAmount_in_cart();
		
		// Get the number of entries in the orders_local table or orders_remote table
		int count=0;
		try {
			stmt1=con1.createStatement();
			ResultSet rs = stmt1.executeQuery("SELECT * FROM "+table+";");
			while(rs.next()) {
				count++;
				}
		} catch (SQLException e) {
		    e.printStackTrace();
		}
		
		// Set the order_code as the number of entries + 1
		String order_code = String.valueOf(count + 1);
		newOrderCode.add(order_code);
		
		if (order instanceof RemoteOrder) {
			RemoteOrder remoteOrder = (RemoteOrder) order;
			switch (remoteOrder.getOrder_type()) {
			case "REMOTE_DELIVERY":
				try (PreparedStatement stmt2=con1.prepareStatement(query)) {
			        stmt2.setString(1, order_code);
			        stmt2.setString(2, remoteOrder.getCustomer_id());
			        stmt2.setString(3, remoteOrder.getOrder_type());
			        stmt2.setString(4, remoteOrder.getMachine_area());
			        stmt2.setString(5, items);
			        stmt2.setFloat(6, remoteOrder.getFinal_price());
			        stmt2.setString(7, remoteOrder.getStatus());
			        stmt2.setString(8, remoteOrder.getOrder_confirmed_date());
			        stmt2.setString(9, remoteOrder.getDelivery_address());
			        stmt2.executeUpdate();
			    } catch (SQLException e) 
				{e.printStackTrace();}
				break;
			case "REMOTE_PICKUP":
				try (PreparedStatement stmt2=con1.prepareStatement(query)) {
			        stmt2.setString(1, order_code);
			        stmt2.setString(2, remoteOrder.getCustomer_id());
			        stmt2.setString(3, remoteOrder.getOrder_type());
			        stmt2.setString(4, remoteOrder.getMachine_id());
			        stmt2.setString(5, remoteOrder.getMachine_area());
			        stmt2.setString(6, items);
			        stmt2.setFloat(7, remoteOrder.getFinal_price());
			        stmt2.setString(8, remoteOrder.getStatus());
			        stmt2.setString(9, remoteOrder.getOrder_confirmed_date());
			        stmt2.executeUpdate();
			    } catch (SQLException e) 
				{e.printStackTrace();}
				break;
			default:
				break;
			}
		}
		else {
			// making ArrayList to enter orders_local
			ArrayList<String> orderList = new ArrayList<>();
			orderList.add(order_code);
			orderList.add(order.getCustomer_id()); 
			orderList.add(order.getMachine_id());
			orderList.add(items);
			String final_priceSTR = String.valueOf(order.getFinal_price());
			orderList.add(final_priceSTR);//price type -> float
			orderList.add(order.getOrder_confirmed_date()); 
			
			try {
				stmt=con1.prepareStatement("INSERT INTO ekrut.orders_local VALUES (?,?,?,?,?,?)");
				for (i=0;i<6;i++) {
					stmt.setString(i+1, orderList.get(i));
				}
				stmt.executeUpdate();
			}catch(SQLException e) {e.printStackTrace();}
		}	
		return newOrderCode;
	}
	
	/**
	 * Retrieves a list of areas, locations, and machine IDs from the 'local_machines' table in the 'ekrut' database.
	 *
	 * @return An ArrayList containing the areas, locations, and machine IDs, in that order.
	 * @throws SQLException if a database error occurs while executing the query.
	 */
	public static ArrayList<String> getAreaLocationsList() {  
		ArrayList<String> areaLocationsList = new ArrayList<>();
		String query = "SELECT area, location, machine_id FROM ekrut.local_machines";
		try {
			Statement stmt = con1.createStatement();
		    ResultSet resultSet = stmt.executeQuery(query);
		    while (resultSet.next()) {
		      String area = resultSet.getString("area");
		      String location = resultSet.getString("location");
		      String machine_id = resultSet.getString("machine_id");
		      areaLocationsList.add(area);
		      areaLocationsList.add(location);
		      areaLocationsList.add(machine_id);
		    }
		  }catch(SQLException e) {e.printStackTrace();}
		  return areaLocationsList;
		}
	
	/**
	 * This method import detail of customer on click search in 
	 * Customer service worker treatment in customer
	 * @author G-10
	 * @param customerID msg from client with id of the customer
	 */
	public static ArrayList<Customers> importCostumer(ArrayList<Object> customerID) {
		ArrayList<Customers> customer = new ArrayList<>();
		try {	
			User user=getUserDetail(customerID);
			String id=((String)customerID.get(0));
			Statement stmt;
			String query = "SELECT * FROM customers WHERE customer_id='" + id + "';";
	        stmt = con1.createStatement();
	        ResultSet rs = stmt.executeQuery(query);
			if(rs.next()) {
				ArrayList<String> sub = new ArrayList<String>();
				for(int i=0;i<5;i++) {
					sub.add(rs.getString(i+1));
				}
				boolean is_sub=false;
				if(sub.get(4).equals("1"))
					is_sub=true;
				customer.add(new Customers(user,sub.get(1),sub.get(2),sub.get(3),is_sub));
			}	
			else {
				System.out.println("The user in not found!");
				customer.add(new Customers("0000","0000","0000",null,null,null,"0000","0000","0000","0000","0000",false));
			}
		}catch(SQLException e) {
			System.out.println("Importing customer from has failed!");
		}
		return customer;
	}

	/**
	 * This method present to the manager all the sale that exists in DB
	 * customer service worker treatment in customer
	 * @return ArrayList of Sales
	 */
	public  ArrayList<Sale> PresentSales() {//Present All sales
		Statement stmt;
		ArrayList<Sale> sales = new ArrayList<>();
		try {//
			stmt=con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM deals;");
			while(rs.next()) {
				ArrayList<String> sub = new ArrayList<String>();
				for(int i=0;i<6;i++) {
					sub.add(rs.getString(i+1));
				}
				HashMap<String, String> buildhashmap=new HashMap<>();
		        String[] strbuild = sub.get(5).split(",");//split the string of area status from data base
		        for(int i=0;i<strbuild.length;i=i+2) {
		        	buildhashmap.put(strbuild[i],strbuild[i+1]);//build hash map from string of areas cloumn in deals table 
		        }
				String saleName = rs.getString("discount") + "%";
				InputStream input = null;
				Object obj = new Object();
				try {
					input = this.getClass().getResourceAsStream("/images/" + saleName +".png");
				} catch (Exception e) {
					//input = new FileInputStream("/styles/defaultProductImage.png");
					System.out.println(e);
					input = this.getClass().getResourceAsStream("/images/defaultProduct.png");

				}
				if(input == null)
				{
					input =  this.getClass().getResourceAsStream("/images/defaultProduct.png");
				}

				ByteArrayOutputStream output = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int n = 0;
				while (-1 != (n = input.read(buffer))) {
					output.write(buffer, 0, n);
				}
				byte[] imageBytes = output.toByteArray();
				sales.add(new Sale(sub.get(0),sub.get(1),sub.get(2),sub.get(3),sub.get(4),buildhashmap,imageBytes));
			}
			rs.close();
		}catch(SQLException | IOException e) {
			System.out.println("Importing sales from has failed!");
		}
		return sales;
	}
	
	/**
	 * This method counts the number rows and return the next id that available
	 * @return String of new sale_id
	 */
	public static String ReturnTheNextIdToSales() {
		Statement stmt;
		int count=0;
		try {
			stmt=con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM deals;");
			while(rs.next()) {
				count++;
				}
			rs.close();
		}
		catch(SQLException e) {
		System.out.println("Importing count from table has failed!");
		}
		if(count<10) {
			return "0"+Integer.toString(count+1);
		}
		else {	
		return Integer.toString(count+1);
		}	
	}
	
	/**
	 * This method updates the sale status in specific area
	 * (Acitve or Not Active) by Marketing Manager 
	 * @param client msg from client with list of sales from manager
	 */
	public static void updateSaleStatusArea(ArrayList<Object> client) {
		for(int i=0;i<client.size();i++) {
			//
			ArrayList<Object> arrtemp=new ArrayList<>();
			arrtemp.add((Object)client.get(i));
			if(checkIfNotExistSale(arrtemp)) {
			
			Sale editSale = (Sale)client.get(i);
			String id = editSale.getSaleid();
			
			String query = "UPDATE deals SET deal_in_areas=?  WHERE deal_id='" + id + "';";
			try {//
				PreparedStatement ps = con1.prepareStatement(query);
				
				ps.setString(1, editSale.getMapStringArea());
				//ps.setString(7, editSale.getImgURL());
				ps.executeUpdate();
			} catch (SQLException e) {
				System.out.println("exception can not update sale");
				e.printStackTrace();
				
			}
		}
		else 
			System.out.println("sale is not found");
		}
	}
  
	/**
	 * This method checks if sale exists before the change of status
	 * @param arr msg from client with id of the sale
	 */
	public static boolean checkIfNotExistSale(ArrayList<Object> arr) {
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM deals WHERE deal_id='" +((Sale)arr.get(0)).getSaleid() + "';";
			ResultSet rs = stmt.executeQuery(queryCheck); 
			if (rs.absolute(1)) {
				System.out.println("the sale exist");
				return true;
			}
		}catch (SQLException e) {System.out.println("exception not exist sale");};
		return false;
	}
	
	/**
	* Saves a new Sale object into the database, added by the manager.
	* @param arr an ArrayList containing the Sale object to be saved
	*/
	public static void saveNewSaleIntoDB(ArrayList<Object> arr) {
		PreparedStatement stmt;
		Sale editSale = (Sale)arr.get(0);
		editSale.setSaleid(ReturnTheNextIdToSales());
		try {
			stmt = con1.prepareStatement("INSERT INTO deals VALUES (?,?,?,?,?,?)");
			stmt.setString(1, editSale.getSaleid()); 
			stmt.setString(2, editSale.getName()); 
			stmt.setString(3, editSale.getPercentage()); 
			stmt.setString(4, editSale.getDescription()); 
			stmt.setString(5, editSale.getHours()); 
			
			String areainfo=editSale.getMapStringArea();
			stmt.setString(6, areainfo);
			stmt.executeUpdate();
		}catch(SQLException e) {e.printStackTrace();}
	}
	
	/**
	 * Retrieve an inventory report from the "monthly_inventory_reports" table 
	 * in the database based on the provided month, year and machine_id.
	 *
	 * @param msg ArrayList of objects that contains the InventoryReport object to retrieve.
	 * @return ArrayList of Objects that contains the inventory report details.
	 */
	public static ArrayList<Object> getInventoryReport(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<Object> inventoryReport= new ArrayList<>();
		if (msg.get(0) instanceof InventoryReport) {
			InventoryReport inventory = (InventoryReport) msg.get(0);
			try {
				String query = "SELECT * FROM ekrut.monthly_inventory_reports WHERE month = ? AND year = ? AND machine_id = ?;";
				stmt = con1.prepareStatement(query);
				stmt.setString(1, inventory.getMonth());
				stmt.setString(2, inventory.getYear());
				stmt.setString(3, inventory.getMachine_id());
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					inventory.generateItemsHashmap(rs.getString(3));
					inventory.generateItemsBelowThreshold(rs.getString(4));
					inventory.generateUnavailableItems(rs.getString(5));
				}
				ResultSet level = stmt.executeQuery("SELECT threshold_level FROM local_machines WHERE machine_id='" + inventory.getMachine_id() + "';");
				while (level.next())
					inventory.setThreshold_level(Integer.parseInt(level.getString(1)));
				rs.close();
			} catch (SQLException e) {
				System.out.println("Importing Inventory report has failed!");
			}
			inventoryReport.add(inventory);
		}
		return inventoryReport;
	}

	/**
	 * Retrieve a list of machines from the "local_machines" table in the database based on the provided area.
	 *
	 * @param msg ArrayList of objects that contains the area to retrieve the machine list for.
	 * @return ArrayList of Strings that contains the machine id and location of the machines.
	 */
	public static ArrayList<String> getMachineList(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<String> machineList= new ArrayList<>();
		if (msg.get(0) instanceof String) {
			String machine = (String) msg.get(0);
			try {
				String query = "SELECT * FROM local_machines WHERE area = ?;";
				stmt = con1.prepareStatement(query);
				stmt.setString(1, machine);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					machineList.add(rs.getString("machine_id"));
					machineList.add(rs.getString("location"));
				}
				rs.close();
			} catch (SQLException e) {
				System.out.println("Importing Machine List has failed!");
			}
		}
		return machineList;
	}
	
	/**
	 * Update the is_logged_in status of a user in the "users" table of the database.
	 *
	 * @param userId The user_id of the user to update.
	 */
	public static void UpdateUserIsLoginStatus(String userId) throws SQLException {
		String query = "UPDATE users SET is_logged_in= ? WHERE user_id='" + userId + "'";
		PreparedStatement preparedStatement = con1.prepareStatement(query);
		preparedStatement.setString(1, "0");
		preparedStatement.executeUpdate();
	}
	
	/**
	*Returns a list of ItemInMachine objects for a given machine ID.
	*
	*@param machine_id the ID of the machine to get items for
	*@return a list of ItemInMachine objects for the given machine ID
	*/
	public static ArrayList<ItemInMachine> getItemsInMachine(String machine_id) {	
		Statement stmt;
		ArrayList<ItemInMachine> items_in_machine = new ArrayList<>();
		try {
			stmt = con1.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT item_code, item_name, amount FROM items_in_machines WHERE machine_id='" + machine_id + "';");
			while(rs.next()) {
				items_in_machine.add(new ItemInMachine(rs.getString("item_code"),rs.getString("item_name"),rs.getInt("amount")));
			}	
		}catch(SQLException e) {
			System.out.println("Importing form items_in_machines has failed!");
			e.printStackTrace();
		}	
		return items_in_machine;
	}
	
	/**
	 * Add an alert to the "items_alerts" table of the database.
	 *
	 * @param alert The alert to be added to the database.
	 */
	public static void addAlertToDB(ItemsAlert alert) {
		PreparedStatement stmt1;
		PreparedStatement stmt2;
		PreparedStatement stmt3;
		int count = 0;
		String manager_username = null;
		String machine_id = alert.getMachine_id();
		
		// Get the number of entries in the items_alerts table	
		try {
			stmt1 = con1.prepareStatement("SELECT * FROM items_alerts;");
		    ResultSet rs = stmt1.executeQuery();
		    while(rs.next()) {
		      count++;
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Set the alert_id as the number of entries + 1
		String alert_id = String.valueOf(count + 1);
		alert.setAlert_id(alert_id);
		
		// get the manager_username from local_machines table
		try {
			stmt2 = con1.prepareStatement("SELECT manager_username FROM local_machines WHERE machine_id = ?");
		    stmt2.setString(1, machine_id);
		    ResultSet rs = stmt2.executeQuery();
			while(rs.next()) {
				manager_username = rs.getString("manager_username");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// set the manager_username in to alert
		alert.setManager_username(manager_username);
		
		String query = "INSERT INTO items_alerts (alert_id, machine_id, date, status, items_in_alert,"
				+ " manager_username) VALUES (?,?,?,?,?,?)";
		try {
			stmt3=con1.prepareStatement(query);
			stmt3.setString(1,alert.getAlert_id());
			stmt3.setString(2,alert.getMachine_id());
			stmt3.setString(3,alert.getDate());
			stmt3.setString(4,alert.getStatus().name());
			stmt3.setString(5,alert.getItems_in_alert());
			stmt3.setString(6,alert.getManager_username());
			stmt3.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a boolean indicating whether the subscriber with the specified ID has made their first order.
	 *
	 * @param userID the ID of the customer to check
	 * @return true if the customer has made their first order, false otherwise
	 */
	public static boolean hasMadeFirstOrder(String userID) {
		String query = "SELECT made_first_order FROM subscribers WHERE customer_id = ?";
		try {
			PreparedStatement pstmt = con1.prepareStatement(query);
			pstmt.setString(1, userID);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getBoolean("made_first_order");
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Updates the made_first_order field in the subscribers table to 1 for the specified customer.
	 *
	 * @param userID the ID of the customer to update
	 */
	public static void updateMadeFirstOrder(String userID) {
	    String query = "UPDATE subscribers SET made_first_order = 1 WHERE customer_id = ?";
	    try {
	        PreparedStatement pstmt = con1.prepareStatement(query);
	        pstmt.setString(1, userID);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	/**
	 * Returns a boolean indicating whether the subscriber with the specified ID has chose delayed payment method.
	 *
	 * @param userID the ID of the customer to check
	 * @return true if the customer has chose delayed payment, false otherwise
	 */
	public static boolean hasDelayPayment(String userID) {
		String query = "SELECT is_delayed_payment FROM subscribers WHERE customer_id = ?";
		try {
			PreparedStatement pstmt = con1.prepareStatement(query);
			pstmt.setString(1, userID);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getBoolean("is_delayed_payment");
	        }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Updates the is_delayed_payment field for the subscriber with the specified user ID.
	 * If the field is currently 0, it will be updated to 1. If it is currently 1, it will be updated to 0.
	 *
	 * @param userID the ID of the subscriber to update
	 */
	public static void updateDelayPayment(String userID) {
	    String query = "UPDATE subscribers SET is_delayed_payment = NOT is_delayed_payment WHERE customer_id = ?";
	    try {
	        PreparedStatement pstmt = con1.prepareStatement(query);
	        pstmt.setString(1, userID);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	/**
	* This method is used to get a list of alerts from the database for a specific manager.
	* The method will retrieve all alerts from the "items_alerts" table 
	* where the manager_username is the same as the input 
	* and the status of the alert is "ACTIVE" or "IN_PROGRESS".
	* The method returns an ArrayList of Strings that contains 
	* the alert_id, machine_id, date, and status of all the alerts.
	* @param msg - an ArrayList of Objects, where the first object should be the manager's username
	* @return ArrayList of Strings that contains the alert_id, machine_id, date, and status 
	* of all the alerts for the specific manager.
	*/
	public static ArrayList<String> getAlertsTableForManager(ArrayList<Object> msg) {  
		PreparedStatement stmt;
		String machine_tmp_id = null;
		String alert_id = null;
		String date = null;
		String status = null;		
		ArrayList<String> alertsFromDB = new ArrayList<>();  
		try {
			stmt=con1.prepareStatement("SELECT alert_id, machine_id, date, status FROM ekrut.items_alerts WHERE manager_username=? and ( status = 'ACTIVE' or status = 'IN_PROGRESS' );"); // and manager_username = ?
			stmt.setString(1, (String)msg.get(0)); 
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				 alert_id = rs.getString("alert_id");
				 machine_tmp_id = rs.getString("machine_id");
				 date = rs.getString("date");
				 status = rs.getString("status");
				if (!(alertsFromDB.contains(machine_tmp_id))) {
					alertsFromDB.add(alert_id);
					alertsFromDB.add(machine_tmp_id);
					alertsFromDB.add(date);
					alertsFromDB.add(status);
				}
			}
				
		}catch(SQLException e) {
			System.out.println("Importing CustomerCardInfo from has failed!");
			e.printStackTrace();
		}
		return alertsFromDB;
	}
	
	/**
	* This method updates the status of an alert to "IN_PROGRESS" and 
	* assigns a worker to it when a Manager clicks the "Send" button 
	* on the alert in the alert's table.
	* @param msg The ArrayList that contains the worker's username as 
	* the first element and the machine's id as the second element.
	* @return ArrayList<String> returns an ArrayList of string 
	* with a message indicating if the update was successful or not.
	*/
	public static ArrayList<String> updateAfterManagerPressedSendBtn(ArrayList<Object> msg) {
		ArrayList<String> returnedStringToServer = new ArrayList<>();
		String query = "UPDATE items_alerts SET worker_username=?, status='IN_PROGRESS' WHERE (status = 'ACTIVE' and machine_id='" + msg.get(2) + "');"; //whichMachineId
		try {
			PreparedStatement ps = con1.prepareStatement(query);
			ps.setString(1, (String) msg.get(1)); 
			ps.executeUpdate();
		} catch (SQLException e) {System.out.println("exception");
		System.out.println("alert ID is not found!!");
		returnedStringToServer.add("Could Not Update Alert!");
		return returnedStringToServer;
		}	
		returnedStringToServer.add("Updated Alert Successfully");
		return returnedStringToServer;
	}
	
	/**
	This method is used to import all the remote orders made by a user 
	from a specific machine from the database.
	@param user_id The ID of the user making the remote order.
	@param machine_id The ID of the machine the order was made from.
	@return An ArrayList of Strings containing the order code and final price 
	of the remote orders.
	*/
	public static ArrayList<String> importUserRemoteOrders (String user_id, String machine_id) {
		ArrayList<String> remote_orders = new ArrayList<>();
		String order_type = "REMOTE_PICKUP";
        String status = "ACTIVE";
        
        String query = "SELECT order_code, final_price FROM orders_remote " +
                "WHERE customer_id = ? AND machine_id = ? AND order_type = ? AND status = ?";
        try (PreparedStatement stmt = con1.prepareStatement(query)) {
            stmt.setString(1, user_id);
            stmt.setString(2, machine_id);
            stmt.setString(3, order_type);
            stmt.setString(4, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String orderCode = rs.getString("order_code");
                    float finalPrice = rs.getFloat("final_price");
                    String str_price = ""+finalPrice;
                    remote_orders.add(orderCode);
                    remote_orders.add(str_price);
                }
            }
		}catch(SQLException e) {
			System.out.println("Importing items catalog from DB has failed!");
			e.printStackTrace();
		}	
		return remote_orders;
	}
	
	/**
	* This method updates the order_received_date and status 
	* of a remote order in the orders_remote table in the database.
	* @param order_code The order code of the remote order to update.
	* @param time The time that the order was received in the format "yyyy-MM-dd HH:mm:ss"
	*/
	public static void updateRemoteOrderReceiveTime (String order_code, String time) {
	    String status = "DONE";
	    int affectedRows = 0;
	    String query = "UPDATE orders_remote SET order_received_date = ?, status = ? WHERE order_code = ?";
	    try (PreparedStatement stmt = con1.prepareStatement(query)) {
	        stmt.setString(1, time);
	        stmt.setString(2, status);
	        stmt.setString(3, order_code);
	        affectedRows = stmt.executeUpdate();
	    } catch (SQLException e) {
	        System.out.println("Updating order_received_date and status has failed!");
	        e.printStackTrace();
	    }   
	    if(affectedRows > 0) {
	        System.out.println("order_received_date and status updated successfully for order_code " + order_code);
	    } else {
	        System.out.println("Error updating order_received_date and status for order_code " + order_code);
	    }
	}
	
	/**
	* Returns a list of ItemsAlert objects that are in progress and 
	* assigned to the given worker username.
	* @param username the worker username to get alerts for
	* @return a list of link ItemsAlert objects that are in progress 
	* and assigned to the given worker username
	*/
	public static ArrayList<ItemsAlert> getISWalerts(String username){
		ArrayList<ItemsAlert> iswAlerts = new ArrayList<>();
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM ekrut.items_alerts WHERE status='IN_PROGRESS' and worker_username='"+username+"'";
			ResultSet rs = stmt.executeQuery(queryCheck);
			while(rs.next()) {
				ItemsAlert temp = new ItemsAlert(rs.getString(1),rs.getString(2),rs.getString(3),AlertStatus.valueOf(rs.getString(4)),rs.getString(5),rs.getString(6),rs.getString(7));
				//checks if machine have been imported to this array
				if(!existsAlert(iswAlerts,temp)) {
					iswAlerts.add(temp);
				}
			}
		}catch(SQLException e) {e.printStackTrace();}
		return iswAlerts;
	}
	
	/**
	* Check if an alert already exists in a list of alerts
	* @param alerts The list of alerts to check
	* @param alert The alert to check if it exists in the list
	* @return true if the alert already exists in the list, false otherwise
	*/
	public static boolean existsAlert(ArrayList<ItemsAlert> alerts, ItemsAlert alert) {
		for(ItemsAlert a : alerts) {
			if(alert.getMachine_id().equals(a.getMachine_id())) {
				return true;
			}
		}
		return false;
	}	

	/**
	 * Returns the threshold level for a given machine.
	 *
	 * @param machine_id The ID of the machine to get the threshold for.
	 * @return The threshold level for the given machine.
	 * @throws SQLException if there is an error executing the SQL query.
	 */
	public static int getThreshold(String machine_id) {
		Statement stmt;
		int threshold=0;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT threshold_level FROM ekrut.local_machines WHERE machine_id='"+machine_id+"'";
			ResultSet rs = stmt.executeQuery(queryCheck);
			rs.next();
			threshold = rs.getInt(1);
			rs.close();
		}catch(SQLException e) {e.printStackTrace();}
		return threshold;
	}
	
	/**
	 * Updates the amount of items in the specified machine, and also updates the "is_under_alert" field if necessary.
	 * 
	 * @param afterupdate an ArrayList of ItemInMachine objects representing the updated amount of items in the machine
	 * @param machine_id the ID of the machine
	 * @param thresholdLvl the threshold level for determining if the machine is under alert
	 */
	public static void ISWUpdate(ArrayList<ItemInMachine> afterupdate,String machine_id,int thresholdLvl) {
		PreparedStatement stmt;
		try {
			stmt=con1.prepareStatement("UPDATE ekrut.items_in_machines SET amount = ?, is_under_alert = IF(amount>'"+thresholdLvl+"','0',is_under_alert) WHERE machine_id='"+machine_id+"' and item_name=?");
			for(ItemInMachine item : afterupdate) {
				stmt.setInt(1, item.getItem_amount_in_machine());
				stmt.setString(2, item.getItem_name());
				stmt.executeUpdate();
			}
		}catch(SQLException e) {e.printStackTrace();}
	}
	
	/**
	 * Updates the status of alerts for the specified machine to "DONE".
	 * 
	 * @param machine_id the ID of the machine
	 */
	public static void ISWUpdateAlertStatus(String machine_id) {
		PreparedStatement prstmt;
		try {
			
			prstmt = con1.prepareStatement("UPDATE ekrut.items_alerts SET status='DONE' WHERE machine_id='"+machine_id+"';");
			prstmt.executeUpdate();
			
			
		}catch(SQLException e) {e.printStackTrace();}
	}

	/**
	* This method is used to get the delivery orders 
	* from the orders_remote table in the database.
	* It returns an ArrayList of objects, where each object is an instance 
	* of the RemoteOrder class representing a delivery order.
	* @param msg an ArrayList of objects, where the first object should be 
	* a string representing the area.
	* @return an ArrayList of objects, where each object is an instance of the RemoteOrder class representing a delivery order.
	*/
	public static ArrayList<Object> getDeliveryOrders(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<Object> deliveryOrders= new ArrayList<>();
		if (msg.get(0) instanceof String) {

			try {
				String area = (String) msg.get(0);
				String query = "SELECT * FROM ekrut.orders_remote WHERE machine_area=? AND order_type='REMOTE_DELIVERY' AND status != 'DONE';";
				stmt = con1.prepareStatement(query);
				stmt.setString(1, area);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					RemoteOrder temp = new RemoteOrder(rs.getString("order_code"),rs.getString("customer_id"), rs.getString("order_confirmed_date"),
							rs.getString("status"), rs.getString("estimated_delivery_date"),rs.getString("order_received_date"), rs.getString("delivery_address"));
					deliveryOrders.add(temp);
				}
				rs.close();
			} catch (SQLException e) {
				System.out.println("Importing Machine List has failed!");
			}
		}
		return deliveryOrders;
	}

	/**
	 * Returns a CustomerActivityReport object based on the given month, year, and area. The report contains
	 * data from the "monthly_customeractivity_reports" table in the database, including a map of the items
	 * in the report and their counts, the area, the date, and counts for three other fields.
	 *
	 * @param msg A list containing the month, year, and area to get the report for.
	 * @return A CustomerActivityReport object with the data for the given month, year, and area.
	 */
	public static ArrayList<Object> getCustomerActivityLevelByOrders(ArrayList<Object> msg) {
		PreparedStatement stmt;
		ArrayList<Object> activityReportData = new ArrayList<>();
		String month = (String) msg.get(0);
		String year = (String) msg.get(1);
		String Area = (String) msg.get(2);
		int count1 = 0, count2 = 0, count3 = 0;
		HashMap<String, Integer> map_ofItemsRange = new LinkedHashMap<String,Integer>(); //HashMap for "item_in_report" column

		ArrayList<CustomerActivityReport> ActivityCustomeReport = new ArrayList<>();
		String date = null;
		try {
			stmt = con1.prepareStatement("SELECT * FROM ekrut.monthly_customeractivity_reports WHERE month= ? AND year= ? AND Area= ?");
			stmt.setString(1, month);
			stmt.setString(2, year);
			stmt.setString(3, Area);

			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ArrayList<String> ActivityReportDetails = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					ActivityReportDetails.add(rs.getString(i + 1));
				}
				String[] strbuild1 = null;
				strbuild1 = ActivityReportDetails.get(2).split(",");
				for (int i = 0; i < strbuild1.length-1; i = i + 2) {
					map_ofItemsRange.put(strbuild1[i+1], Integer.parseInt(strbuild1[i]));
				}
				date = month + "/"+year;
			}
		} catch (SQLException e) {
			System.out.println("Importing Orders Report has failed!");
		}
		activityReportData.add(new CustomerActivityReport(map_ofItemsRange,Area, date, count1, count2, count3));
		return activityReportData;
	}
	
	/**
	* This method checks if a report exists in the specified table in the database
	* @param arr an ArrayList that contains the following: name of table, year and month of the report
	* @return true if the report exists in the table and false if it does not
	*/
	public static boolean checkIfExistReport(ArrayList<String> arr) {
		Statement stmt;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM "+ arr.get(0) +" WHERE year='" + arr.get(1) + "' AND month='" + arr.get(2) + "'";
			ResultSet rs = stmt.executeQuery(queryCheck);
			if (rs.absolute(1)) {
				return true;
			}
			rs.close();
		}catch (SQLException e) {System.out.println("exception");};
		return false;
	}
	
	/**
	* This method generates reports for inventory, customer activity and orders for a specific month and year.
	* @param currentYear the year of the report
	* @param currentMonth the month of the report
	* @return a MsgHandler object indicating whether the generation of the reports was successful or not
	*/
	public static MsgHandler<Object> generateReports(int currentYear, int currentMonth){
		ArrayList<String> typeReport = new ArrayList<>(Arrays.asList("monthly_inventory_reports","monthly_customeractivity_reports","monthly_orders_reports"));
		for (String report: typeReport) {
			if (checkIfExistReport(new ArrayList<>(Arrays.asList(report,String.valueOf(currentYear),String.valueOf(currentMonth)))))
				return new MsgHandler<>(TypeMsg.AlreadyExsit,null);
		}
		try {
			prepareInventoryRow(currentYear, currentMonth);
			clearInventoryData();
			prepareCustomerActivityRow(currentYear, currentMonth);
			generateOrdersReports(currentYear, currentMonth);
			return new MsgHandler<>(TypeMsg.GenerateSuccessfully,null);
		}catch (Exception e){
			return new MsgHandler<>(TypeMsg.GenerateFailed,null);
		}
	}
	
	/**
	* This method prepares the customer activity report for a specific area for a specific month and year.
	* It retrieves the machine list for the area and then queries the orders_local table to get all the orders
	* that were placed on the machines in the area for the specific month and year. It then counts the number of orders
	* placed by each customer and saves this information in a HashMap.
	* Finally, the method saves the report in the monthly_customeractivity_reports table in the database.
	* @param currentYear the year for which the report is being generated
	* @param currentMonth the month for which the report is being generated
	*/
	private static void prepareCustomerActivityRow(int currentYear, int currentMonth) {
		ArrayList<String> areaList = new ArrayList<>(Arrays.asList("North","South","UAE"));
		for (String area:areaList) {
			ArrayList<String> reportRowDetails = new ArrayList<>();
			ArrayList<String> machine_id = getMachineList(new ArrayList<>(Arrays.asList(area)));
			HashMap<String,Integer> details = new HashMap<>();

			for (int i=0;i<machine_id.size();i+=2) {
				String machine = machine_id.get(i);
				int count = 0;
				String query = "SELECT * FROM orders_local WHERE machine_id ='" + machine + "'";
				try (PreparedStatement stmt = con1.prepareStatement(query)) {
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						String customer_id = rs.getString("customer_id");
						String[] date = rs.getString("order_confirmed_date").split("-");
						Integer row_year = Integer.parseInt(date[0]);
						Integer row_month = Integer.parseInt(date[1]);
						if (row_year.equals(currentYear) && row_month.equals(currentMonth)) {
							if (details.containsKey(customer_id)) {
								int cnt = details.get(customer_id);
								details.replace(customer_id, cnt+1);
							}
							else{details.put(customer_id, count+1);}
						}
					}
				}catch (SQLException e) {
					System.out.println("Generate inventory report failed!");
					e.printStackTrace();
				}
				String query_remote = "SELECT * FROM orders_remote WHERE machine_id ='" + machine + "'";
				try (PreparedStatement stmt = con1.prepareStatement(query_remote)) {
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						String customer_id = rs.getString("customer_id");
						String[] date = rs.getString("order_confirmed_date").split("-");
						Integer row_year = Integer.parseInt(date[0]);
						Integer row_month = Integer.parseInt(date[1]);
						if (row_year.equals(currentYear) && row_month.equals(currentMonth)) {
							if (details.containsKey(customer_id)) {
								int cnt = details.get(customer_id);
								details.replace(customer_id, cnt+1);
							}
							else{details.put(customer_id, count+1);}
						}
					}
				}catch (SQLException e) {
					System.out.println("Generate inventory report failed!");
					e.printStackTrace();
				}
			}
			reportRowDetails.add(area);
			reportRowDetails.add(rangeCountCustomer(details));
			reportRowDetails.add(String.valueOf(currentMonth));
			reportRowDetails.add(String.valueOf(currentYear));
			saveCustomerActivityReport(reportRowDetails);
		}
	}
	
	/**
	* This method takes in a HashMap of customer details and returns a string of the range of customers and the count in each range.
	* The ranges are as follows: 1-9, 10-15, 16-20, 21+.
	* @param details A HashMap containing customer details with the key as the customer's name and value as the customer's age.
	* @return A string containing the count and range of customers, separated by commas.
	*/
	private static String rangeCountCustomer(HashMap<String, Integer> details) {
		Map<String,Integer > range = new TreeMap<>();
		range.put("1-9",0);
		range.put("10-15",0);
		range.put("16-20",0);
		range.put("16-20",0);
		range.put("21+",0);
		String rangeCustomer = "";
		for (Map.Entry<String,Integer> item: details.entrySet()) {
			if ((1<=item.getValue()) && (item.getValue()<=9)) {
				range.replace("1-9", (range.get("1-9"))+1);
			}
			if ((10<=item.getValue()) && (item.getValue()<=15)) {
				range.replace("10-15", (range.get("10-15"))+1);
			}
			if ((16<=item.getValue()) && (item.getValue()<=20)) {
				range.replace("16-20", (range.get("16-20"))+1);
			}
			if (21<=item.getValue()){
				range.replace("21+", (range.get("21+"))+1);
			}
		}
		for (Map.Entry<String,Integer> item: range.entrySet()) {
			rangeCustomer += item.getValue().toString() + "," + item.getKey() + ",";
		}
		return rangeCustomer;
	}

	/**
	* This method prepares an inventory row for each machine in the list by querying the items in machines table in the database.
	* The inventory row contains details such as items in report, items below threshold, and unavailable items.
	* It also saves the inventory report by calling the saveInventoryReport method.
	* @param currentYear the current year
	* @param currentMonth the current month
	*/
	private static void prepareInventoryRow(int currentYear, int currentMonth) {
		ArrayList<String> areaLocationsList = getMachineList();
		String items_in_report = "";
		String items_below_threshold = "";
		String unavailable_items = "";
		String month = String.valueOf(currentMonth);
		if (String.valueOf(currentMonth).length()==1){
			month = "0"+String.valueOf(currentMonth);
		}
		for (String machine:areaLocationsList) {
			ArrayList<String> details = new ArrayList<>();
			items_in_report = "";
			items_below_threshold = "";
			unavailable_items = "";
			String query = "SELECT * FROM ekrut.items_in_machines WHERE machine_id ='"+machine+"'";
			try (PreparedStatement stmt = con1.prepareStatement(query)) {
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						items_in_report += rs.getString("item_name") + "," + rs.getString("amount") + ",";
						items_below_threshold += rs.getString("item_name") + "," + rs.getString("count_under_threshold") + ",";
						unavailable_items += rs.getString("item_name") + "," + rs.getString("count_unavailable") + ",";
					}
				}
			details.add(machine);
			details.add(items_in_report);
			details.add(items_below_threshold);
			details.add(unavailable_items);
			details.add(month);
			details.add(String.valueOf(currentYear));
			details.add(unavailable_items);
			saveInventoryReport(details);
			}catch(SQLException e) {
				System.out.println("Generate inventory report failed!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieves a list of machine IDs from the 'local_machines' table in the 'ekrut' database.
	 *
	 * @return An ArrayList containing the machine IDs, in that order.
	 * @throws SQLException if a database error occurs while executing the query.
	 */
	public static ArrayList<String> getMachineList() {
		ArrayList<String> areaLocationsList = new ArrayList<>();

		String query = "SELECT machine_id FROM ekrut.local_machines";
		try {
			Statement stmt = con1.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			while (resultSet.next()) {
				String machine_id = resultSet.getString("machine_id");
				areaLocationsList.add(machine_id);
			}
		}catch(SQLException e) {e.printStackTrace();}

		return areaLocationsList;
	}
	
	/**
	* This method saves the inventory report to the monthly_inventory_reports table in the database.
	* The report_id is set as the number of entries in the table + 1.
	* @param details an ArrayList containing the details of the inventory report such as machine, items in report, items below threshold, unavailable items, month, year, and unavailable items.
	*/
	public static void saveInventoryReport(ArrayList<String> details) {
		PreparedStatement stmt;
		Statement stmt1;
		// Get the number of entries in the inventory_report
		int count=0;
		try {
			stmt1=con1.createStatement();
			ResultSet rs = stmt1.executeQuery("SELECT * FROM monthly_inventory_reports;");
			while(rs.next()) {
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Set the report_id as the number of entries + 1
		String report_id = String.valueOf(count + 1);
		details.add(0,report_id);

		try {
			stmt=con1.prepareStatement("INSERT INTO ekrut.monthly_inventory_reports VALUES (?,?,?,?,?,?,?)");
			for (int i=0;i<7;i++) {
				stmt.setString(i+1, details.get(i));
			}
			stmt.executeUpdate();
		}catch(SQLException e) {e.printStackTrace();}
	}
	public static void clearInventoryData() {

		PreparedStatement prstmt;
		try {
			prstmt = con1.prepareStatement("UPDATE ekrut.items_in_machines SET count_under_threshold=? ,count_unavailable=? ");
			prstmt.setString(1,"0");
			prstmt.setString(2, "0");
			prstmt.executeUpdate();
		}catch(SQLException e) {e.printStackTrace();}
	}
	
	/**
	* Saves a new customers activity report to the DB.
	* Creates a new report number and insert the new entry to the monthly_customeractivity_reports
	* @param reportRowDetails an ArrayList containing the details 
	* of the customers activity report.
	*/
	private static void saveCustomerActivityReport(ArrayList<String> reportRowDetails) {
		PreparedStatement stmt;
		Statement stmt1;
		// Get the number of entries in the inventory_report
		int count=0;
		try {
			stmt1=con1.createStatement();
			ResultSet rs = stmt1.executeQuery("SELECT * FROM ekrut.monthly_customeractivity_reports");
			while(rs.next()) {
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Set the report_id as the number of entries + 1
		String report_id = String.valueOf(count + 1);
		reportRowDetails.add(0,report_id);
		try {
			stmt=con1.prepareStatement("INSERT INTO ekrut.monthly_customeractivity_reports VALUES (?,?,?,?,?)");
			stmt.setString(1, reportRowDetails.get(0));
			stmt.setString(2, reportRowDetails.get(1));
			stmt.setString(3, reportRowDetails.get(2));
			stmt.setString(4, reportRowDetails.get(3));
			stmt.setString(5, reportRowDetails.get(4));
			stmt.executeUpdate();
		}catch(SQLException e) {e.printStackTrace();}
	}


	/**
     * Method to update the status and estimated delivery date of a list of RemoteOrder objects in the database.
     * 
     * The method takes an ArrayList of RemoteOrder objects as an input and uses a PreparedStatement to update the
     * status and estimated delivery date of each order in the database.
     * For each RemoteOrder object in the input ArrayList, the method sets the status and estimated delivery date of the
     * object to the corresponding values in the database.
     * 
     * This method throws SQLException if an exception occurs while executing the update statement.
     *
     * @param ro an ArrayList of RemoteOrder objects that need to be updated in the database.
     * @throws SQLException if an exception occurs while executing the update statement.
     */
	public static void updateOrdersRemote(ArrayList<RemoteOrder> ro) {
		PreparedStatement prstmt;
		try {
			prstmt = con1.prepareStatement("UPDATE ekrut.orders_remote SET status=? ,estimated_delivery_date=? WHERE order_code=?");
			for(RemoteOrder o : ro) {
				
				prstmt.setString(1,o.getStatus());
				prstmt.setString(2, o.getEstimated_delivery_date());
				prstmt.setString(3, o.getOrder_code());
				prstmt.executeUpdate();
			}
			
		}catch(SQLException e) {e.printStackTrace();}
	}

	/**
	 * Retrieves a list of orders that are remote orders that were made in pickup option 
	 * 
	 * @param idMSG customer id of which we want his remote pick up orders
	 * @return An ArrayList containing the pickup orders.
	 */
	public static ArrayList<PickUpOrder> getPickUps(ArrayList<Object>idMSG){
		PreparedStatement stmt;
		ArrayList<PickUpOrder>pickUps=new ArrayList<PickUpOrder>();
		String id=((String)idMSG.get(0));
		
		try {
			String query="SELECT * FROM orders_remote WHERE customer_id= ? AND order_type='REMOTE_PICKUP' AND status='ACTIVE';";
			stmt=con1.prepareStatement(query);
			stmt.setString(1, id);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ArrayList<String>insert=new ArrayList<String>();
				insert.add(rs.getString(1));
				insert.add(rs.getString(9));
				insert.add(rs.getString(4));
				
				pickUps.add(new PickUpOrder(insert.get(0),insert.get(1),insert.get(2)));
			}
			rs.close();
			
		}catch(SQLException e) {
			System.out.println("Importing pick ups from DB has failed!");
		}
		return pickUps;
	}
	
	/**
	 * Retrieves a list of orders that are remote orders that were made in delivery option 
	 * 
	 * @param idMSG customer id of which we want his remote delivery orders
	 * @return An ArrayList containing the delivery orders.
	 */
	public static ArrayList<PresentDeliveryOrder> getDelivery(ArrayList<Object>idMSG){
		PreparedStatement stmt;
		ArrayList<PresentDeliveryOrder>delivery=new ArrayList<PresentDeliveryOrder>();
		String id=((String)idMSG.get(0));
		
		try {
			String query="SELECT * FROM orders_remote WHERE customer_id= ? AND order_type='REMOTE_DELIVERY' AND status='CONFIRM';";
			stmt=con1.prepareStatement(query);
			stmt.setString(1, id);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ArrayList<String>insert=new ArrayList<String>();
				insert.add(rs.getString(1));
				insert.add(rs.getString(9));
				insert.add(rs.getString(10));
				insert.add(rs.getString(12));
				delivery.add(new PresentDeliveryOrder(insert.get(0),insert.get(1),insert.get(2),insert.get(3)));
			}
			rs.close();
			
		}catch(SQLException e) {
			System.out.println("Importing pick ups from DB has failed!");
		}
		return delivery;
	}
	
	/**
	 * Update the order_received_date of the delivery order
	 * 
	 * @param order_info ArrayList that contains the order code and date as strings
	 */
	public static void updateDeliveryOrderStatus(ArrayList<Object> order_info) {
		try {
			String status = "DELIVERED";
			String editOrder = (String)order_info.get(0);
			String time=(String)order_info.get(1);
			String query = "UPDATE orders_remote SET status=? ,order_received_date=? WHERE order_code=?";
			PreparedStatement ps = con1.prepareStatement(query);
			ps.setString(1, status);
			ps.setString(2, time);
			ps.setString(3, editOrder);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieves a list of format machine_id, location, threshold_level according
	 * to the machines in that specified area
	 * 
	 * @param area Area to import machines threshold levels
	 * @return ArrayList<String> strings of machine_id, location, threshold_level
	 */
	public static ArrayList<String> importMachinesThresholds(ArrayList<Object> area) {
		PreparedStatement stmt;
		ArrayList<String> machineList= new ArrayList<>();
		if (area.get(0) instanceof String) {
			String m_area = (String) area.get(0);
			try {
				String query = "SELECT * FROM local_machines WHERE area = ?;";
				stmt = con1.prepareStatement(query);
				stmt.setString(1, m_area);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					machineList.add(rs.getString("machine_id"));
					machineList.add(rs.getString("location"));
					int tresHold = rs.getInt("threshold_level");
					String thresh_hold = ""+tresHold;
					machineList.add(thresh_hold);
				}
				rs.close();
			} catch (SQLException e) {
				System.out.println("Importing Machine List has failed!");
			}
		}
		return machineList;
	}
	
	/**
	 * This method updates the threshold level for a specific machine and updates the status of items in that machine if the threshold level changes.
	 * If the new threshold level is higher than the old one, the method will update the "is_under_alert" column for items in the machine to 1 if their amount is less than the new threshold level.
	 * If the new threshold level is lower than the old one, the method will update the "is_under_alert" column for items in the machine to 0 if their amount is greater than or equal to the new threshold level.
	 * Additionally, the method will create an alert for items in the machine that have an amount less than the new threshold level, and add that alert to the database.
	 *
	 * @param machine_info - ArrayList containing the following information in this order: area, machine_id, new threshold level (as a string)
	 */
	public static void UpdateThresholdLevel (ArrayList<Object> machine_info) {
		String area = (String)machine_info.get(0);
		String machine_id = (String)machine_info.get(1);
		int new_tl= Integer.parseInt((String)machine_info.get(2));
		int old_threshold = getThreshold(machine_id); // save the old th_level
		String query2;
		String items_below_th="";
		
		try {
			String query = "UPDATE local_machines SET threshold_level=? WHERE area=? AND machine_id=?";
			PreparedStatement ps = con1.prepareStatement(query);
			ps.setInt(1, new_tl);
			ps.setString(2, area);
			ps.setString(3, machine_id);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (new_tl > old_threshold) {
			query2 = "UPDATE ekrut.items_in_machines SET is_under_alert = IF(amount<"+new_tl+",'1',is_under_alert), "
					+ "count_under_threshold = count_under_threshold + IF(amount >= "+old_threshold+" AND amount < "+new_tl+",1,0) "
							+ "WHERE machine_id='"+machine_id+"'";
			try {
				PreparedStatement ps = con1.prepareStatement(query2);
				ps.executeUpdate();
				
				String select_query = "SELECT item_name, amount FROM items_in_machines "
						+ "WHERE machine_id = '"+machine_id+"' AND amount < "+new_tl+"";
				ps = con1.prepareStatement(select_query);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					items_below_th += rs.getString("item_name") + "," + rs.getString("amount") + ",";
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else { // new_tl < old_threshold
			query2 = "UPDATE ekrut.items_in_machines SET is_under_alert = IF(amount>="+new_tl+",'0',is_under_alert) "
					+ "WHERE machine_id='"+machine_id+"'";
			try {
				PreparedStatement ps = con1.prepareStatement(query2);
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		int length = items_below_th.length();
		String items_below_threshold = items_below_th.substring(0, length - 1);
		
		// set up the date to be send in the alert
		ZoneId zone = ZoneId.of("Asia/Jerusalem");
	    ZonedDateTime now = ZonedDateTime.now(zone);
	    String fullTime = now.toString();
	    // Split the string by the 'T' character
	    String[] parts = fullTime.split("T");
	    // Get the first part (the date)
	    String date = parts[0];
	    
		ItemsAlert alert = new ItemsAlert(null,machine_id, date, AlertStatus.ACTIVE, items_below_threshold, null, null);
		addAlertToDB(alert);
	}

	//ManagePremision By Area Manager
	/**
	 * This Method upgrade user to customer after approve of Area Manager
	 * @author G-10
	 * @param msg  -msg from client with customer detail*/
	public static void UpdateUserToCustomer(ArrayList<Object> msg) {
			ArrayList<Object> arrtemp=new ArrayList<>();
			String id = (String)msg.get(0);
			Customers customer=BuildCustomer(id);
			

			String query = "DELETE FROM customers_request  WHERE user_id='" + id + "';";
			String query2 = "UPDATE users SET role=?  WHERE user_id='" + id + "';";
	
			try {//
				PreparedStatement ps = con1.prepareStatement(query);
				PreparedStatement pss=con1.prepareStatement(query2);
				PreparedStatement ps2=con1.prepareStatement("INSERT INTO customers VALUES (?,?,?,?,?)");
				ps2.setString(1, customer.getCustomer_id()); 
				ps2.setString(2,customer.getCredit_card_num()); 
				ps2.setString(3,customer.getCredit_card_exp());
				ps2.setString(4,customer.getCvv());
				ps2.setString(5,"0");
			

				ps.executeUpdate();
				ps.close();
				pss.setString(1, "Customer");
				pss.executeUpdate();
				pss.close();
				ps2.executeUpdate();
				ps2.close();

			} catch (SQLException e) {
				System.out.println("exception can not update to customer");
				e.printStackTrace();
				
			}

		}
	/**
	 * This Method find the customer by the id
	 * Method for UpdateUserToCustomer
	 * @author G-10
	 * @param msg  -id of customer */


	private static Customers BuildCustomer(String id) {
		Statement stmt;
		Customers c=null;
		try {
			stmt=con1.createStatement();
			String queryCheck = "SELECT * FROM customers_request WHERE user_id='" +id + "';";
			ArrayList<Object> arrtemp=new ArrayList<>();
			arrtemp.add((String)id);
			User user =getUserDetail(arrtemp);
			ResultSet rs = stmt.executeQuery(queryCheck); 
			if(rs.next()) {
				ArrayList<String> sub=new ArrayList<>();
				for(int i=0;i<6;i++) {
					sub.add(rs.getString(i+1));
				}			
			c=new Customers(user,sub.get(3),sub.get(4),sub.get(5),false);
		}
			
		}catch (SQLException e) {System.out.println("exception not exist customer");};
	
		return c;
	}
	/**
	 * This Method returned all the users request in specific area to the manager
	 * @author G-10
	 * @param msg  from client with the area of the Area Manager */
	public static ArrayList<String> UserRequestInSpecificArea(ArrayList<Object> msg) {
		ArrayList<String> user = new ArrayList<>();
		try {	
			String area=((String)msg.get(0));
			Statement stmt;
			String query = "SELECT * FROM customers_request WHERE area_treatment= '" + area + "';";
	        stmt = con1.createStatement();
	   
	        ResultSet rs = stmt.executeQuery(query);
	        
			while(rs.next()) {
				ArrayList<String> sub = new ArrayList<String>();
				for(int i=0;i<6;i++) {
					sub.add(rs.getString(i+1));
				}
				user.add(sub.get(0));
				user.add(sub.get(1));	
			
			}
			rs.close();
		}catch(SQLException e) {
			System.out.println("Importing sales from has failed!");
			e.printStackTrace();
		}
		return user;
	}

	/**This Java method generateCustomerAndOrdersReports(int month, int year) is used to
	 *  generate customer and order reports for a specific month and year.
	 *  The method queries two tables in a database, "orders_remote" and "orders_local", and filters the results by
	 *  the specified month and year. The location of the machines associated with the orders is determined using a HashMap,
	 *  and the filtered data is added to an ArrayList. The method then calls another method,
	 *  generateOrderReport(data_remote_orders,String.valueOf(year), String.valueOf(month)),
	 *  passing in the ArrayList of filtered data as well as the year and month used for filtering.
	 *  The data passed in the ArrayList is in the format of "location,machine_id,customer_id,items" .
	 * /
	 * @param month
	 * @param year
	 */
	public static void generateOrdersReports(int year, int month) {
		PreparedStatement stmt,stmt1;
		int indexMachine=0;
		ArrayList<String> data_remote_orders=new ArrayList<>();
		HashMap<String,String> hashMapareaLocation= getAreaLocationsHashMap();
		ArrayList<String> machines = new ArrayList<>();
		ArrayList<String> machineList=new ArrayList<>();
		try {
			stmt = con1.prepareStatement("SELECT machine_area,machine_id,customer_id,items,order_confirmed_date FROM ekrut.orders_remote WHERE order_type='REMOTE_PICKUP'");
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {

				if (((Integer.parseInt(rs.getString(5).split(" ")[0].split("-")[1]) == month)) && (Integer.parseInt(rs.getString(5).split(" ")[0].split("-")[0]) == year)) {
					String place1 = "";

					for (String location1 : hashMapareaLocation.keySet()) {
						String[] listOf_machine1 = hashMapareaLocation.get(location1).split(",");
						for (int i = 0; i < listOf_machine1.length; i++) {
							if (listOf_machine1[i].equals(rs.getString(2))) {
								place1 += location1 + " " + listOf_machine1[i];

							}
						}
					}

					data_remote_orders.add(place1 + "," + rs.getString(2) + "," + rs.getString(3) + "," + rs.getString(4));
				}
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try {

			stmt1 = con1.prepareStatement("SELECT machine_id,customer_id,items,order_confirmed_date FROM ekrut.orders_local");
			ResultSet rs = stmt1.executeQuery();

			while(rs.next()){
				if(((Integer.parseInt(rs.getString(4).split(" ")[0].split("-")[1]) == month)) && (Integer.parseInt(rs.getString(4).split(" ")[0].split("-")[0]) == year)){
					String place="";
					for(String location : hashMapareaLocation.keySet()){
						String[] listOf_machine = hashMapareaLocation.get(location).split(",");
						for(int i=0;i<listOf_machine.length;i++){
							if(listOf_machine[i].equals(rs.getString(1))){
								place+=location+" "+listOf_machine[i];
							}

						}
					}
					data_remote_orders.add(place +","+rs.getString(1)+ ","+rs.getString(2) + "," + rs.getString(3));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		generateOrderReport(data_remote_orders,String.valueOf(year), String.valueOf(month));
	}

	/**
	 * Retrieves a list of areas, locations, and machine IDs from the 'local_machines' table in the 'ekrut' database.
	 *
	 * @return An ArrayList containing the areas, locations, and machine IDs, in that order.
	 * @throws SQLException if a database error occurs while executing the query.
	 */
	public static HashMap<String,String> getAreaLocationsHashMap () {
		HashMap<String,String> machineIn_areaLocationsList = new HashMap<>();
		String query = "SELECT area, location, machine_id FROM ekrut.local_machines";
		try {
			Statement stmt = con1.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			while (resultSet.next()) {
				String area = resultSet.getString("area");
				String location = resultSet.getString("location");
				String machine_id = resultSet.getString("machine_id");
				if(machineIn_areaLocationsList.containsKey(area + " " + location)){
					String places = machineIn_areaLocationsList.get(area + " " + location); //the priviues value
					places+=","+machine_id; //north1,north2,north3
					machineIn_areaLocationsList.put(area + " " + location,places);
				}
				else{
					machineIn_areaLocationsList.put(area + " " + location,machine_id);
				}


			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return machineIn_areaLocationsList;
	}
	/**

	 This method is used to generate a report of machine orders.
	 It takes in two parameters, an ArrayList of data containing information about the orders and a year and month to specify the time frame of the report.
	 The method creates three HashMaps: one for the number of orders for each machine, one for the list of orders for each machine in the given month, and one for the total amount of each item ordered.
	 It then iterates through the data ArrayList, parsing the information and adding it to the appropriate HashMap.
	 Once all the data has been processed, the method uses the information in the HashMaps to generate the report.
	 @param data - an ArrayList containing information about the orders.
	 @param year - the year for which the report is being generated.
	 @param month - the month for which the report is being generated.
	 */

	public static void generateOrderReport(ArrayList<String> data, String year, String month)
	{
		HashMap<String, Integer> mapForAmountOfOrders = new HashMap<>(); //map for numOfTotalOrders
		HashMap<String, String> mapForListOfOrders = new HashMap<>(); //map for list of orders for each machine in the month
		HashMap<String, Integer> mapForOrders = new HashMap<>(); //calculate every item amount
		String place="";
		ArrayList<String> justItems=new ArrayList<>();
		HashMap<String,Integer> productAndAmount_ofMachine= new HashMap<>();
		String item;
		String item1="";
		String[] arrdata;
		String listOfItem[];
		int value;
		int k=0;
		String items="";
		String place3 = "";


		for(int i=0;i<data.size();i++) {

			place = data.get(i).split(",")[0];


			if (!mapForAmountOfOrders.containsKey(place)) {
				mapForAmountOfOrders.put(place, 1);
				arrdata=data.get(i).split(",");
				k=3;
				for (int j=3 ;j< arrdata.length;j+=2){

					item1+=arrdata[j]+","+arrdata[j+1];
					if((k+1) < arrdata.length){

						item1+=",";
					}
					k+=2;
				}
				mapForListOfOrders.put(place,item1); //key:place-area,location && value: list of items

			}

			else
			{
				//if there is more orders to this machine
				k=3;
				arrdata=data.get(i).split(",");
				item1=mapForListOfOrders.get(place); //returned the values until now
				for (int j=3 ;j< arrdata.length;j+=2) {
					item1+=arrdata[j]+","+arrdata[j+1];

					if((k+1) <arrdata.length) {
						item1 += ",";
					}
					k+=2;
				}
				mapForListOfOrders.put(place,item1);

				//increase the amount of orders +1
				value = mapForAmountOfOrders.get(place);
				value++;
				mapForAmountOfOrders.put(place, value);
			}
		}
		for(String str : mapForListOfOrders.keySet()){
			String str1 = mapForListOfOrders.get(str);
			mapForListOfOrders.put(str,str1.substring(0,str1.length()-1));
		}
		for(String location : mapForAmountOfOrders.keySet()) {
			item = mapForListOfOrders.get(location);
			listOfItem = item.split(","); //arr with item&amount
			for(int i=0;i<listOfItem.length-1;i+=2)
			{
				if(!mapForOrders.containsKey(listOfItem[i]))
				{
					mapForOrders.put(listOfItem[i], Integer.parseInt(listOfItem[i+1]));
				}

				else
				{
					int amount = mapForOrders.get(listOfItem[i]);
					amount = amount + Integer.parseInt(listOfItem[i+1]);

					mapForOrders.put(listOfItem[i], amount);
				}
			}
			String machineNameAndArea[] = location.split(" ");
			place3 = location.split(" ")[2];
			String listOfItems = "";

			int size=0;
			int maxValue = 0;
			String maxKey = null;
			for(String itemInMap : mapForOrders.keySet())
			{
				listOfItems +=  itemInMap + "," + Integer.toString(mapForOrders.get(itemInMap));
				if(size+1 != mapForOrders.size())
				{
					listOfItems += ",";
				}

				if(mapForOrders.get(itemInMap) >= maxValue){
					maxValue=mapForOrders.get(itemInMap); //the value
					maxKey=itemInMap; //the Key
				}

				size++;
			}
			Statement stmt;
			int reportId = 0;
			try
			{
				stmt = con1.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM monthly_orders_reports");
				while (rs.next()) {
					//reportId = Integer.parseInt(rs.getString(1));
					reportId++;
				}
				PreparedStatement ps = con1.prepareStatement("insert into ekrut.monthly_orders_reports values(?,?,?,?,?,?,?,?,?)");
				ps.setString(1, Integer.toString(reportId+1));// number of report
				ps.setString(2, machineNameAndArea[1]); // location
				ps.setString(3, listOfItems);// list Of Items
				ps.setString(4, String.valueOf(mapForAmountOfOrders.get(location)));// numOfTotalOrders
				ps.setString(5,place3); // machine_id
				ps.setString(6, maxKey); // most_sold
				ps.setString(7, month); // month
				ps.setString(8, year); // year
				ps.setString(9, machineNameAndArea[0]); // machine area
				ps.executeUpdate();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			mapForOrders.clear();
		}

	}

















}

