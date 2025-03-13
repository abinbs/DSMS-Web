package org.example;


import javax.jws.WebService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebService(endpointInterface = "org.example.DSMSService")
public class ServerManagement implements DSMSService {
    public static final String SHARE_SERVER_NEWYORK = "NEWYORK";
    public static final String SHARE_SERVER_LONDON = "LONDON";
    public static final String SHARE_SERVER_TOKYO = "TOKYO";
    public static final int SERVER_PORT_NEWYORK = 8523;
    public static final int SERVER_PORT_LONDON= 8524;
    public static final int SERVER_PORT_TOKYO = 8525;

    private String ServerId;
    private String ServerName;
    // HashMap<ShareType, HashMap <ShareID, Share>>
    private Map<String, Map<String, ShareModel>> allShares;
    // HashMap<UserID, HashMap <ShareType, List<ShareID>>>
    private Map<String, Map<String, List<String>>> userShares;
    // HashMap<UserID, User>
    private Map<String, UserModel> serverUsers;


    public ServerManagement(String serverID, String serverName){
        super();
        this.ServerId = serverID;
        this.ServerName = serverName;
        allShares = new ConcurrentHashMap<>();
        allShares.put(ShareModel.EQUITY, new ConcurrentHashMap<>());
        allShares.put(ShareModel.BONUS, new ConcurrentHashMap<>());
        allShares.put(ShareModel.DIVIDEND, new ConcurrentHashMap<>());
        userShares = new ConcurrentHashMap<>();
        serverUsers = new ConcurrentHashMap<>();
    }


