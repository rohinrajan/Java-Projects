/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cserver;

import com.sun.glass.ui.MenuItem;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.sql.*;
import javax.swing.JOptionPane;

class Groups {
        public String name;
        public HashSet<String> memberList;
        
        public Hashtable<String,PrintWriter> OutputStream;
        
        public Groups(){
            memberList  = new HashSet<>();
            OutputStream = new Hashtable<String,PrintWriter>();
        }
    }

/**
 *
 * @author rohin
 */
public class CServer {

    private final String SEPERATOR =";";
    
    HashSet<Groups> OverallListOfGroups = new HashSet<>();
    
    HashSet<String> currentOnlineMembers = new HashSet<>(); //list of online members in the group
    
    
    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://localhost:9021/groupchat";
    
    public static CServer servr;
    
    enum UserSelection
    {
        REGISTER,
        LOGIN
    };
       
    
    enum SendMsgStructure
    {
        GroupName,
        Message
    };
    
    enum QueryRequest
    {
      GroupList,
      UserList,
      LoginCredenitals,
      GroupMapping,
      MemberList,
      InsertUserCredentials,
      InsertGroupMapping,
      InsertNewGroupName
    };
      
   // Connectors and statements
   public Connection con = null;
   public Statement stat = null;

       
   //  Database credentials
   static final String USER = "root";
   static final String PASS = "rohin!123";
   
   final String LOGIN = "LOGIN";
   final String REGISTER = "REGISTER";
   final String GROUPLIST = "GROUPLIST";
   
   ResultSet dataSet;
   
   
   
  public CServer (int port){
        InitalizeConnectwithDataBase();
        GenerateGroupDetailsMappingFromDataServer();
        CServerListener(port);
    }
    
  public void CServerListener(int port) {
        ServerSocket socket= null;
        try{
            socket = new ServerSocket(port);
            System.out.println("Chat Server is running");
            while(true){
                Socket Csocket = socket.accept();
                new CServerHandler(Csocket);
            }
        }
        catch(Exception ex){
            System.out.println("Exception: "+ Arrays.toString(ex.getStackTrace()));
        }
        finally{
            try{
                if (socket != null) {
                    socket.close();
                    stat.close();
                    con.close();
                }
            }
            catch(Exception ex){
                System.out.println("Exception occurred in closing the socket connection in the server: "+ex);
            }
        }
    }

    //Method to generate the sql query for varous operations 
    private String GenerateQueryRequests(QueryRequest request){
        String query = null;
        switch(request){
            case GroupMapping:
                query = "SELECT groupnames, usernames as members FROM groupmembers order by groupnames;";
                break;
            case MemberList:
                query = "select usernames from groupmembers where groupnames like ? ;";
                break;
            case LoginCredenitals:
                query = "SELECT username, password FROM usercredentials;";
                break;
            case GroupList:
                query = "SELECT groupnames FROM grouplist";
                break;
            case UserList:
                query = "SELECT username FROM usercredentials";
                break;
            case InsertUserCredentials:
                query = "INSERT INTO usercredentials (username, password) "
                        + "VALUES (?,?);";
                break;
            case InsertGroupMapping:
                query = "INSERT INTO groupmembers(groupnames, usernames) values (?,?);";
                break;
            case InsertNewGroupName:
                query = "INSERT INTO grouplist (groupnames) VALUES (?)";
                break;
        }
        return query;
    }
  
    // Fetch the group details from the mysql database server
    // and store it into the group list 
    private void GenerateGroupDetailsMappingFromDataServer() {
        try{
            Groups grp = null;
            HashSet<String> str = null;
            String sql;
            
            sql = GenerateQueryRequests(QueryRequest.GroupMapping);
            //Executing the sql query and fetching the result
             dataSet = GetData(sql);
            while(dataSet.next()){
                String grpName = dataSet.getString("groupnames");
                String userName = dataSet.getString("members");
                
                //Check to see if the group name is existing 
                //if so then we are adding to the list
                if (null != grp ) {
                    if (grp.name.equalsIgnoreCase(grpName)) {
                        str.add(userName);
                    }
                    else{
                        grp.memberList = str;
                        OverallListOfGroups.add(grp);
                        //Resetting the values for the new group
                        str = new HashSet<>();
                        grp = new Groups();
                        grp.name = grpName;
                        str.add(userName);
                    }
                }
                //if it is a new group then we are adding the 
                else{
                    str = new HashSet<>();
                    grp = new Groups();
                    grp.name = grpName;
                    str.add(userName);
                }
            }
            //Final list to be added into the group
            if (grp != null && grp.name.length() > 0) {
                grp.memberList = str;
                OverallListOfGroups.add(grp);
            }
        }
        catch(Exception ex){
            System.out.println("Exception has occured in "
                    + "GenerateGroupMappingFromDataServer: " +ex);
        }
    }

