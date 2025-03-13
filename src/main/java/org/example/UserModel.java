package org.example;


public class UserModel {
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_BUYER = "BUYER";
    public static final String USER_SERVER_NEWYORK = "NEWYORK";
    public static final String USER_SERVER_LONDON = "LONDON";
    public static final String USER_SERVER_TOKYO = "TOKYO";

    private String UserId;
    private String UserType;
    private String UserServer;

    public UserModel(String userId) {
        this.UserId = userId;
        this.UserType = identifyType();
        this.UserServer = identifyServer();
    }

    private String identifyType() {
        if(UserId.substring(3,4).equals("A")) {
            return USER_TYPE_ADMIN;
        }
        else {
            return USER_TYPE_BUYER;
        }
    }

    private String identifyServer() {
        if(UserId.substring(0,3).equals("NYK")){
            return USER_SERVER_NEWYORK;
        }
        else if(UserId.substring(0,3).equals("LON")){
            return USER_SERVER_LONDON;
        }
        else {
            return USER_SERVER_TOKYO;
        }
    }

    public String getUserType() {
        return this.UserType;
    }

    public void setUserType(String userType) {
        this.UserType = userType;
    }

    public String getUserServer() {
        return this.UserServer;
    }

    public void setUserServer(String userServer) {
        this.UserServer = userServer;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        this.UserId = userId;
    }

    @Override
    public String toString() {
        return "User : "+ getUserType()+"\t ID : "+ getUserId()+"\t on Server : "+ getUserServer();
    }

}