    @Override
    public String addShare(String shareId, String shareType, int capacity) {
        String response;

        // Ensure the shareType exists in allShares
        allShares.computeIfAbsent(shareType, k -> new ConcurrentHashMap<>());

        // Use thread-safe operations for concurrency
        synchronized (allShares.get(shareType)) {
            if (allShares.get(shareType).containsKey(shareId)) {
                ShareModel existingShare = allShares.get(shareType).get(shareId);
                if (existingShare.getShareCapacity() <= capacity) {
                    // Update the event capacity
                    ShareModel updatedShare = new ShareModel(existingShare.getShareType(), existingShare.getShareId(), capacity);
                    allShares.get(shareType).put(shareId, updatedShare);
                    response = "Success: Share " + shareId + " Capacity increased to " + capacity;
                    try {
                        Logging.serverLog(ServerId, "null", " RMI addShare ", " shareId: " + shareId + " shareType: " + shareType + " added " + capacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response = "Failed: Share Already Exists, Cannot Decrease share number";
                    try {
                        Logging.serverLog(ServerId, "null", " RMI addShare ", " shareId: " + shareId + " shareType: " + shareType + " added " + capacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Check if the share belongs to the current server
                if (ShareModel.identifyServer(shareId).equals(ServerName)) {
                    // Create a new share and add it to the map
                    ShareModel share = new ShareModel(shareType, shareId, capacity);
                    allShares.get(shareType).put(shareId, share);
                    response = "Success: Share " + shareId + " added successfully";
                    try {
                        Logging.serverLog(ServerId, "null", " RMI addShare ", " shareId: " + shareId + " shareType: " + shareType + " added " + capacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    response = "Failed: Cannot Add shares to servers other than " + ServerName;
                    try {
                        Logging.serverLog(ServerId, "null", " RMI addShare ", " shareId: " + shareId + " shareType: " + shareType + " added " + capacity + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return response;
    }

    @Override
    public String removeShare(String shareId, String shareType) {
        String response;

        // Ensure the shareType exists in allEvents
        if (!allShares.containsKey(shareType)) {
            response = "Failed: Share Type " + shareType + " Does Not Exist";
            try {
                Logging.serverLog(ServerId, "null", " RMI removeShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Check if the share belongs to the current server
        if (!ShareModel.identifyServer(shareId).equals(ServerName)) {
            response = "Failed: Cannot Remove share from servers other than " + ServerName;
            try {
                Logging.serverLog(ServerId, "null", " RMI removeShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Use thread-safe operations for concurrency
        synchronized (allShares.get(shareType)) {
            if (allShares.get(shareType).remove(shareId) != null) {
                response = "Success: Share Removed Successfully";
                try {
                    Logging.serverLog(ServerId, "null", " RMI removeShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                response = "Failed: Share " + shareId + " Does Not Exist";
                try {
                    Logging.serverLog(ServerId, "null", " RMI removeShare ", " shareId: " + shareId + " shareType: " + shareType +  " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return response;
    }

    @Override
    public String listShareAvailability(String shareType){
        StringBuilder builder = new StringBuilder();
        String response;

        // Retrieve shares from the current server
        Map<String, ShareModel> shares;
        synchronized (allShares) {
            shares = allShares.get(shareType);
        }
        builder.append(ServerName + " Server " + shareType + ":\n");
        if (shares == null || shares.isEmpty()) {
            builder.append("No shares of Type " + shareType + " on " + ServerName + " Server\n");
        } else {
            for (ShareModel share : shares.values()) {
                builder.append(share.toString()).append(" || ");
            }
            builder.append("\n=====================================\n");
        }

        // Query other servers for share availability
        String otherServer1, otherServer2;
        try {
            if (ServerId.equals("NYK")) {
                otherServer1 = sendUDPMessage(SERVER_PORT_LONDON, "listShareAvailability", "null", shareType, "null");
                otherServer2 = sendUDPMessage(SERVER_PORT_TOKYO, "listShareAvailability", "null", shareType, "null");
            } else if (ServerId.equals("LON")) {
                otherServer1 = sendUDPMessage(SERVER_PORT_NEWYORK, "listShareAvailability", "null", shareType, "null");
                otherServer2 = sendUDPMessage(SERVER_PORT_TOKYO, "listShareAvailability", "null", shareType, "null");
            } else {
                otherServer1 = sendUDPMessage(SERVER_PORT_NEWYORK, "listShareAvailability", "null", shareType, "null");
                otherServer2 = sendUDPMessage(SERVER_PORT_LONDON, "listShareAvailability", "null", shareType, "null");
            }
            builder.append(otherServer1).append(otherServer2);
        } catch (Exception e) {
            builder.append("Failed to retrieve events from other servers: ").append(e.getMessage());
        }

        response = builder.toString();
        try {
            Logging.serverLog(ServerId, "null", " RMI listShareAvailability ", " shareType: " + shareType + " ", response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public String purchaseShare(String buyerId, String shareId, String shareType, int shareCount){
        String response;

        // Ensure the user exists
        if (!serverUsers.containsKey(buyerId)) {
            addNewPersonToUsers(buyerId);
        }

        // Check if the share belongs to the current server
        if (ShareModel.identifyServer(shareId).equals(ServerName)) {
            // Ensure the event exists
            if (allShares.get(shareType) == null || !allShares.get(shareType).containsKey(shareId)) {
                response = "Failed: Share " + shareId + " Does Not Exist";
                try {
                    Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }

            // Check if the share is full
            ShareModel purchasedShare = allShares.get(shareType).get(shareId);
            if (purchasedShare.isFull()) {
                response = "Failed: Share " + shareId + " is Full";
                try {
                    Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }

            // Ensure the user has not already purchased the share
            synchronized (userShares.get(buyerId)) {
                if (userShares.get(buyerId).containsKey(shareType)) {
                    if (userShares.get(buyerId).get(shareType).contains(shareId)) {
                        response = "Failed: Share " + shareId + " Already Bought";
                        try {
                            Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return response;
                    }
                }
            }

            // Calculate the number of shares available for purchase
            int availableShares = purchasedShare.getShareRemainCapacity();
            int sharesToPurchase = Math.min(shareCount, availableShares);

            if (sharesToPurchase <= 0) {
                response = "Failed: No Shares Available for " + shareId;
                try {
                    Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }

            // Add the shares to the share's purchased count
            int addResult = purchasedShare.addShares(sharesToPurchase);
            if (addResult == ShareModel.ADD_SUCCESS) {
                // Update the user's share list
                synchronized (userShares.get(buyerId)) {
                    userShares.putIfAbsent(buyerId, new ConcurrentHashMap<>()); // Ensure buyer exists
                    userShares.get(buyerId).putIfAbsent(shareType, new ArrayList<>()); // Ensure shareType exists

                    List<String> shareList = userShares.get(buyerId).get(shareType);
                    for (int i = 0; i < sharesToPurchase; i++) {
                        shareList.add(shareId);  // Add shareId multiple times based on quantity
                    }
                    System.out.println(userShares);
                }

                // Prepare response message
                if (sharesToPurchase < shareCount) {
                    response = "Partial Success: Only " + sharesToPurchase + " Shares of " + shareId + " Were Available and Purchased";
                } else {
                    response = "Success: Purchased " + sharesToPurchase + " Shares of " + shareId + " Successfully";
                }
            } else if (addResult == ShareModel.SHARE_FULL) {
                response = "Failed: Share " + shareId + " is Full";
            } else {
                response = "Failed: Cannot Purchase Share " + shareId;
            }

        } else {
            // Check if the user has exceeded the weekly limit for other servers
            if (exceedWeeklyLimit(buyerId, shareId.substring(4))) {
                response = "Failed: You Cannot Buy Shares in Other Servers For This Week (Max Weekly Limit = 3)";
                try {
                    Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }

            // Send a UDP message to the appropriate server
            String serverResponse = sendUDPMessage(getServerPort(shareId.substring(0, 3)), "PurchaseShare", buyerId, shareType, shareId, shareCount);
            response = serverResponse;
        }
        try {
            Logging.serverLog(ServerId, buyerId, " RMI purchaseShare ", " shareId: " + shareId + " shareType: " + shareType + " ", response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response+userShares;
    }

    public void addNewPersonToUsers(String buyerId) {
        UserModel newUser = new UserModel(buyerId);
        serverUsers.put(newUser.getUserId(), newUser);
        userShares.put(newUser.getUserId(), new ConcurrentHashMap<>());
    }

    private boolean exceedWeeklyLimit(String buyerId, String shareDate) {
        int limit = 0;

        // Check if the user has any purchased shared
        if (!userShares.containsKey(buyerId)) {
            return false;
        }

        // Parse the shareDate
        int shareDay = Integer.parseInt(shareDate.substring(0, 2));
        int shareMonth = Integer.parseInt(shareDate.substring(2, 4));
        int shareYear = Integer.parseInt(shareDate.substring(4, 6));

        // Iterate through share types
        for (String shareType : new String[]{ShareModel.EQUITY, ShareModel.BONUS, ShareModel.DIVIDEND}) {
            if (!userShares.get(buyerId).containsKey(shareType)) {
                continue; // Skip if the user has no shares of this type
            }

            // Retrieve registered share IDs
            List<String> registeredIDs = userShares.get(buyerId).get(shareType);

            // Check each share
            for (String shareId : registeredIDs) {
                // Parse the share date from the eventID
                int shareIdMonth = Integer.parseInt(shareId.substring(6, 8));
                int shareIdDay = Integer.parseInt(shareId.substring(8, 10));

                // Check if the share is in the same month and year
                if (shareIdMonth == shareMonth && shareId.substring(0, 4).equals(shareDate.substring(0, 4))) {
                    // Calculate the week number for the share and shareDate
                    int eventWeek = (shareIdDay - 1) / 7;
                    int currentWeek = (shareDay - 1) / 7;

                    // Check if the shares are in the same week
                    if (eventWeek == currentWeek) {
                        limit++;
                        if (limit >= 3) {
                            return true; // Weekly limit exceeded
                        }
                    }
                }
            }
        }

        return false; // Weekly limit not exceeded
    }

    private static int getServerPort(String City) {
        if (City.equalsIgnoreCase("NYK")) {
            return SERVER_PORT_NEWYORK;
        } else if (City.equalsIgnoreCase("LON")) {
            return SERVER_PORT_LONDON;
        } else if (City.equalsIgnoreCase("TOK")) {
            return SERVER_PORT_TOKYO;
        }
        return 1;
    }


    @Override
    public String getShares(String userId) {
        StringBuilder builder = new StringBuilder();
        String response;

        // Query all servers for user's shares, including the local server
        String localServerResponse, otherServer1, otherServer2;
        try {
            localServerResponse = sendUDPMessage(SERVER_PORT_NEWYORK, "getShares", userId, "null", "null");
            otherServer1 = sendUDPMessage(SERVER_PORT_LONDON, "getShares", userId, "null", "null");
            otherServer2 = sendUDPMessage(SERVER_PORT_TOKYO, "getShares", userId, "null", "null");

            builder.append(localServerResponse).append(otherServer1).append(otherServer2);
        } catch (Exception e) {
            builder.append("Failed to retrieve user shares from servers: ").append(e.getMessage());
        }

        response = builder.toString();
        try {
            Logging.serverLog(ServerId, userId, " RMI getShares ", " userId: " + userId + " ", response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }



    /**
     * Retrieves the share type (e.g., "Bonus", "Dividend") for a given share ID.
     *
     * @param shareID The ID of the share (e.g., "NYKE131125").
     * @return The share type (e.g., "Bonus"), or null if the share ID is not found.
     */
    private String getShareTypeFromShareID(String shareID) {
        // Iterate through the outer map (shareType -> inner map)
        for (Map.Entry<String, Map<String, ShareModel>> entry : allShares.entrySet()) {
            String shareType = entry.getKey(); // Get the share type (e.g., "Bonus")
            Map<String, ShareModel> sharesOfType = entry.getValue(); // Get the inner map (shareID -> ShareModel)

            // Check if the inner map contains the shareID
            if (sharesOfType.containsKey(shareID)) {
                return shareType; // Return the share type
            }
        }

        // If the shareID is not found in any share type, return null
        return null;
    }

    @Override
    public String sellShare(String buyerID, String shareID, int shareCount){
        String response;

        // Validate buyer existence
        if (!serverUsers.containsKey(buyerID)) {
            response = "Failed: Buyer " + buyerID + " Not Found";
            logAction(buyerID, shareID, response);
            return response;
        }

        String serverShareId = ShareModel.identifyServer(shareID);

        // Identify the share's home server
        String serverPrefix = shareID.length() >= 3 ? shareID.substring(0, 3) : "";
        if (!ShareModel.identifyServer(shareID).equals(ServerName)) {
            return sendUDPMessage(getServerPort(serverPrefix), "sellShare", buyerID, getShareTypeFromShareID(shareID), shareID, shareCount);
        }

        // Determine the share type from shareID
        String shareType = getShareTypeFromShareID(shareID);
        if (shareType == null) {
            response = "Failed: Invalid Share ID " + shareID;
            logAction(buyerID, shareID, response);
            return response;
        }


        // Ensure the buyer owns shares of this type
        if (!userShares.containsKey(buyerID) || !userShares.get(buyerID).containsKey(shareType)) {
            response = "Failed: Buyer " + buyerID + " Does Not Own Any Shares of Type " + shareType;
            logAction(buyerID, shareID, response);
            return response;
        }

        // Retrieve the buyer's share list
        List<String> shareList = userShares.get(buyerID).get(shareType);
        if (shareList == null || shareList.isEmpty()) {
            response = "Failed: No Shares of " + shareID + " Found for Buyer " + buyerID;
            logAction(buyerID, shareID, response);
            return response;
        }


        // Count the number of shares the buyer owns
        int currentShareCount = Collections.frequency(shareList, shareID);
        if (currentShareCount < shareCount) {
            response = "Failed: Buyer " + buyerID + " Only Owns " + currentShareCount + " Shares of " + shareID;
            logAction(buyerID, shareID, response);
            return response;
        }


        // Remove the requested number of shares safely
        Iterator<String> iterator = shareList.iterator();
        int removedCount = 0;
        while (iterator.hasNext() && removedCount < shareCount) {
            if (iterator.next().equals(shareID)) {
                iterator.remove();
                removedCount++;
            }
        }


        // Cleanup empty lists/maps
        if (shareList.isEmpty()) {
            userShares.get(buyerID).remove(shareType);
            if (userShares.get(buyerID).isEmpty()) {
                userShares.remove(buyerID);
            }
        }

        // Update available shares in stock
        if (allShares.containsKey(shareType) && allShares.get(shareType).containsKey(shareID)) {
            allShares.get(shareType).get(shareID).removeShares(shareCount);
        }

        response = "Success: Sold " + shareCount + " Shares of " + shareID + " for Buyer " + buyerID;
        logAction(buyerID, shareID, response);

        return response+userShares;
    }

    /**
     * Helper method to log the action.
     */
    private void logAction(String buyerID, String shareID, String message) {
        try {
            Logging.serverLog(ServerId, buyerID, "RMI sellShare", "shareId: " + shareID, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String swapShares(String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType) {
        String response = "";

        int availableShareCount = 0;
        // Step 1: Validate buyer existence
        if (!serverUsers.containsKey(buyerID)) {
            response = "Failed: Buyer " + buyerID + " Not Found";
            logAction(buyerID, oldShareID, response);
            return response;
        }

        //Checks if the oldShare is from the current server
        String currentServer = buyerID.substring(0, 3);
        if(!oldShareID.substring(0,3).equals(currentServer)) {
            response = "Old share is not from the current server";
            logAction(buyerID, oldShareID, response);
            return response;
        }

        // Step 2: Check if the buyer owns the old share
        if (!userShares.containsKey(buyerID) || !userShares.get(buyerID).containsKey(oldShareType) ||
                !userShares.get(buyerID).get(oldShareType).contains(oldShareID)) {
            response = "Failed: Buyer " + buyerID + " Does Not Own Share " + oldShareID;
            logAction(buyerID, oldShareID, response);
            return response;
        }

        //no of shares he owns
        int numberOfOldShares = Collections.frequency(userShares.get(buyerID).get(oldShareType), oldShareID);
        System.out.println(": Old Shares found - " + numberOfOldShares);


        //Step 4: Check the availability of the new share on the new server
        String newServerPrefix = newShareID.substring(0, 3);
        int newServerPort = getServerPort(newServerPrefix);
        String newServerResponse = sendUDPMessage(newServerPort, "checkShareAvailability", buyerID, newShareType, newShareID);

        System.out.println("checkShareAvailability New Server response: " + newServerResponse);
        if (!newServerResponse.startsWith("Success:")) {
            response = "Failed: New Share " + newShareID + " Not Available";
            logAction(buyerID, oldShareID, response);
            return response;
        }

        if(newServerResponse.startsWith("Success:")) {
            Pattern pattern = Pattern.compile("\\(Quantity: (\\d+)\\)");
            Matcher matcher = pattern.matcher(newServerResponse);

            if (matcher.find()) {
                availableShareCount = Integer.parseInt(matcher.group(1));
                System.out.println(": New Share NYKM090225 Available Share Count: " + availableShareCount);
            }
            System.out.println(": New Share " + newShareID + " Available Share Count: " + availableShareCount);
        }



        if(availableShareCount < numberOfOldShares){
            response = "Failed: New Share " + newShareID + " Insufficient Quantity";
            logAction(buyerID, oldShareID, response);
            return response;
        }


        // Step 5:  perform the swap (purchase new share and sell old share)
        String purchaseResponse = sendUDPMessage(newServerPort, "purchaseShare", buyerID, newShareType, newShareID, numberOfOldShares);
        if (!purchaseResponse.startsWith("Success:")) {
            response = "Failed: Unable to Purchase New Share " + newShareID;
            logAction(buyerID, oldShareID, response);
            return response;
        }


        String sellResponse = sendUDPMessage(newServerPort, "sellShare", buyerID, oldShareType, oldShareID, numberOfOldShares);
        if (!sellResponse.startsWith("Success:")) {
            response = "Failed: Unable to Sell Old Share " + oldShareID;
            logAction(buyerID, oldShareID, response);
            return response;
        }

        response = "Success: Swapped " + numberOfOldShares + " Shares of " + oldShareID + " with " + newShareID;
        logAction(buyerID, oldShareID, response);

        return response;
    }


    public String listUserSharesUDP(String buyerId) throws RemoteException {
        Map<String, List<String>> shares = userShares.get(buyerId);
        StringBuilder builder = new StringBuilder();
        builder.append(ServerName).append(" Server - Shares for Buyer: ").append(buyerId).append("\n");

        if (shares == null || shares.isEmpty()) {
            builder.append("No shares owned by Buyer ").append(buyerId).append("\n\n=====================================\n");
        } else {
            for (Map.Entry<String, List<String>> entry : shares.entrySet()) {
                builder.append("Share Type: ").append(entry.getKey()).append(" -> ");
                for (String share : entry.getValue()) {
                    builder.append(share).append(" ");
                }
                builder.append("\n");
            }
            builder.append("\n=====================================\n");
        }
        return builder.toString();
    }


    public String listServerAvailabilityUDP(String shareType) throws RemoteException {
        Map<String, ShareModel> shares = allShares.get(shareType);
        StringBuilder builder = new StringBuilder();
        builder.append(ServerName + " Server " + shareType + ":\n");
        if (shares.size() == 0) {
            builder.append("No shares of Type " + shareType);
        } else {
            for (ShareModel share :
                    shares.values()) {
                builder.append(share.toString() + " || ");
            }
        }
        builder.append("\n=====================================\n");
        return builder.toString();
    }

    public String checkShareAvailability(String shareType, String shareID) {
        // Step 1: Check if the share type exists
        if (!allShares.containsKey(shareType)) {
            return "Failed: Share Type " + shareType + " Does Not Exist";
        }

        // Step 2: Check if the share exists
        Map<String, ShareModel> sharesOfType = allShares.get(shareType);
        if (!sharesOfType.containsKey(shareID)) {
            return "Failed: Share " + shareID + " Does Not Exist";
        }

        // Step 3: Check if the share has sufficient capacity
        ShareModel share = sharesOfType.get(shareID);
        int availableShareCount = share.getShareRemainCapacity();

        // Step 4: Return success if the share is available
        return "Success: Share " + shareID + " is Available (Quantity: " + availableShareCount + ")";
    }


    private String sendUDPMessage(int serverPort, String method, String userId, String shareType, String shareId) {
        return sendUDPMessage(serverPort, method, userId, shareType, shareId, 0); // Default shareCount to 0
    }

    private String sendUDPMessage(int serverPort, String method, String userId, String shareType, String shareId, int shareCount) {
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + userId + ";" + shareType + ";" + shareId+ ";" + shareCount;
        try {
            Logging.serverLog(ServerId, userId, " UDP request sent " + method + " ", " shareId: " + shareId + " shareType: " + shareType + " ", " ... ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData(), 0, reply.getLength()).trim();
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        try {
            Logging.serverLog(ServerId, userId, " UDP reply received" + method + " ", " shareID: " + shareId + " shareType: " + shareType + " ", result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }
}

