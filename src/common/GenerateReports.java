package common;

import server.MySQLConnection;

import java.time.Year;
import java.util.Calendar;



public class GenerateReports implements Runnable {
    public static final String REPORTS_ALREADY_EXISTS = "Reports are already exists";
    public static final String NEW_REPORTS_CREATED = "Generating All Reports has done successfully";
    private int lastCollectedMonth = -1;
    private final int HOUR_IN_MILLIS = 60 * 60 * 1000;

    /**
     * Creating all reports on the end of each month
     * checks if last month is current month, if true, thread sleeps for 1 hour.
     * else trying to create monthly reports
     * This task is executed every 1 hour until the thread is interrupted.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH) + 1;
                if (lastCollectedMonth != currentMonth) {
                    System.out.println("Month is over, start creating reports");
                    boolean created_successfully = createAllReports();
                    if (created_successfully)
                        lastCollectedMonth = currentMonth;
                    // else => will retry next hour
                } else {
                    System.out.println("Generate Reports-Didn't pass a month, doesn't creating reports");
                }
                Thread.sleep(HOUR_IN_MILLIS);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean createAllReports() {
        MsgHandler<Object> response = MySQLConnection.generateReports(getYear(),getMonth());
        switch (response.getType()) {
            case AlreadyExsit:
                System.out.println(response.getType().toString());
                return true;
            case GenerateSuccessfully:
                System.out.println(response.getType().toString());
                return true;
            case GenerateFailed:
                System.out.println(response.getType().toString());
                return false;
        }
        return true;
    }
    private int getYear(){
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) +1;
        int currentYear = Year.now().getValue();
        if(currentMonth==1){
            return currentYear -1;
        }
        return currentYear;
    }
    private int getMonth(){
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) +1;
        if(currentMonth==1){
            return 12;
        }
        return currentMonth;
    }

}
