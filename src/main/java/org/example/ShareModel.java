package org.example;


import java.util.ArrayList;
import java.util.List;


public class ShareModel {
    public static final String SHARE_TIME_MORNING = "Morning";
    public static final String SHARE_TIME_AFTERNOON = "Afternoon";
    public static final String SHARE_TIME_EVENING = "Evening";
    //TODO check
    public static final String SHARE_SERVER_NEWYORK = "NEWYORK";
    public static final String SHARE_SERVER_LONDON = "LONDON";
    public static final String SHARE_SERVER_TOKYO = "TOKYO";
    public static final String EQUITY = "Equity";
    public static final String BONUS = "Bonus";
    public static final String DIVIDEND = "Dividend";
    public static final int ADD_SUCCESS = 1;
    public static final int SHARE_FULL = 0;
    public static final int ADD_FAILURE = -1;

    private String ShareType;
    private String ShareId;
    private String ShareServer;
    private int ShareCapacity;
    private String ShareDate;
    private String ShareTimeSlot;
    private int PurchasedShares;
    private List<String> RegisteredUsers;

    public ShareModel(String shareType, String shareId, int shareCapacity) {
        this.ShareType = shareType;
        this.ShareId = shareId;
        this.ShareCapacity = shareCapacity; //total shares
        this.PurchasedShares = 0;
        this.ShareServer = identifyServer(shareId);
        this.ShareDate = identifyTime(shareId);
        this.ShareTimeSlot = identifyTimeSlot(shareId);
        RegisteredUsers = new ArrayList<String>();
    }

    public static String identifyServer(String shareId) {
        // Null check
        if (shareId == null) {
            return "UNKNOWN"; // Or throw an exception: throw new IllegalArgumentException("shareId cannot be null");
        }
        if(shareId.substring(0,3).equals("NYK")) {
            return SHARE_SERVER_NEWYORK;
        }
        else if(shareId.substring(0,3).equals("LON")) {
            return SHARE_SERVER_LONDON;
        }
        else {
            return SHARE_SERVER_TOKYO;
        }
    }

    public String identifyTimeSlot(String shareId) {
        if(shareId.substring(3,4).equals("M")) {
            return SHARE_TIME_MORNING;
        }
        else if(shareId.substring(3,4).equals("A")) {
            return SHARE_TIME_AFTERNOON;
        }
        else {
            return SHARE_TIME_EVENING;
        }
    }

    public String identifyTime(String shareId) {
        return shareId.substring(4, 6) + "/" + shareId.substring(6, 8) + "/20" + shareId.substring(8, 10);
    }

    public String getShareType() {
        return this.ShareType;
    }

    public void setShareType(String shareType) {
        this.ShareType = shareType;
    }

    public String getShareId() {
        return this.ShareId;
    }

    public void setShareId(String shareId) {
        this.ShareId = shareId;
    }

    public String getShareServer() {
        return this.ShareServer;
    }

    public void setShareServer(String shareServer) {
        this.ShareServer = shareServer;
    }

    public int getShareCapacity() {
        return ShareCapacity;
    }

    public void setShareCapacity(int shareCapacity) {
        this.ShareCapacity = shareCapacity;
    }

    public String getShareDate() {
        return this.ShareDate;
    }

    public void setShareDate(String shareDate) {
        this.ShareDate = shareDate;
    }

    public String getShareTimeSlot() {
        return this.ShareTimeSlot;
    }

    public void setShareTimeSlot(String shareTimeSlot) {
        this.ShareTimeSlot = shareTimeSlot;
    }

    public int getPurchasedShares() {
        return this.PurchasedShares;
    }

    public int getShareRemainCapacity() {
        return this.ShareCapacity - this.PurchasedShares;
    }

    public boolean isFull() {
        return getShareRemainCapacity() == 0; // Share is full if remaining capacity is 0
    }

    public boolean isEmpty() {
        return this.PurchasedShares == 0; // Share is empty if no shares are purchased
    }

    public List<String> getRegisteredUserIds() {
        return RegisteredUsers;
    }


    public int addRegisteredUser(String buyerId) {
        return addRegisteredUser(buyerId, 1); // Default to purchasing 1 share
    }

    public int addRegisteredUser(String buyerId, int shareCount) {
        if (shareCount <= 0) {
            return ADD_FAILURE; // Invalid share count
        }

        if (getShareRemainCapacity() < shareCount) {
            return SHARE_FULL; // Not enough shares available
        }

        // Update the number of purchased shares
        PurchasedShares += shareCount;

        // Add the buyer to the registered users list
        if (!RegisteredUsers.contains(buyerId)) {
            RegisteredUsers.add(buyerId);
        }

        return ADD_SUCCESS;
    }

    public boolean removeRegisteredUser(String registeredUserId) {
        return RegisteredUsers.remove(registeredUserId);
    }

    public int addShares(int shareCount) {
        if (shareCount <= 0) {
            return ADD_FAILURE; // Invalid share count
        }

        if (getShareRemainCapacity() < shareCount) {
            return SHARE_FULL; // Not enough shares available
        }

        // Update the number of purchased shares
        this.PurchasedShares += shareCount;

        return ADD_SUCCESS;
    }

    public int removeShares(int shareCount) {
        if (shareCount <= 0) {
            return ADD_FAILURE; // Invalid share count
        }

        if (this.PurchasedShares < shareCount) {
            return ADD_FAILURE; // Not enough shares to remove
        }

        // Update the number of purchased shares
        PurchasedShares -= shareCount;

        return ADD_SUCCESS;
    }




    @Override
    public String toString() {
        return String.format(
                "Share ID: %s | Date: %s | Time Slot: %s | Capacity: %d [Remaining: %d]",
                getShareId(),
                getShareDate(),
                getShareTimeSlot(),
                getShareCapacity(),
                getShareRemainCapacity()
        );
    }

}

