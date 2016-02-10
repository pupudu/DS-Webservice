package com.sid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.net.Inet4Address;
import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

class Node {

    private String ip;
    private int port;
    private String username;

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Node(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return username + "@" + ip + ":" + port;
    }

}


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * @author Shan
 */
 
@Path("/main")
public class Main {

    private final String SERVER_HOST = "sid.projects.mrt.ac.lk";
    private final int SERVER_PORT = 3000;

    //#############################################################
    //######################  IMPORTENT  ##########################
    //#############################################################
    //
    //Change this count everytime you run the project :)
    //
    //#############################################################
    private int count = 3;

    private String MY_IP = "localhost";
    private int MY_PORT;
    //            = 300 + count;
    private String MY_USERNAME;
    //    = "Shan123" + count;
    private Socket clientSocket;

    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    private LinkedList<Node> routingTable = new LinkedList<>();

    private LinkedList<String> movieList;

    private Random random = new Random();

    Gson gson = new Gson();

    public Main() {
        MY_PORT = Integer.parseInt("300" + count);
        MY_USERNAME = "Shan1234" + count;
    }

    @GET
    @Path("/register/")
    public Response start() {

        movieList = new LinkedList<>();
        loadMovies();

        boolean connected = createConnection();

        if (connected) {

            register();

            try {

                String reply = inFromServer.readLine();
                System.out.println("REPLY FROM SERVER: " + reply);

                String[] temp = reply.split(" ");

                Node node;

                try {
                    int n = Integer.parseInt(temp[2]);

                    if (n > 0) {

                        for (int i = 0; i < n; i++) {
                            node = new Node(temp[3 * (i + 1)], Integer.parseInt(temp[3 * (i + 1) + 1]), temp[3 * (i + 1) + 2]);
                            routingTable.add(node);
                            System.out.println("Node: " + node);
                        }

                        System.out.println("Routing Table initial size: " + routingTable.size());

                        //If the routing table contains more than 2 entries, we should remove the excess entries
                        Collections.shuffle(routingTable);

                        while (routingTable.size() > 2) {
                            routingTable.remove();
                        }

                        System.out.println("Routing Table final size: " + routingTable.size());
                    }

                } catch (Exception e) {
                    System.out.println("ExTbl: " + e);
                }

                joinDistributedSystem();
                String output = gson.toJson(routingTable);
                return Response.status(200).entity(output).build();
                //createUDPListener();
                //processQueries();
            } catch (Exception ex) {
                System.out.println("Ex1: " + ex);
            }
        }
        return Response.status(200).entity("Error Occured").build();
    }

