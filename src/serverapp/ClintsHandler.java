package serverapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClintsHandler extends Thread {

    DataInputStream dis;
    DataOutputStream dos;
    Socket socket;
    String userName;
    static String received = "";
    static HashMap<String,ClintsHandler> clintsMap = new HashMap<>();
    String reciever;
    String sender;

    ClintsHandler(Socket s) {
        try {
            socket = s;
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        while (!socket.isClosed()) {

            try {
                 
                    received = dis.readUTF();

                    if (received != null) {
                        System.out.println("Received: " + received);
                        String [] parts = received.split(",");
                        switch (parts[0]) {

                            case "signup":

                                Users user = new Users();
                                user.setUserName(parts[1]);
                                user.setPassword(parts[2]);
                                user.setEmail(parts[3]);
                                user.setScore(0);
                                user.setStatus(false);

                                try {
                                    DAL.insert(user);
                                } catch (SQLException ex) {
                                    Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;

                            case "checkUserName":
                                if (DAL.isExist(parts[1])) {
                                    dos.writeUTF("exist");
                                    System.out.println(parts[1]);
                                } else {
                                    dos.writeUTF("notExist");
                                }
                                break;
                            case "signIn":
                                if (DAL.isPlayerExist(parts[1], parts[2])) {
                                    sender = parts[1];
                                    DAL.updateStatus(parts[1], true,true);
                                    clintsMap.put(parts[1], this);
                                    dos.writeUTF("true,"+sender);
                                } else {
                                    dos.writeUTF("false");
                                }
                                break;

                            case "signOut":
                                
                                DAL.updateStatus(parts[1], false,false);
                                System.out.println("Client disconnected");
                                clintsMap.remove(parts[1]);
                                break;

                            case "getUsersData":
                                
                                sendUsersData();

                                break;
                                
                            case "Invitation":
                                sender= parts[1];
                                reciever = parts[2];
                                System.out.println("reciever = "+reciever+","+sender);
                                sendInvitation(reciever,sender);

                             break;
                             case "Accepted":
                                 reciever = parts[1];
                                 sender = parts[2];
                                 clintsMap.get(sender).dos.writeUTF("Challenge accepted,"+sender+","+reciever);
                                 clintsMap.get(reciever).dos.writeUTF("Challenge accepted,"+sender+","+reciever);
                                 clintsMap.get(reciever).dos.writeUTF("disableButtons,");

                                break;

                               case"refused":
                                   reciever = parts[1];
                                    sender = parts[2];
                                    refusedInvitation("Challenge rejected");
                                    System.out.println("cha rejected"+clintsMap.size());
                                   break;
                               case "ExitGame":
                                   if (sender.equals(parts[1])){
                                       clintsMap.get(reciever).dos.writeUTF("gemeOver,"+sender);
                                   }else{
                                       clintsMap.get(sender).dos.writeUTF("gemeOver,"+reciever);
                                   }
                                   
                                   break;
                                   
                               case"updateAvailability":

                                    DAL.updateStatus(parts[1], true,false);
                                    DAL.updateStatus(parts[2], true,false);
                                   break;
                               case"returnAvailability":
                                   
                                   DAL.updateStatus(parts[1], true,true);
                                   DAL.updateStatus(parts[2], true,true);
                                   break;
                                   
                                case"move":
                                   
                                    if (parts[3].equals("X")){
                                       clintsMap.get(reciever).dos.writeUTF("responceMove,"+parts[1]+","+parts[2]+","+parts[3]+",enable");
                                       clintsMap.get(sender).dos.writeUTF("responceMove,"+parts[1]+","+parts[2]+","+parts[3]+",disabled");
                                    }else if (parts[3].equals("O")){
                                       clintsMap.get(sender).dos.writeUTF("responceMove,"+parts[1]+","+parts[2]+","+parts[3]+",enable");
                                       clintsMap.get(reciever).dos.writeUTF("responceMove,"+parts[1]+","+parts[2]+","+parts[3]+",disabled");
                                    }
                                    
                                     break;
                }              
                    
                    
                   if (parts[0].equals("signOut"))
                   {
                       break;
                   }
                }
            } catch (EOFException ef) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ef);
                break;
            } catch (IOException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (SQLException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }

    }
    
    private void sendUsersData()
    {
        DAL.userList.removeAllElements();
        try {
            DAL.getAllData();
        } catch (SQLException ex) {
            Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Users user : DAL.userList)
        {
            String availability ; 
            String status;
            System.out.println(user.userName);
            if (user.isAvailableity()){
                availability = "Available";
            }
            else{ availability = "UnAvailable"; }
            if (user.isStatus()){
                status = "Online";
            }else{
                status= "Offline";
            }
            String data = "userData,"+user.userName+","
                    +status+","+availability;
            try {
                dos.writeUTF(data);
            } catch (IOException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    protected void sendInvitation(String receiver,String sender)
    {

        try {
            clintsMap.get(receiver).dos.writeUTF("invitation recieved,"+receiver+","+sender);
        } catch (IOException ex) {
            Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
   
    }
    
    protected void refusedInvitation(String reply)
    {
        try {
            clintsMap.get(sender).dos.writeUTF(reply);
        } catch (IOException ex) {
            Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private void sendMessageToAll(String receved) {
        for (ClintsHandler clint: clintsMap.values()) {
            try {
               clint.dos.writeUTF(receved);
                System.out.println("sending to client : "+ receved);
            } catch (IOException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
