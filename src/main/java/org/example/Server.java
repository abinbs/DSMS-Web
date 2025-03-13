package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {
    private String serverId;
    private String serverName;
    private int serverUdpPort;

    public Server(String serverId) {
        this.serverId = serverId;
        switch (serverId) {
            case "NYK":
                serverName = ServerManagement.SHARE_SERVER_NEWYORK;
                serverUdpPort = ServerManagement.SERVER_PORT_NEWYORK;
                break;
            case "LON":
                serverName = ServerManagement.SHARE_SERVER_LONDON;
                serverUdpPort = ServerManagement.SERVER_PORT_LONDON;
                break;
            case "TOK":
                serverName = ServerManagement.SHARE_SERVER_TOKYO;
                serverUdpPort = ServerManagement.SERVER_PORT_TOKYO;
                break;
        }

        try {
            ServerManagement serObj = new ServerManagement(this.serverId,this.serverName);

            javax.xml.ws.Endpoint.publish("http://localhost:8080/" + this.serverId, serObj);

            System.out.println(serverName + " Server is Up & Running");
            Logging.serverLog(serverId, " Server is Up & Running");

            Runnable task = () -> listenForRequest(serObj, serverUdpPort, serverName, serverId);
            Thread thread = new Thread(task);
            thread.start();


            System.out.println(this.serverName + " started and listening on http://localhost:8080/" + this.serverId);

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);
        }
    }

    private static void listenForRequest(ServerManagement serObj, int serverUdpPort, String serverName, String serverID) {
        DatagramSocket aSocket = null;
        try {
            aSocket = new DatagramSocket(serverUdpPort);
            byte[] buffer = new byte[1000];
            System.out.println(serverName + " UDP Server Started at port " + aSocket.getLocalPort());
            Logging.serverLog(serverID, " UDP Server Started at port " + aSocket.getLocalPort());

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                String receivedData = new String(request.getData(), 0, request.getLength());
                String[] parts = receivedData.split(";");
                String method = parts[0];
                String userId = parts[1];
                String shareType = parts[2];
                String shareId = parts[3];
                int shareCount = Integer.parseInt(parts[4]);

                String response = "";
                if (method.equalsIgnoreCase("listShareAvailability")) {
                    response = serObj.listServerAvailabilityUDP(shareType);
                } else if (method.equalsIgnoreCase("sellShare")) {
                    response = serObj.sellShare(userId, shareId, shareCount);
                } else if (method.equalsIgnoreCase("purchaseShare")) {
                    response = serObj.purchaseShare(userId, shareId, shareType, shareCount);
                }
                else if (method.equalsIgnoreCase("checkShareAvailability")) {
                    response = serObj.checkShareAvailability(shareType, shareId);
                }
                else if (method.equalsIgnoreCase("getShares")) {
                    response = serObj.listUserSharesUDP(userId);
                }

                byte[] sendData = response.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, response.length(), request.getAddress(), request.getPort());
                aSocket.send(reply);
                Logging.serverLog(serverID, userId, " UDP reply sent " + method, " shareId: " + shareId + " shareType: " + shareType, response);
            }
        } catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (aSocket != null) aSocket.close();
        }
    }
}