    private void loadMovies() {

        try {
//            FileReader fileReader = new FileReader(new File("FileNames.txt"));
            File file = new File("FileNames.txt");
           // System.out.println(file.getAbsolutePath());
            Scanner scanner = new Scanner(file);

            Random random = new Random();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
//                System.out.println("Line: " + line);

                if (random.nextBoolean()) {
                    movieList.add(line);
                }
            }

            Collections.shuffle(movieList);

            int len = movieList.size() / 2;

            for (int i = 0; i < len; i++) {
                movieList.remove();
            }

            System.out.println(movieList);

        } catch (FileNotFoundException ex) {
            System.out.println("ExL: " + ex);
        }
    }

    private boolean createConnection() {
        try {
            MY_IP = Inet4Address.getLocalHost().getHostAddress();
            //System.out.println("My IP: " + MY_IP);

            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);

            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            return true;

        } catch (Exception e) {
            System.out.println("ExCC: " + e);
            return false;
        }
    }

    private void register() {
        try {
            String msg = " REG " + MY_IP + " " + MY_PORT + " " + MY_USERNAME;
//            System.out.println("MSG: " + msg + " , LENGTH: " + msg.length());
//            System.out.println("COMPLETE MSG: " + "00" + (msg.length() + 4) + msg);
            outToServer.write(("00" + (msg.length() + 4) + msg).getBytes());
        } catch (Exception e) {
            System.out.println("ExR: " + e);
        }
    }

    @POST
    @Path("/call/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    private void createUDPListener(CallBean call) {

        System.out.println("Creating listener on port: " + MY_PORT);
        try {

            String msg = call.getMessage();
            String[] temp = msg.split(" ");

            String IPAddress = call.getIp();
            String port = call.getPort();

            String reply;

            switch (temp[1]) {
                case "JOIN":
                    boolean ok = processJoinMsg(temp[2], temp[3]);
                    if (ok) {
                        reply = "0013 JOINOK 0";
                    } else {
                        reply = "0016 JOINOK 9999";
                    }
                    sendPost(IPAddress, port, "call", reply);
                    break;
                case "LEAVE":
                    System.out.println("Leaving...");
                    break;
                case "SER":
                    System.out.println("Searching...");

                    LinkedList<String> tempList = new LinkedList<>();

                    for (String movie : movieList) {
                        if (movie.toLowerCase().trim().contains(temp[4].toLowerCase().trim())) {
                            tempList.add(movie);
                        }
                    }
                    System.out.println("Temp List: " + tempList);

                    if (tempList.size() == 0) {
                        int hops;
                        try {
                            hops = Integer.parseInt(temp[5]);
                        } catch (Exception e) {
                            hops = 0;
                        }

                        if (hops > 0) {

                        } else {
                            reply = "SEROK 0";
                            reply = "00" + reply.length() + " " + reply;
                            sendPost(IPAddress, port, "call", reply);
                        }

                    } else {
                        System.out.println("Sending reply");

                        reply = "SEROK 0";
                        for (int i = 0; i < tempList.size(); i++) {
                            reply = reply + " " + tempList.get(i);
                        }

                        reply = reply.length() + " " + reply;
                        if (reply.length() > 99) {
                            reply = "0" + reply;
                        } else if (reply.length() > 9) {
                            reply = "00" + reply;
                        }

                        sendPost(temp[2], temp[3], "call", reply);
                    }
                    break;
                default:
                    System.out.println("xx");
                    break;
            }
        } catch (Exception ex) {
            System.out.println("ExUDP: " + ex);
        }
    }

    private boolean processJoinMsg(String ip, String port) {

        System.out.println("ip: " + ip);
        System.out.println("port: " + port);
        int intPort = Integer.parseInt(port.trim());

        routingTable.add(new Node(ip, intPort));
        return true;
    }

    private void joinDistributedSystem() {
        System.out.println("joinDistributedSystem()");
        try {
            String message = "JOIN " + MY_IP + " " + MY_PORT;
            message = "00" + (message.length() + 5) + " " + message;

            for (Node node : routingTable) {
                String address = node.getIp();
                String port = Integer.toString(node.getPort());
                sendPost(address, port, "call", message);
                System.out.println("message sent");
            }
        } catch (Exception e) {
            System.out.println("ExJ: " + e);
        }
    }

    /*
     private void processQueries() {
     new Thread(new Runnable() {
     @Override
     public void run() {
     Scanner scanner = new Scanner(System.in);
     System.out.print("Enter movie name: ");
     while (true) {
     String name = scanner.nextLine();
     System.out.println(name + " entered");
     try {
     String message = "SER " + MY_IP + " " + MY_PORT + " " + name;
     message = "00" + (message.length() + 5) + " " + message;
						
     for (Node node : routingTable) {
     String address = node.getIp();
     String port = node.getPort();
     sendPost(address,port,"search","msg="+message);
     System.out.println("message sent");
     }
     } catch (Exception ex) {
     System.out.println("Ex: " + ex);
     }
     }
     }
     }).start();
     }*/
    @POST
    @Path("/search/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    private void processQueriesService(CallBean call) {

        String name = call.getMessage();
        System.out.println(name + " entered");
        try {
            String message = "SER " + MY_IP + " " + MY_PORT + " " + name;
            message = "00" + (message.length() + 5) + " " + message;

            for (Node node : routingTable) {
                String address = node.getIp();
                String port = Integer.toString(node.getPort());
                sendPost(address, port, "call", message);
                System.out.println("message sent");
            }
        } catch (Exception ex) {
            System.out.println("ExQ: " + ex);
        }

    }

    private void sendPost(String ip, String port, String path, String message) throws Exception {
        String url = ip + ":" + port + "/RESTfulExample/main/" + path;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("Content-Type", "application/json");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());

        CallBean call = new CallBean();
        call.setIp(ip);
        call.setPort(port);
        call.setMessage(message);

        String payload = gson.toJson(call);

        wr.writeBytes(payload);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        System.out.println(response.toString());
    }
}
