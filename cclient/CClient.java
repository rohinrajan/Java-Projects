/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.HashSet;
import javax.swing.JOptionPane;


/**
 *
 * @author rohin
 */
public class CClient extends javax.swing.JFrame {

    Socket sock;
    BufferedReader reader;
    PrintWriter writer;
    
    ObjectInputStream objReader;
    boolean isConnnected = false;
    ResultSet dataSet;
    javax.swing.JFrame opt = this;
    
    
    final String MEMBERLIST = "MEMBERLIST";
    final String SEPERATOR = ";";
    final String LOGINACCEPTED = "LOGINACCEPTED";
    final String LOGINREJECTED = "LOGINREJECTED";
    final String NEWGROUPNAME = "NEWGROUPNAME";
    final String NEWMEMBERNAMEADDED = "NEWMEMBERNAMEADDED"; //Command to add a member to the group
    final String GROUPUPDATE = "GROUPUPDATE"; //Command to update the group list
    
    CClientListener cclientlistener;
    ActionListener cbox_selection;
    
    HashSet<String> currentGrpList;
    
    
    private void SetComboxToGroupList(String[] groupList) {
        cbox_GroupList.removeAllItems();
        
        for(String group: groupList)
        {
            System.out.println(group);
            cbox_GroupList.addItem(group);
        }
    }

    //Method to Display an exception to the user if there is a problem with the server connection
    public void ShowException(){
        
        JOptionPane.showMessageDialog(this,  "Issue with server connection,"
                                           + " please close the application and try again", 
                                              "Unable to connect to Server",JOptionPane.ERROR_MESSAGE);
        //Disable the components for the user
        btn_Send.setEnabled(false);
        cbox_GroupList.setEnabled(false);
        btn_Join.setEnabled(false);
        btn_CreateGroup.setEnabled(false);
    }
    
