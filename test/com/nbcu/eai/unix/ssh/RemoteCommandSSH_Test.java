package com.nbcu.eai.unix.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Anil Puvvada
 */
public class RemoteCommandSSH_Test {

    private static final Logger LOGGER = Logger.getLogger(RemoteCommandSSH_Test.class.getName());
    private final static int OFFSET = 4;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        /**
         * ********************************************
         * args[0] - Server args[1] - Username args[2] - Encrypted Password
         * args[3] - Command
         *********************************************
         */
        //String Input_Server = args[0];
        //String Input_Username = args[1];
        //String Input_Encypted_Password = args[2];
        //String Input_Command = args[3];
        
        //String Input_Server = "USHAPLD00241LC.tfayd.com";
        //String Input_Username = "tibroot";
        //String Input_Encypted_Password = "#!~MXQE=6]mpKh";
        
        //String Input_Server = "eclaplq00110lb.tfayd.com";
        String Input_Server = "eclaplq00110lb";
        String Input_Username = "tibroot";
        String Input_Encypted_Password = "#!AULQ{Mr]tV7\\tJ[^";
        //  Pipe separated multiple commands do not work. Use ; to make the code execute multiple commands in sequence.
        String Input_Command = "cd /tibco_installables/Inst/TIB6_BW_6.10.0; pwd; ls";

        StringBuilder outputBuffer = new StringBuilder();
        Session sesConnection = null;
        try {
            //sesConnection = (new JSch()).getSession(args[1], args[0], 22);
            sesConnection = (new JSch()).getSession(Input_Username, Input_Server, 22);
        } catch (JSchException ex) {
            //Logger.getLogger(RemoteCommandSSH.class.getName()).log(Level.SEVERE, null, ex);
            printErrorAndUsage("Error opening session to remote server -> " + Input_Server, ex);
            System.exit(-2);
        }
        //sesConnection.setPassword(decrypt(args[2]));
        sesConnection.setPassword(decrypt(Input_Encypted_Password));
        //sesConnection.setConfig("StrictHostKeyChecking", "no");

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        sesConnection.setConfig(config);
        boolean isConnected = false;
        int attemptCount = 0;
        do {
            attemptCount++;
            try {
                sesConnection.connect(5000);
                isConnected = true;
            } catch (JSchException ex) {
                //Logger.getLogger(RemoteCommandSSH_Test.class.getName()).log(Level.SEVERE, null, ex);
                printErrorAndUsage("Error connecting to remote server session -> " + Input_Server, ex);
                //System.exit(-2);
            }
        } while (!isConnected && attemptCount<3);
        if(!isConnected)
        {
            System.exit(-2);
        }

        Channel channel = null;
        try {
            channel = sesConnection.openChannel("exec");
        } catch (JSchException ex) {
            //Logger.getLogger(RemoteCommandSSH.class.getName()).log(Level.SEVERE, null, ex);
            sesConnection.disconnect();
            printErrorAndUsage("Error opening channel to remote server -> " + Input_Server, ex);
            System.exit(-3);
        }
        //((ChannelExec) channel).setCommand(args[3]);
        ((ChannelExec) channel).setCommand(Input_Command);

        try {
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();
            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = commandOutput.read();
            }
        } catch (IOException ex) {
            //Logger.getLogger(RemoteCommandSSH.class.getName()).log(Level.SEVERE, null, ex);
            sesConnection.disconnect();
            channel.disconnect();
            printErrorAndUsage("IO Error executing command -> " + Input_Server, ex);
            System.exit(-4);
        } catch (JSchException ex) {
            //Logger.getLogger(RemoteCommandSSH.class.getName()).log(Level.SEVERE, null, ex);
            sesConnection.disconnect();
            channel.disconnect();
            printErrorAndUsage("JSch Error executing command -> " + Input_Server, ex);
            System.exit(-5);
        }

        channel.disconnect();
        sesConnection.disconnect();

        System.out.println(outputBuffer.toString());
        //return outputBuffer.toString();
    }

    private static void printErrorAndUsage(String errMsg, Exception e) {
        System.err.println(errMsg);
        System.out.println("Usage: <<Command>> <<Server DNS Name or IP Address>> <<Username>> <<Encrypted Password>> <<Command to Execute>>");
        if (e != null) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, errMsg, e);
        } else {
            LOGGER.severe(errMsg);
        }
    }

    private static String decrypt(String secret) {
        if (!secret.startsWith("#!")) {
            return null;
        }
        String secret1 = secret.substring(2, secret.length());
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < secret1.length(); i++) {
            tmp.append((char) (secret1.charAt(i) - OFFSET));
        }
        return new String(Base64.getDecoder().decode(new StringBuffer(tmp.toString()).reverse().toString()));
    }
}
