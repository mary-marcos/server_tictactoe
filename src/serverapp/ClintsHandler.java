package serverapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClintsHandler extends Thread {

    DataInputStream dis;
    DataOutputStream dos;
    Socket socket;
    String userName;
    static String received = "";
    static Vector<ClintsHandler> clintsVector = new Vector<>();

    ClintsHandler(Socket s) {
        try {
            socket = s;
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            clintsVector.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        while (true) {

            try {
                 if (received!=null)
                 {
                 received = dis.readUTF();
                 }
                if (received != null) {
                    System.out.println("Received: " + received);
                    String [] parts = received.split(",");
                    switch (parts[0]) {
                        case "move":
                            sendMessageToAll(received);
                            
                        break;

                        case "signup":

                            UserTable user = new UserTable();
                            user.setUserName(parts[1]);
                            user.setPassword(parts[2]);
                            user.setEmail(parts[3]);
                            user.setScore(0);
                            user.setStatus(false);

                            try {
                                DAL_1.insert(user);
                            } catch (SQLException ex) {
                                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;

                        case "checkUserName":
                            if (DAL_1.isExist(parts[1])) {
                                dos.writeUTF("exist");
                                System.out.println(parts[1]);
                            } else {
                                dos.writeUTF("notExist");
                            }
                            break;
                        case "signIn":
                            if (DAL_1.isPlayerExist(parts[1], parts[2])) {
                                DAL_1.updateStatus(parts[1], true);
                                dos.writeUTF("true,yes");
                                System.out.println("true");
                            } else {
                                dos.writeUTF("false");
                            }
                            break;
                        
                        case "signOut":
                            if (parts.length > 1)
                            {
                            DAL_1.updateStatus(parts[1], false);
                            }
                            System.out.println("Client disconnected");
                            clintsVector.remove(this);
                            break;
                    }
                    
                   if (parts[0].equals("signOut"))
                   {
                       break;
                   }
                }
            } catch (EOFException ef) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ef);
            } catch (IOException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    
    private void sendMessageToAll(String receved) {
        for (ClintsHandler ch : clintsVector) {
            try {
                ch.dos.writeUTF(receved);
                
                System.out.println("sending to client : "+ receved);
            } catch (IOException ex) {
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }}

}