   //Method to execute the data on the sql server
   private ResultSet GetData(String sql){
       ResultSet result= null;
        try{
            result = stat.executeQuery(sql);
        }
        catch(Exception ex){
            System.out.println("Exception has occured while fetching the data: " +ex);
        }
        return result;
    }    
        
   //Method to establish a connection with the data base
   private void InitalizeConnectwithDataBase() {
        try{
            Class.forName(JDBC_DRIVER);
            System.out.println("Connecting with the sql server");
            con = DriverManager.getConnection(DB_URL, USER, PASS);
            stat = con.createStatement();
        }
        catch(SQLException ex){
            System.out.println("Exception while connecting to a mysql database " + ex);
        }
        catch(ClassNotFoundException ex){
            System.out.println("Class not found while connecting with the mysql database " + ex);
        }
    }
    
       
   public class CServerHandler extends Thread{
        Socket ClientSocket;
        PrintWriter writer;
        BufferedReader reader;
        String LoginName, data;
        String[] dataStream;
        String _groupName;
        boolean isConnected = false;
        String _currentMember;
        ObjectOutputStream objwriter;
        
        final String LOGIN="LOGIN";
        final String REGISTER="REGISTER";
        final String SYSTEM = "SYSTEM";
        final String MEMBERLIST = "MEMBERLIST";
        final String SELECTEDMEMBERLIST = "SELECTEDMEMEMBERLIST";
        final String GROUPLIST = "GROUPLIST";
        final String JOIN = "JOIN";
        final String NEWGROUPNAME="NEWGROUPNAME";
        final String NEWGROUPNAMEACCEPTED = "NEWGROUPNAMEACCEPTED";
        final String GROUPUPDATE = "GROUPUPDATE";
        
        HashSet<String> userJoinedGroups; // list that holds the information of the members on the groups that this member has joined
        
        
        public CServerHandler(Socket socket) throws Exception{
            ClientSocket = socket;
            start();
        }
            
