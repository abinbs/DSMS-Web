package org.example;


public class ServerStart {
    public static void main(String[] args) throws Exception {
        Runnable nyTask = () -> new Server("NYK");
        Runnable lonTask = () -> new Server("LON");
        Runnable tokTask = () -> new Server("TOK");

        Thread nyThread = new Thread(nyTask);
        Thread lonThread = new Thread(lonTask);
        Thread tokThread = new Thread(tokTask);

        nyThread.start();
        lonThread.start();
        tokThread.start();
    }
}