    //Method to define the action listener for combobox selection
    private ActionListener DefineActionListener() {
         cbox_selection = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    String selectedItem = cbox_GroupList.getSelectedItem().toString();
                    if (selectedItem != null) {
                        //Send the cmd to server to select based on the group name
                        WriteMessage("SELECTEDMEMEMBERLIST");
                        //send the groupname selected
                        WriteMessage(selectedItem);
                        
                        //Check if the current group has been joined or not
                        EnableOrDisableJoinBtn(selectedItem);
                    }
                }
                catch(Exception ex){
                    System.out.println("Excepton is actionlistener for combobox: " +ex);
                }            
            }
        };
        return cbox_selection;
    }
    
    //Method to Enable to disable the join button based on groupname
    private void EnableOrDisableJoinBtn(String groupName) {
        boolean isJoinBtnEnabled = true;
        //if the clientgrpList is null during registeration then returnback
        if(currentGrpList.isEmpty())
            return;
        //enabling or disabling the join group button
        for(String grp: currentGrpList){
            if(grp.equalsIgnoreCase(groupName)){
                isJoinBtnEnabled = false;
                break;
            }
        }
        //enable or disable based on the flag value
        btn_Join.setEnabled(isJoinBtnEnabled);
        
    }
    
    //Method to write message to the server
    private void WriteMessage(String message) throws Exception{
        try{
            //Before sending any message we check to see if the server is connected or not
            writer.println(message);
            writer.flush();
        }
        catch(Exception ex){
            System.out.println("Exception occured in the WriteMessage: " +ex);
            throw ex;
        }
    }

    
    //Method to read the data from the serber
    private String ReadMessage() throws Exception {
        String data = null;
            try{
                while(data == null){
                    data = reader.readLine();
                }
            }
            catch(Exception ex){
                System.out.println("Exception has occured in the ReadMessage:" +ex);
                throw ex;
            }
        return data;
    }

    //Method to update the group members and update list 
    private void FillGroupAndMemberList() {
        String data;
        try{
            
           
            if (cbox_selection != null) {
                cbox_GroupList.removeActionListener(cbox_selection);
            }
            //get the group list from the server
            //Request server for group details
            WriteMessage("GROUPLIST");
            //Read the group list
            String[] grpNames;
            //Read the message and get the information 
            data = ReadMessage();
            grpNames = data.split(SEPERATOR);
            
            //Setting the combo list to the availale group list
            SetComboxToGroupList(grpNames);
            isConnnected = true;

            //Get the current members of the selected group
            WriteMessage("SELECTEDMEMEMBERLIST");
            data = cbox_GroupList.getSelectedItem().toString();
            WriteMessage(data);
            //Disabling the join button if set as default
            EnableOrDisableJoinBtn(data);
            //sending the message to the server to request for the members of the group
            String[] memberList;
            //Reading the messages from the server
            data = ReadMessage();
            // checking for member list 
            if (data.equalsIgnoreCase(MEMBERLIST)) {
                //Read the list of members from the server
                data = ReadMessage();
                memberList = data.split(SEPERATOR);
                lst_memberList.setListData(memberList);
            }
            
            //Set the combobox with the action listener
            cbox_GroupList.addActionListener(DefineActionListener());
        }
        catch(Exception ex){
             System.out.println("Exception has occured in the FillGroup: " +ex);   
        }
    }

    //Method to start client listener to read the incoming messages 
    //This would also disable the components to prevent relogin
    private void StartClientListenerAndDisabledComponents() {
        cclientlistener = new CClientListener();
        cclientlistener.start();
        //Disable all the components to prevent from relogin 
        txt_ScreenName.setEnabled(false);
        txt_passwd.setEnabled(false);
        btn_login.setEnabled(false);
        btn_Register.setEnabled(false);


        //setting the title of the name of the client application
        //after login is accepted
        opt.setTitle(txt_ScreenName.getText() + " - Client Chat Application");
    }
    
    //Class that would create to read for any incomming messages
    public class CClientListener extends Thread {
        public void run() {
            try{
                System.out.println("Starting the client listener for reading messages");
                String data;
                while(true){
                    data = ReadMessage();
                    if (data != null) {
                        if (data.equalsIgnoreCase(MEMBERLIST)) {
                            data = ReadMessage();
                            String[] memberList = data.split(SEPERATOR);
                            lst_memberList.setListData(memberList);
                        }
                        else if(data.equalsIgnoreCase(GROUPUPDATE)){
                            //Updating the group list 
                            System.out.println("Recieved an group updaate will update the list now!..");
                            FillGroupAndMemberList();
                        }
                        else{
                            if (txt_msgArea.getText().length() > 0) {
                                txt_msgArea.append("\n"); //new line character spacing for formatting
                                txt_msgArea.append(data);
                            }
                            else
                                txt_msgArea.setText(data);
                       }
                    }
                }                
            }catch(Exception ex){
                System.out.println("CClientListener exception:" +ex);
                ShowException();
            }
        }
    }
        
    /**
     * Creates new form CClient
     */
    public CClient() {
        try {
            initComponents();
            sock = new Socket("127.0.0.1",9011);
            InputStreamReader streamreader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(streamreader);
            writer = new PrintWriter(sock.getOutputStream());
            writer.flush(); 
            opt.setTitle("ChatApplication");
            objReader = new ObjectInputStream(sock.getInputStream());
            currentGrpList = new HashSet<>();
        }
        catch(Exception ex){
            System.out.println("Exception occured in cclient:" + ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnl_ChatWindow = new javax.swing.JPanel();
        lbl_ScreenName = new javax.swing.JLabel();
        lbl_Password = new javax.swing.JLabel();
        txt_ScreenName = new javax.swing.JTextField();
        txt_passwd = new javax.swing.JPasswordField();
        btn_login = new javax.swing.JButton();
        btn_Register = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txt_msgArea = new javax.swing.JTextArea();
        txt_entermsg = new javax.swing.JTextField();
        btn_Send = new javax.swing.JButton();
        pnl_GroupMembers = new javax.swing.JPanel();
        scrpnl_memberList = new javax.swing.JScrollPane();
        lst_memberList = new javax.swing.JList();
        btn_Join = new javax.swing.JButton();
        cbox_GroupList = new javax.swing.JComboBox();
        lbl_groups = new javax.swing.JLabel();
        lbl_memberList = new javax.swing.JLabel();
        btn_CreateGroup = new javax.swing.JButton();
        lbl_GroupName = new javax.swing.JLabel();
        lbl_CurrentScreenName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("MainClient"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lbl_ScreenName.setText("ScreenName");

        lbl_Password.setText("Password");

        btn_login.setText("Login");
        btn_login.setToolTipText("");
        btn_login.setActionCommand("");
        btn_login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_loginActionPerformed(evt);
            }
        });

        btn_Register.setText("Register");
        btn_Register.setToolTipText("");
        btn_Register.setActionCommand("");
        btn_Register.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_RegisterActionPerformed(evt);
            }
        });

        txt_msgArea.setEditable(false);
        txt_msgArea.setColumns(20);
        txt_msgArea.setRows(5);
        jScrollPane1.setViewportView(txt_msgArea);

        txt_entermsg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_entermsgActionPerformed(evt);
            }
        });

        btn_Send.setText("Send");
        btn_Send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SendActionPerformed(evt);
            }
        });

        scrpnl_memberList.setViewportView(lst_memberList);

        btn_Join.setText("Join");
        btn_Join.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_JoinActionPerformed(evt);
            }
        });

        cbox_GroupList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lbl_groups.setText("Groups");

        lbl_memberList.setText("Member List");

        btn_CreateGroup.setText("Create");
        btn_CreateGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_CreateGroupActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_GroupMembersLayout = new javax.swing.GroupLayout(pnl_GroupMembers);
        pnl_GroupMembers.setLayout(pnl_GroupMembersLayout);
        pnl_GroupMembersLayout.setHorizontalGroup(
            pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_GroupMembersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbox_GroupList, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_groups)
                    .addComponent(btn_Join)
                    .addComponent(btn_CreateGroup))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addGroup(pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrpnl_memberList, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbl_memberList))
                .addContainerGap())
        );
        pnl_GroupMembersLayout.setVerticalGroup(
            pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl_GroupMembersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_groups)
                    .addComponent(lbl_memberList))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_GroupMembersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_GroupMembersLayout.createSequentialGroup()
                        .addComponent(cbox_GroupList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(78, 78, 78)
                        .addComponent(btn_Join)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_CreateGroup)
                        .addContainerGap())
                    .addComponent(scrpnl_memberList)))
        );

        lbl_GroupName.setText("Group: Not Set");

        lbl_CurrentScreenName.setText("ScreenName: Not Set");
        lbl_CurrentScreenName.setToolTipText("");

        javax.swing.GroupLayout pnl_ChatWindowLayout = new javax.swing.GroupLayout(pnl_ChatWindow);
        pnl_ChatWindow.setLayout(pnl_ChatWindowLayout);
        pnl_ChatWindowLayout.setHorizontalGroup(
            pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                        .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pnl_GroupMembers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                                        .addGap(47, 47, 47)
                                        .addComponent(btn_login)
                                        .addGap(18, 18, 18)
                                        .addComponent(btn_Register))
                                    .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(lbl_ScreenName)
                                            .addComponent(lbl_Password))
                                        .addGap(18, 18, 18)
                                        .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txt_passwd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txt_ScreenName, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(10, 10, 10))
                    .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbl_GroupName)
                        .addGap(42, 42, 42)
                        .addComponent(lbl_CurrentScreenName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                        .addComponent(txt_entermsg)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_Send)
                        .addGap(8, 8, 8))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 283, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        pnl_ChatWindowLayout.setVerticalGroup(
            pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17)
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_entermsg, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_Send, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addGroup(pnl_ChatWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_ScreenName)
                    .addComponent(txt_ScreenName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_Password)
                    .addComponent(txt_passwd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_login)
                    .addComponent(btn_Register))
                .addGap(24, 24, 24)
                .addGroup(pnl_ChatWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_GroupName)
                    .addComponent(lbl_CurrentScreenName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnl_GroupMembers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_ChatWindow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnl_ChatWindow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_loginActionPerformed
        try {
            System.out.println("Login Button Pressed");
            if (txt_ScreenName.getText() != null && txt_passwd.getPassword() !=null ){
                String data = "LOGIN" + SEPERATOR;
                data += txt_ScreenName.getText() + SEPERATOR;
                data += new String(txt_passwd.getPassword());
                WriteMessage(data);
                System.out.println("Sent the login information to the server");
                if (reader != null) {
                    data = ReadMessage();
                    if (data !=null) {
                        System.out.println("Message sent from the server after verification: "+ data);
                        
                        // if the login is accepted then we fill the group lists and then start the client listener
                        if (data.equalsIgnoreCase("LOGINACCEPTED")) {
                            JOptionPane.showMessageDialog(this, "Login is accepted","WelcomeBack..",JOptionPane.INFORMATION_MESSAGE);
                            data = ReadMessage();
                            String[] clientGrpList = data.split(SEPERATOR);
                            for(String grp: clientGrpList){
                                currentGrpList.add(grp);
                            }
                            //setting the group names and the screen name labels
                            if (clientGrpList.length > 1) {
                                lbl_GroupName.setText(lbl_GroupName.getText().replace("Not Set", "Multiple Groups"));
                            }
                            else
                                lbl_GroupName.setText(lbl_GroupName.getText().replace("Not Set", clientGrpList[0]));
                            lbl_CurrentScreenName.setText(lbl_CurrentScreenName.getText().
                                                    replace("Not Set", txt_ScreenName.getText()));
                            FillGroupAndMemberList();
                            StartClientListenerAndDisabledComponents();
                            //Here if there are more than two groups are present for a user then only the first group is set
                            cbox_GroupList.setSelectedItem(clientGrpList[0]);
                        }
                        else
                            JOptionPane.showMessageDialog(this, data,"Login Rejected", JOptionPane.INFORMATION_MESSAGE);//show login has been rejected 
                    }                    
                }
            }
        }
        catch(Exception ex){
           System.out.println("Exception occurred on btn_loginActionPerformed: "+ ex);
        }
    }//GEN-LAST:event_btn_loginActionPerformed
    
    //Method to register the user to the server
    private void btn_RegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_RegisterActionPerformed
        try{
            System.out.println("Register button is clicked");
            if (txt_ScreenName.getText() != null && txt_passwd.getPassword() !=null ){
                String data = "REGISTER" + SEPERATOR;
                data += txt_ScreenName.getText() + SEPERATOR;
                data += new String(txt_passwd.getPassword());
                WriteMessage(data);
                System.out.println("Sent the registeration information to the server");
                
                //Checking to see if the verification has passed or not
                data = ReadMessage();
                if (data.equalsIgnoreCase(LOGINACCEPTED)) {
                    JOptionPane.showMessageDialog(this, "Registeration is successfull" ,"Success",JOptionPane.INFORMATION_MESSAGE);
                    FillGroupAndMemberList();
                    StartClientListenerAndDisabledComponents();
                    lbl_CurrentScreenName.setText(lbl_CurrentScreenName.getText().
                                                    replace("Not Set", txt_ScreenName.getText()));
                }
                else
                    JOptionPane.showMessageDialog(this, data,"Registeration failed",JOptionPane.INFORMATION_MESSAGE);
            }
        }
        catch(Exception ex){
            System.out.println("Exception occurred in the Register button click: " +ex);
        }
    }//GEN-LAST:event_btn_RegisterActionPerformed
    //Method to send the text to the other group members
    private void btn_SendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_SendActionPerformed
        try{
            if (txt_entermsg.getText().length() > 0) {
                System.out.println("Sending msg " + txt_entermsg.getText() + " to server");
                String data = "SEND" + SEPERATOR + cbox_GroupList.getSelectedItem().toString() + SEPERATOR 
                        + txt_entermsg.getText() + "\n";
                WriteMessage(data); //Write the data to the server
                //clear the txt_message area
                txt_entermsg.setText(""); //clearing the messages
            }
        }
        catch(Exception ex){
            System.out.println("Exception occurred in btn_SendActionPerformed: " +ex);
        }
    }//GEN-LAST:event_btn_SendActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try{
            writer.println("USEREXIT" +  SEPERATOR + cbox_GroupList.getSelectedItem());
            writer.flush();
            //Stoping the thread 
            cclientlistener.interrupt();
            System.out.println("Closing the socket connection");
            sock.close();
            reader.close();
            writer.close();
        }
        catch(Exception ex){
            System.out.println("Exception caused when closing the form" +ex);
        }
    }//GEN-LAST:event_formWindowClosing

    private void txt_entermsgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_entermsgActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_entermsgActionPerformed

    
    private void btn_JoinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_JoinActionPerformed
        try{
            //send a join request with the group name 
            String currentSelectedGroup = cbox_GroupList.getSelectedItem().toString();
            
            String msg = "JOIN" + SEPERATOR + currentSelectedGroup;
            if (!currentGrpList.contains(currentSelectedGroup)) {
                // add the selected item from the group and apend it
                currentGrpList.add(currentSelectedGroup); 
                EnableOrDisableJoinBtn(currentSelectedGroup);
            }
            
            WriteMessage(msg);
        }
        catch(Exception ex){
            System.out.println("Exception occurred in btn_Join: " +ex);
        }
    }//GEN-LAST:event_btn_JoinActionPerformed

    private void btn_CreateGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_CreateGroupActionPerformed
        try{
            String newGrpName = JOptionPane.showInputDialog(this, "Please enter a Unique Name..");
            //check if the newGrpName is not blank
            if (newGrpName == null) {
                return;
            }
            //Check to see if the new group name already exists in the combo list
            //setting to blank value
            cbox_GroupList.setSelectedIndex(-1);
            //setting to the new group name
            cbox_GroupList.setSelectedItem(newGrpName);
            //check to see of the index has changed if changed then we know it is present in the group list
            if (cbox_GroupList.getSelectedIndex() > -1) {
                JOptionPane.showMessageDialog(this, "Group Name: " + newGrpName + " already exists."
                        + "Please provide a unique group name");
            }
            else
            {
                newGrpName = NEWGROUPNAME + SEPERATOR + newGrpName;
                WriteMessage(newGrpName);
            }
        }
        catch(Exception ex){
            System.out.println("Exception occured in btn_CreateGroupActionPerformed: " +ex);
        }
    }//GEN-LAST:event_btn_CreateGroupActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CClient().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_CreateGroup;
    private javax.swing.JButton btn_Join;
    private javax.swing.JButton btn_Register;
    private javax.swing.JButton btn_Send;
    private javax.swing.JButton btn_login;
    private javax.swing.JComboBox cbox_GroupList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbl_CurrentScreenName;
    private javax.swing.JLabel lbl_GroupName;
    private javax.swing.JLabel lbl_Password;
    private javax.swing.JLabel lbl_ScreenName;
    private javax.swing.JLabel lbl_groups;
    private javax.swing.JLabel lbl_memberList;
    private javax.swing.JList lst_memberList;
    private javax.swing.JPanel pnl_ChatWindow;
    private javax.swing.JPanel pnl_GroupMembers;
    private javax.swing.JScrollPane scrpnl_memberList;
    private javax.swing.JTextField txt_ScreenName;
    private javax.swing.JTextField txt_entermsg;
    private javax.swing.JTextArea txt_msgArea;
    private javax.swing.JPasswordField txt_passwd;
    // End of variables declaration//GEN-END:variables
}