        @Override
        public void run()
        {
            try
            {
                //Initalizing the input and output streams to the socket
                writer = new PrintWriter(ClientSocket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
                objwriter = new ObjectOutputStream(ClientSocket.getOutputStream());
                userJoinedGroups = new HashSet<>();
                //Check to see if the login credentials are correct or not
                while(true){
                    data = reader.readLine();
                    if (data !=null) {
                        if (data.contains(LOGIN)) {
                            dataStream = data.split(SEPERATOR);
                            System.out.println("Recieed the following information from the client for login verification");
                            System.out.println("UserName: " + dataStream[1] + " Password: " + dataStream[2]);
                            //Validate the user credentials and return if it is accepted or not
                             if(ValidateUserCredentials(dataStream[1],dataStream[2], UserSelection.LOGIN)){
                                 WriteMsg("LOGINACCEPTED");
                                 BroadcastMsg(_groupName, " has connected to this Group...", true);
                                 currentOnlineMembers.add(dataStream[1]); //Adding to collection for validation of unique name in registeration
                                 //dont break the loop until the Login or registeration is accepted
                                 data = null;
                                 //format the data to send to the server
                                 for(String mem: userJoinedGroups){
                                     System.out.println(mem);
                                     if (data == null) {
                                         data = mem;
                                         data += SEPERATOR;
                                     }
                                     else {
                                        data += mem;
                                        data += SEPERATOR;
                                     }
                                 }
                                 WriteMsg(data);
                                 break;
                             }
                             else{
                                 WriteMsg("Login failed: either the screen name and password does not match "
                                         + "or the user has already logged in");
                             }
                        }
                        else if(data.contains(REGISTER)){
                            dataStream = data.split(SEPERATOR);
                            System.out.println("Server has recieved new UserName: " + dataStream[1] + " and new Password: " + dataStream[2]);
                            if(ValidateUserCredentials(dataStream[1],dataStream[2],UserSelection.REGISTER)){
                                WriteMsg("LOGINACCEPTED");
                                currentOnlineMembers.add(dataStream[1]); //Adding to collection for reference
                                //break only when the user has been registered successfull
                                break;
                            }
                            else
                                WriteMsg("Register failed as the screen name is already used");
                        }
                    }
                }
                
                //Reading the messages and sending to the corresponding group members
                System.out.println("Ready for messages to be sent to the group memebers");
                while(true){
                    data = reader.readLine();
                    if (data != null && data.trim().length() > 0) {
                        System.out.println("Debug: Message recieved by the client " + data);
                        if (data.contains("USEREXIT")) {
                            String grpName = data.split(SEPERATOR)[1];
                            data = " has exited the application";// resetting the value to show that the user has exited
                            BroadcastMsg(grpName,data,true);
                            currentOnlineMembers.remove(_currentMember);
                        }
                        else if (data.contains("SEND")) {
                            //Remove the send derviative
                            String[] content = data.split(SEPERATOR);
                            BroadcastMsg(content[1],content[2],false);
                        }
                        //Clientreader request for group list 
                        else if (data.equals(GROUPLIST)) {
                            //Fetch the grouplist from the database and send the information to the client
                            String qData = FetchDataToSendToClient(GROUPLIST);
                            //send the formatted data to the client
                            System.out.println("The group names before sending to client: " + qData);
                            writer.println(qData);
                            writer.flush();
                        }
                        //Client request for the members in the selected group
                        else if(data.equalsIgnoreCase(SELECTEDMEMBERLIST)){
                            //Read the selected groupname from the client
                            String selectedGrpName = null;
                            while(selectedGrpName == null){
                                selectedGrpName = reader.readLine();
                            }
                            
                            String qData = FetchDataToSendToClient("MEMBERLIST", selectedGrpName);
                            System.out.println("The list of members of selected group to the client are: " + qData);
                            WriteMsg("MEMBERLIST");
                            WriteMsg(qData);
                        }
                        else if(data.contains(JOIN)){
                            //Split the group name and update the database 
                            String newGrp = data.split(SEPERATOR)[1];
                            boolean isGroupAvailableinOverallListOfGroups=false;
                            if(WriteToDataServer(newGrp,QueryRequest.InsertGroupMapping)){
                                //update the data into Group list
                                for(Groups group: OverallListOfGroups){
                                    if (group.name.equalsIgnoreCase(newGrp)) {
                                        group.memberList.add(_currentMember);
                                        group.OutputStream.put(_currentMember, writer);
                                        isGroupAvailableinOverallListOfGroups = true;
                                        break;
                                    }
                                }
                                //If the group is not available in the overallgroup list then we need to add it to the list
                                //The scenario is when we add a new group but we dont add any information into the overallgrouplist
                                //We'll create the new group and set the value to that group list
                                if (!isGroupAvailableinOverallListOfGroups) {
                                    Groups grp = new Groups();
                                    grp.name = newGrp;
                                    grp.memberList.add(_currentMember);
                                    grp.OutputStream.put(_currentMember, writer);
                                    OverallListOfGroups.add(grp);
                                }
                                
                                //if it is written then we need to add into the grouplist 
                                // and check if the user has already 
                                userJoinedGroups.add(newGrp);
                                WriteMsg("WELCOME TO "+ newGrp + " !...");
                                BroadcastMsg(newGrp ," has connected to this Group...", true);
                            }
                        }
                        else if (data.contains(NEWGROUPNAME)) {
                            String newGrpName = data.split(SEPERATOR)[1];
                            if (WriteToDataServer(newGrpName, QueryRequest.InsertNewGroupName)) {                                
                                //appending the values to the group list
                                Groups newgrp = new Groups();
                                newgrp.name = newGrpName;
                                OverallListOfGroups.add(newgrp); 
                                BroadcastMsgToAll(null, GROUPUPDATE);
                            }
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                System.out.println("Exception occurred in run method on CServerHandler: "+ex);
            }
        }
             
        // Method to get the Group name based on the member name
        private String GetGroupNameBasedonMemberName(String member){
            String grpName = null;
            for(Groups grp: OverallListOfGroups){
                if (grp != null && grp.memberList.contains(member)) {
                    grpName = grp.name;
                    break;
                }
            }
            return grpName;
        }
        
        //Method to validate the user name and password 
        private boolean ValidateUserCredentials(String userName, String passWord, UserSelection selectionMade ) {
            boolean isloginAceepted = false; //flag to check if the login is accepted or not
            try{
                    switch(selectionMade){
                        case LOGIN:
                            HashMap<String,String> UserCredentials = null;
                            //fetch the user credentials from the data base server
                            UserCredentials = FetchDataFromDataServer("USERCREDENTIALS");
                            if (UserCredentials != null && UserCredentials.containsKey(userName)) {
                                //check to see if the screen name is existing or not
                                if (currentOnlineMembers.contains(userName)) {
                                    isloginAceepted = false;
                                }
                                else if (UserCredentials.get(userName).equalsIgnoreCase(passWord)) {                                 
                                    isloginAceepted = true;
                                    AppendingToExistingGroups(userName,GetGroupNameBasedonMemberName(userName),writer);
                                    UpdateCurrentJoinedGroupsList();
                                }
                                else{
                                    isloginAceepted = false;
                                }
                            }
                            else{
                                isloginAceepted = false;
                            }
                            break;
                        case REGISTER:
                            HashSet<String> RgUserList = FetchListOfUsersFromDataServer();
                            if (!RgUserList.contains(userName)) {
                                isloginAceepted = true;
                                //Write the username and password into the database
                                if(WriteToDataServer(userName,passWord) != -1){
                                    System.out.println("Successfull in writing into the database the user credentials");
                                    //Setting the current member name to the username
                                    _currentMember = userName;
                                }
                            }
                            else
                                isloginAceepted = false;
                            break;
                    }
                }
                catch(Exception ex){
                    System.out.println("Exception occured in ValidateUserCredentials: "+ ex);
                    isloginAceepted = false;
                }
            return isloginAceepted;
        }
        
        //Method to get the data from the database and add it to the group list
        private HashMap<String,String> FetchDataFromDataServer (String queryRequest) {
            HashMap<String,String> result=null;
            try {
                switch(queryRequest){
                    case "USERCREDENTIALS":
                        HashMap<String,String> data = new HashMap<>();
                        ResultSet strdata = GetData(GenerateQueryRequests(QueryRequest.LoginCredenitals));
                        while(strdata.next()){
                            String usrnme = strdata.getString("username");
                            String passwd = strdata.getString("password");
                            data.put(usrnme,passwd);
                        }
                        result = data;
                    break;
                }
            }
            catch(Exception ex){
                System.out.println("Exception in FetchDataFromDataServer: "+ex);
            }
            return result;
        }
            
        //Method to add the current output stream into the member list
        private void AppendingToExistingGroups(String userName, String groupName, PrintWriter writer) {
            try{
                System.out.println("Length of the group list: " + OverallListOfGroups.size());
                //Check to see if the groupnames are present if they are present then we need to add it to the list
                if (groupName != null) {
                    System.out.println("GroupName is : " + groupName);
                    _groupName = groupName;
                    for(Groups grp: OverallListOfGroups){
                        for(String member: grp.memberList){
                            if (member != null && member.equalsIgnoreCase(userName)) {
                                System.out.println("Found "+ userName+" in group "+grp.name+" and "
                                                        + "now inserting the corresponding output stream into list");
                                grp.OutputStream.put(userName, writer);
                                break; //breaking from the member list loop
                            }
                        }
                    }
                }
                _currentMember = userName;
            }catch(Exception ex){
                System.out.println("Exception in AppendingToExistingGroups: "+ex);
            }
        }

        //Method to get the memberoutput stream for a given member
        private PrintWriter GetMemberOutputStream(String member,String GroupName) {
            for(Groups grp: OverallListOfGroups){
                if (grp.name.equalsIgnoreCase(GroupName)) {
                    return grp.OutputStream.get(member);
                }
            }
            return null;
        }

        
        //Method to fetch the member list based on the group names given in the sql query
        private String FetchDataToSendToClient(String strcmd,String grpName){
            String result = new String();
            try{
                ResultSet qresult = null;
                //check for the cmd is to fetch the memberlist based on the group name
                if (strcmd.contains("MEMBERLIST")) {
                    PreparedStatement query = con.prepareStatement(GenerateQueryRequests(QueryRequest.MemberList));
                    query.setString(1, grpName);
                    qresult = query.executeQuery(); 
                    if(qresult !=null){
                        while(qresult.next()){
                            //result.add(qresult.getString("username"));
                            result += qresult.getString("usernames");
                            result += SEPERATOR;
                        }
                    }
                }
            }
            catch(Exception ex){
                System.out.println("Exception occured in FetchDataToSendToClient" + ex);
            }
            return result;
        }
        
        
        //Mehod to fetch data from the data base and format it 
        //to string array so that it can be sent to the client
        private String FetchDataToSendToClient(String strcmd){
            String result = new String();
            try{
                ResultSet qresult = null;
                //Check for the cmd is to fetch group list
                if (strcmd.contains("GROUPLIST")) {
                    String sqlcmd = GenerateQueryRequests(QueryRequest.GroupList);
                    qresult = GetData(sqlcmd);
                    if (qresult!=null) {
                        while(qresult.next()){
                            result += qresult.getString("groupnames");
                            result += SEPERATOR;
                        }
                    }
                    //check for the cmd is to fetch the memberlist based on the group name
                    else if (strcmd.contains("MEMBERLIST")) {
                        PreparedStatement query = con.prepareStatement(GenerateQueryRequests(QueryRequest.MemberList));
                        query.setString(1, _groupName);
                        qresult = query.executeQuery(); 
                        if(qresult !=null){
                            while(qresult.next()){
                                result += qresult.getString("usernames");
                                result+= SEPERATOR;
                            }
                        }
                    }
                }
            }
            catch(Exception ex){
                System.out.println("Exception occured in FetchDataToSendToClient " + ex);
            }
            return result;
        }

        //Broadcast message to all the members in the group
        private void BroadcastMsgToAll(String Msg,String Cmd){
            try{
                //Based on the Command we are setting the messages
                switch(Cmd){
                    case GROUPUPDATE:
                        Msg = Cmd;
                    break;
                }
                //Broadcasting the messages to all the members in every group
                for(String member: currentOnlineMembers){
                    String grpName = GetGroupNameBasedonMemberName(member);
                    PrintWriter wrt;
                    if (grpName == null) //in case of registered users we'll get null for group names 
                        wrt = writer;
                    else
                        wrt = GetMemberOutputStream(member, grpName);
                    wrt.println(Msg);
                    wrt.flush();
                    Thread.sleep(100);
                }
                
            }
            catch(Exception ex){
                System.out.println("Exception occured in the BroadcastMsgToAll: "+ex);
            }
        }
        
        //Method to broadcast the message to other members in the group
        private void BroadcastMsg(String GroupName,String Msg, boolean isSystemMsg) {
            try{
                //Formatting the message to be sent to the clients 
                // isSystemMsg flag is used to check if the message is from other member or it
                // is a system message 
                if (isSystemMsg) {
                    Msg = SYSTEM + ": " + _currentMember + Msg;
                }
                else
                    Msg = _currentMember+ ": " + Msg;
                if (userJoinedGroups.contains(GroupName)) {
                        HashSet<String> grpMembers = GetGroupMembersBasedOnGroupName(GroupName);
                        for(String member: grpMembers){
                            PrintWriter wrt = GetMemberOutputStream(member,GroupName);
                            if (wrt == null) {
                                System.out.println("The OutputStream for this member: " +member+ " is not available ");
                            }
                            else{
                                //msg everyone except the current member who has sent the message
                                if (!member.equalsIgnoreCase(_currentMember)) {
                                    wrt.println(Msg);
                                    wrt.flush();
                                }
                            }
                        }
                    }//Message to the client to select the appropriate group in order to send the messahe
                    else
                    {
                        WriteMsg("Please select a joined Group to send the information to");
                    }
            }
            catch(Exception ex){
                System.out.println("Exception occurred in the BroadCaseMsg: "+ex);
            }
        }

        //API for sending the message to the client
        private void WriteMsg(String msg) {
            try{
                writer.println(msg);
                writer.flush();
            }
            catch(Exception ex){
                System.out.println("Exception occured in WriteMsg: "+ex);
            }
        }
        
        //Method to fetch the list of users who registered into the database
        private HashSet<String> FetchListOfUsersFromDataServer() {
            HashSet<String> result = new HashSet<>();
            try{
                ResultSet qdata = GetData(GenerateQueryRequests(QueryRequest.UserList));
                while(qdata.next()){
                    result.add(qdata.getString("username"));
                }
            }
            catch(Exception ex){
                System.out.println("Exception occured in FetchListOfUsersFromDataServer: "+ex);
            }
            return result;
        }

        //Method to wirte the data to the database server 
        private int WriteToDataServer(String userName, String passWord) {
            int result = -1;
            try{
                //Get the insert query to insert into the database
                String insqery = GenerateQueryRequests(QueryRequest.InsertUserCredentials);
                //Execute the prepared statement
                PreparedStatement preStat = con.prepareStatement(insqery);
                preStat.setString(1, userName);
                preStat.setString(2, passWord);
                //Insert the data into the data server
                result = preStat.executeUpdate();
                System.out.println("Result from inserting into the database: " +result);                
            }
            catch(Exception ex){
                System.out.println("Exception occurred in the WriteToDataServer: "+ex);
            }
            return result;
        }

        //Method to write the new group mapping to the database
        private boolean WriteToDataServer(String newGrp, QueryRequest writemode) {
            //Write the data to the database 
            // Update the currentgrouplist for the user
            boolean isWritten = false;
            try{
                PreparedStatement prepStat = con.prepareStatement(GenerateQueryRequests(writemode));
                switch(writemode){
                    case InsertGroupMapping:
                            prepStat.setString(1, newGrp);
                            prepStat.setString(2, _currentMember);
                            if(prepStat.executeUpdate() > 0){
                                isWritten = true;
                            }
                        break;
                    case InsertNewGroupName:
                            prepStat.setString(1, newGrp);
                            if (prepStat.executeUpdate() > 0) {
                                isWritten = true;
                            }
                        break;
                }
            }
            catch(Exception ex){
                System.out.println("Exception occurred in the WriteToDataServer: " +ex);
                isWritten = false;
            }
            return isWritten;
        }
        
        //Method to Get the Group members based on the group name
        private HashSet<String> GetGroupMembersBasedOnGroupName(String groupName) {
            HashSet<String> rtnMembers = null;
            try{                
                for(Groups grp: OverallListOfGroups){
                    if (grp.name.equalsIgnoreCase(groupName)) {
                        rtnMembers = grp.memberList;
                        break;
                    }
                }
            }
            catch(Exception ex){
                System.out.println("Ëxception occurred on GetGroupMembersBasedOnGroupName: "+ex);
            }
            return rtnMembers;
        }
        
        //Method to update the current joined groups list in case of users who have more than 
        //one group they have joined
        private void UpdateCurrentJoinedGroupsList() {
            try{
                //Iterate the Overall Groups and get the group mapping
                //based on the current member
                for(Groups grp: OverallListOfGroups){
                    if (grp.memberList.contains(_currentMember)) {
                        userJoinedGroups.add(grp.name);
                    }
                }
            }
            catch(Exception ex){
                System.out.println("Exception occurred in UpdateCurrentJoinedGroups: "+ex);
            }
        }
        
    }    
        
   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) {
        try {
             servr = new CServer(9011); //Calling the Class constructor and passing the port number         
        }
        catch(Exception ex){
            System.out.println("Exception occured in the main: "+ ex);
        }
    }
}
