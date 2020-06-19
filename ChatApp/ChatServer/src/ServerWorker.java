import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread{

    private final Socket clientSocket;
    private final Server server;
    private serverUser user;
    private String lname = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();
    private static ArrayList<serverUser> serverUserArrayList = new ArrayList<>();

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;

    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getLname(){
        return lname;
    }

    public String getTopics() { return String.valueOf(topicSet); }

    public List getList() { return serverUserArrayList; }

    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        outputStream = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader((inputStream)));
        String line;
        outputStream.write(("Register & Login to server if you want to chat..." + "\r\n").getBytes());
        outputStream.write(("---To register : register <name> <pw>" + "\r\n").getBytes());
        outputStream.write(("---To login : login <name> <pw>" + "\r\n").getBytes());
        outputStream.write(("---To logoff : logoff" + "\r\n").getBytes());
        outputStream.write(("---To message someone : msg <name> <text>" + "\r\n").getBytes());
        outputStream.write(("---To join a groupchat : join #<name> " + "\r\n").getBytes());
        outputStream.write(("---To message a groupchat : msg #<topic> <text>" + "\r\n").getBytes());
        outputStream.write(("---To check what groupchat you belong to: rooms" + "\r\n").getBytes());
        while ((line = reader.readLine()) != null){

            String[] tokens = line.split(" ");

            if(tokens != null && tokens.length > 0) {


                String command = tokens[0];

                if ("logoff".equalsIgnoreCase(command)) {
                    handleLogoff();
                    break;
                }
                else if("login".equalsIgnoreCase(command)){
                    handleLogin(outputStream, tokens);
                }
                else if("msg".equalsIgnoreCase(command)){
                    String[] tokensMsg = line.split(" ",3);
                    handleMessage(tokensMsg);
                }
                else if("join".equalsIgnoreCase(command)){
                    handleJoin(tokens);
                }
                else if("rooms".equalsIgnoreCase(command)){
                    handleRooms();
                }
                else if("leave".equalsIgnoreCase(command)){
                    handleLeave(tokens);
                }
                else if("register".equalsIgnoreCase(command)){
                    handleRegister(tokens);
                }
                else {
                    String msg = "unknown " + command + "\r\n";
                    outputStream.write(msg.getBytes());
                }

            }
        }
        clientSocket.close();
    }


    private void handleRegister(String[] tokens) {
        String nick = tokens[1];
        String pw = tokens[2];
        addUserToDatabase(nick, pw);

    }

    private void handleLeave(String[] tokens) throws IOException {
        String tok = tokens[1];
        if(topicSet.contains(tok)) {
            topicSet.remove(tok);
            outputStream.write("You've left the topic".getBytes());
        }
    }

    private void handleRooms() throws IOException {
        String msg = getTopics();
        outputStream.write((msg+"\r\n").getBytes());
    }

    public boolean isThisTopicAMemberOfTopics(String topic){
        return topicSet.contains(topic);
    }


    private void handleJoin(String[] tokens) throws IOException {
        if (tokens.length > 1){
            String topic = tokens[1];
            if(tokens[1].charAt(0) == '#') {
                outputStream.write(("You've joined successfuly" + "\r\n").getBytes());
                topicSet.add(topic);
            }
            else{
                outputStream.write(("# is missing" + "\r\n").getBytes());
            }
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList){
            if(isTopic){
                if((worker.isThisTopicAMemberOfTopics(sendTo)) && (topicSet.contains(sendTo))){
                    String outMsg = "[" + sendTo +"] " + lname + ": " + body + "\r\n";
                    worker.send(outMsg);
                }
            }
            else if(sendTo.equalsIgnoreCase(worker.getLname()) && !(sendTo.equalsIgnoreCase(lname))){
                String outMsg = "msg from " + lname + ": " + body + "\r\n";
                worker.send(outMsg);
            }
        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorkerFromList(this);
        List<ServerWorker> workerList = server.getWorkerList();
        //send other online users current user's status
        String onlineMsg = "offline is  " + lname + "\r\n";
        outputStream.write(("you just logged out" + "\r\n").getBytes());
        for(ServerWorker worker : workerList) {
            if (!lname.equals(worker.getLname())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {

        boolean correctLoginInfo = false;
        if (tokens.length == 3){
            String lname= tokens[1];
            String lpw = tokens[2];

            getUsersFromDB();
            for(serverUser user: serverUserArrayList)
            {
                if (((user.getName().equals(lname)) && (user.getPw().equals(lpw))))

                {
                    correctLoginInfo = true;
                }
            }
                if((correctLoginInfo) || (lname.equals("jim") && lpw.equals("jones"))
                                      || (lname.equals("ben") && lpw.equals("bones"))) {
                String msg = "---------correct login info---------" + "\r\n";
                String loggedAs = "---------LOGGED IN AS:" + lname + "\r\n" + "\r\n";
                outputStream.write(msg.getBytes());
                outputStream.write(loggedAs.getBytes());
                this.lname = lname;
                System.out.println("User: " + lname + " logged in!");

                //sends current user all online ppl
                List<ServerWorker> workerList = server.getWorkerList();
                for(ServerWorker worker : workerList) {
                    if (worker.getLname() != null) {
                        if (!lname.equals(worker.getLname())) {
                            if(workerList.size() > 1) {
                                String msg2 = "* Currently online is: " + worker.getLname() + "\r\n";
                                send(msg2);
                            }
                            else if(workerList.size() == 1){
                                outputStream.write("noone is online".getBytes());
                            }
                        }
                    }
                }

                //send other online users current user's status
                String onlineMsg = "* currently online is: " + lname + "\r\n";
                for(ServerWorker worker : workerList) {
                    if (!lname.equals(worker.getLname())) {
                        if(workerList.size() > 1){
                            worker.send(onlineMsg);
                        }
                        else if (workerList.size() == 1){
                            outputStream.write("noone is online".getBytes());
                        }
                    }
                }
            } else {
                String msg = "error in login info" + "\r\n";
                outputStream.write(msg.getBytes());
            }
        }
    }


    private void send(String onlineMsg) throws IOException {
        if (lname != null) {
            outputStream.write(onlineMsg.getBytes());
        }
    }

    public void addUserToDatabase(String name, String pw){
        Connection myConn = null;
        try {
            myConn = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "");
            PreparedStatement pstmt = myConn.prepareStatement("INSERT INTO chatapp.user (Name, Password) " +
                    "VALUES (?,?)");
            pstmt.setString(1, name);
            pstmt.setString(2, pw);
            pstmt.executeUpdate();
            outputStream.write(("User: " + name + " has been registered" + "\r\n").getBytes());

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<serverUser> getUsersFromDB() {
        Connection myConn = null;
        try {
            myConn = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "");
            Statement myStmt = myConn.createStatement();

            String sql = "select * from chatapp.user";
            ResultSet rs = myStmt.executeQuery(sql);

            while (rs.next()) {
                serverUserArrayList.add(new serverUser(rs.getString("Name"), rs.getString("Password")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return serverUserArrayList;
    }

}
