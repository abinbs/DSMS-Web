package org.example;


import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class User {
    public static final int USER_ADMIN = 1;
    public static final int USER_BUYER = 2;
    public static final int BUYER_PURCHASE_SHARE = 1;
    public static final int BUYER_GET_SHARE = 2;
    public static final int BUYER_SELL_SHARE = 3;
    public static final int BUYER_SWAP_SHARE = 4;
    public static final int BUYER_LOGOUT = 5;
    public static final int ADMIN_ADD_SHARE = 1;
    public static final int ADMIN_REMOVE_SHARE = 2;
    public static final int ADMIN_LIST_SHARE_AVAILABILITY = 3;
    public static final int ADMIN_PURCHASE_SHARE = 4;
    public static final int ADMIN_GET_SHARE = 5;
    public static final int ADMIN_SELL_SHARE = 6;
    public static final int ADMIN_SWAP_SHARE = 7;
    public static final int ADMIN_LOGOUT = 8;


    static Scanner sc;

    public static void main(String[] args) throws Exception{
        Initialize(args);
    }

    public static void Initialize(String[] args) throws IOException{
        sc = new Scanner(System.in);
        String userId;
        System.out.println("Welcome to DSMS!");
        System.out.println("Enter your user Id:");
        userId = sc.next().trim().toUpperCase();
        Logging.clientLog(userId, " login attempt");
        switch(checkUserType(userId)) {
            case USER_ADMIN:
                try {
                    System.out.println("Admin logged in succesfully!");
                    Logging.clientLog(userId, " Admin Login successful");
                    adminAccount(userId,args);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                break;
            case USER_BUYER:
                try {
                    System.out.println("Buyer logged in succesfully!");
                    Logging.clientLog(userId, " User Login successful");
                    buyerAccount(userId, args);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            default:
                System.out.println("UserId is not in correct format");
                Logging.clientLog(userId, " UserId is not in correct format");
                Initialize(args);
        }
    }


    private static int checkUserType(String userID) {
        if (userID.length() == 8) {
            if (userID.substring(0, 3).equalsIgnoreCase("NYK") ||
                    userID.substring(0, 3).equalsIgnoreCase("LON") ||
                    userID.substring(0, 3).equalsIgnoreCase("TOK")) {
                if (userID.substring(3, 4).equalsIgnoreCase("A")) {
                    return USER_ADMIN;
                } else if (userID.substring(3, 4).equalsIgnoreCase("B")) {
                    return USER_BUYER;
                }
            }
        }
        return 0;
    }


    private static void adminAccount(String adminId, String[] args) throws Exception {

        String serverId = adminId.substring(0, 3).toUpperCase();
        // URL of the WSDL document for the web service
        URL url = new URL("http://localhost:8080/"+serverId+"?wsdl"); // Change Server1 to desired server

        // QName representing the service name and namespace
        QName qname = new QName("http://example.org/", "ServerManagementService");

        // Create a Service object for the specified WSDL document and QName
        Service service = Service.create(url, qname);

        DSMSService SerObj = service.getPort(DSMSService.class);

        boolean repeat = true;
        printMenu(USER_ADMIN);
        String userId;
        String shareType;
        String shareId;
        String serverResponse;
        String newShareId;
        String newShareType;
        int capacity;
        int menuSelection = sc.nextInt();
        switch(menuSelection) {
            case ADMIN_ADD_SHARE:
                Logging.clientLog(adminId, " attempting to addShare");
                shareType = identifyShareType();
                shareId = identifyShareId();
                capacity = identifyCapacity();
                serverResponse = SerObj.addShare(shareId, shareType, capacity);
                System.out.println(serverResponse);
                break;

            case ADMIN_REMOVE_SHARE:
                Logging.clientLog(adminId, " attempting to removeShare");
                shareType = identifyShareType();
                shareId = identifyShareId();
                serverResponse = SerObj.removeShare(shareId, shareType);
                System.out.println(serverResponse);
                break;

            case ADMIN_LIST_SHARE_AVAILABILITY:
                Logging.clientLog(adminId, " attempting to shareListAvailability");
                shareType = identifyShareType();
                serverResponse = SerObj.listShareAvailability(shareType);
                System.out.println(serverResponse);
                break;

            case ADMIN_PURCHASE_SHARE:
                userId = askForUserIdFromAdmin(adminId.substring(0, 3));
                shareType = identifyShareType();
                shareId = identifyShareId();
                capacity = identifyCapacity();
                Logging.clientLog(adminId, " attempting to purchaseShare");
                serverResponse = SerObj.purchaseShare(userId, shareId, shareType, capacity);
                Logging.clientLog(adminId, "purchaseShare", " userId : " + userId +"shareId: " + shareId + " shareType: " + shareType + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case ADMIN_GET_SHARE:
                Logging.clientLog(adminId, " attempting to getShare");
                userId = askForUserIdFromAdmin(adminId.substring(0, 3));
                serverResponse = SerObj.getShares(userId);
                Logging.clientLog(adminId, "getShare", " userId: "+userId , serverResponse);
                System.out.println(serverResponse);
                break;

            case ADMIN_SELL_SHARE:
                Logging.clientLog(adminId, " attempting to sellShare");
                userId = askForUserIdFromAdmin(adminId.substring(0, 3));
                shareType = identifyShareType();
                capacity = identifyCapacity();
                shareId = identifyShareId();
                serverResponse = SerObj.sellShare(userId, shareId, capacity);
                Logging.clientLog(adminId, " sellShare", " userId : " + userId+ "shareId: " + shareId + " capacity: " + capacity + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case ADMIN_SWAP_SHARE:
                Logging.clientLog(adminId, " attempting to swapShare");
                userId = askForUserIdFromAdmin(adminId.substring(0, 3));
                System.out.println("Enter details of share you have to sell");
                shareId = identifyShareId();
                shareType = identifyShareType();
                System.out.println("Enter details of share you have to purchase");
                newShareId = identifyShareId();
                newShareType = identifyShareType();
                serverResponse = SerObj.swapShares(userId,shareId, shareType, newShareId, newShareType);
                Logging.clientLog(adminId, "swapShare", " userId : " + userId+ "oldShareId: " + shareId + "newShareId: "+ newShareId + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case ADMIN_LOGOUT:
                repeat = false;
                Logging.clientLog(adminId, " attempting to logout");
                Initialize(args);
                break;
        }
        if (repeat) {
            adminAccount(adminId, args);
        }
    }






    private static void printMenu(int userType) {
        System.out.println("*************************************");
        System.out.println("Please choose an option below:");
        if (userType == USER_BUYER) {
            System.out.println("1.Purchase Share");
            System.out.println("2.Get Shares");
            System.out.println("3.Sell Shares");
            System.out.println("4.Swap Share");
            System.out.println("5.Logout");
        } else if (userType == USER_ADMIN) {
            System.out.println("1.Add Shares");
            System.out.println("2.Remove Shares");
            System.out.println("3.List Share Availability");
            System.out.println("4.Purchase Share");
            System.out.println("5.Get Shares");
            System.out.println("6.Sell Shares");
            System.out.println("7.Swap Share");
            System.out.println("8.Logout");
        }
    }

    private static String identifyShareType() {
        System.out.println("*************************************");
        System.out.println("Please choose an shareType below:");
        System.out.println("1.Equity");
        System.out.println("2.Bonus");
        System.out.println("3.Dividend");
        switch (sc.nextInt()) {
            case 1:
                return ShareModel.EQUITY;
            case 2:
                return ShareModel.BONUS;
            case 3:
                return ShareModel.DIVIDEND;
        }
        return identifyShareType();
    }

    private static String identifyShareId() {
        System.out.println("*************************************");
        System.out.println("Please enter the ShareId");
        String shareId = sc.next().trim().toUpperCase();
        if (shareId.length() == 10) {
            if (shareId.substring(0, 3).equalsIgnoreCase("NYK") ||
                    shareId.substring(0, 3).equalsIgnoreCase("LON") ||
                    shareId.substring(0, 3).equalsIgnoreCase("TOK")) {
                if (shareId.substring(3, 4).equalsIgnoreCase("M") ||
                        shareId.substring(3, 4).equalsIgnoreCase("A") ||
                        shareId.substring(3, 4).equalsIgnoreCase("E")) {
                    return shareId;
                }
            }
        }
        return identifyShareId();
    }

    private static int identifyCapacity() {
        System.out.println("*************************************");
        System.out.println("Please enter the no of shares.");
        return sc.nextInt();
    }

    private static String askForUserIdFromAdmin(String City) {
        System.out.println("Please enter a userId(Within " + City + " Server):");
        String userID = sc.next().trim().toUpperCase();
        if (checkUserType(userID) != USER_BUYER || !userID.substring(0, 3).equals(City)) {
            return askForUserIdFromAdmin(City);
        } else {
            return userID;
        }
    }

    private static void buyerAccount(String userId, String[] args) throws Exception {

        String serverId = userId.substring(0, 3).toUpperCase();
        // URL of the WSDL document for the web service
        URL url = new URL("http://localhost:8080/"+serverId+"?wsdl"); // Change Server1 to desired server

        // QName representing the service name and namespace
        QName qname = new QName("http://example.org/", "ServerManagementService");

        // Create a Service object for the specified WSDL document and QName
        Service service = Service.create(url, qname);

        DSMSService SerObj = service.getPort(DSMSService.class);

        boolean repeat = true;
        printMenu(USER_BUYER);
        int menuSelection = sc.nextInt();
        String shareType;
        String shareId;
        int capacity;
        String newShareId;
        String newShareType;
        String serverResponse;
        switch (menuSelection) {
            case BUYER_PURCHASE_SHARE:
                shareType = identifyShareType();
                shareId = identifyShareId();
                capacity = identifyCapacity();
                Logging.clientLog(userId, " attempting to purchaseShare");
                serverResponse = SerObj.purchaseShare(userId,shareId, shareType, capacity);
                Logging.clientLog(userId, "purchaseShare", " shareId: " + shareId + " shareType: " + shareType + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case BUYER_GET_SHARE:
                Logging.clientLog(userId, " attempting to getShare");
                serverResponse = SerObj.getShares(userId);
                Logging.clientLog(userId, "getShare", " null ", serverResponse);
                System.out.println(serverResponse);
                break;

            case BUYER_SELL_SHARE:
                shareId = identifyShareId();
                capacity = identifyCapacity();
                Logging.clientLog("User area"+userId, " attempting to sellShare");
                System.out.println(userId + " "+ shareId + " "+ capacity);
                serverResponse = SerObj.sellShare(userId, shareId, capacity);
                Logging.clientLog(userId, " sellShare", " shareId: " + shareId + " capacity: " + capacity + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case BUYER_SWAP_SHARE:
                System.out.println("Enter the details of share to sell");
                shareId = identifyShareId();
                shareType = identifyShareType();
                System.out.println("Enter the details of share to purchase");
                newShareId = identifyShareId();
                newShareType = identifyShareType();
                serverResponse = SerObj.swapShares(userId,shareId, shareType, newShareId, newShareType);
                Logging.clientLog(userId, " swapShare", " oldShareId: " + shareId + "newShareId: "+ newShareId + " ", serverResponse);
                System.out.println(serverResponse);
                break;

            case BUYER_LOGOUT:
                repeat = false;
                Logging.clientLog(userId, " attempting to logout");
                Initialize(args);
                break;
        }
        if (repeat) {
            buyerAccount(userId, args);
        }
    }

}

