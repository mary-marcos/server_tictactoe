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
                String received;
                 received = dis.readUTF();
                if (received != null) {
                    System.out.println("Received: " + received);
                    String[] parts = received.split(",");
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
                        case "signin":
                            if (DAL_1.isPlayerExist(parts[1], parts[2])) {
                                DAL_1.updateStatus(parts[1], true);
                                dos.writeUTF("true");
                            } else {
                                dos.writeUTF("false");
                            }
                            break;
                        
                        case "signOut":

                            DAL_1.updateStatus(parts[1], false);
                            
                            break;
                    }

                }
            } catch (EOFException ef) {
                System.out.println("Client disconnected.");
                clintsVector.remove(this);
                break;
            } catch (IOException ex) {
                System.out.println("Client disconnected.");
                clintsVector.remove(this);
                Logger.getLogger(ClintsHandler.class.getName()).log(Level.SEVERE, null, ex);
                break;
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
