package com.nbcu.eai.unix.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Anil Puvvada
 */
public class RemoteCommandSSH {

    private static final Logger LOGGER = Logger.getLogger(RemoteCommandSSH.class.getName());
    private final static int OFFSET = 4;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        /**
         * ********************************************
         * args[0] - Server
         * args[1] - Username
         * args[2] - Encrypted Password
         * args[3] - Command
         *********************************************/
        
        if (args.length != 4) {
            printErrorAndUsage("Unexpected number of Arguments. Required 4, got only + " + args.length + " -> " + Arrays.toString(args), null);
            System.exit(-1);
        }
        
        StringBuilder outputBuffer = new StringBuilder();
        Session sesConnection = null;
        try {
            sesConnection = (new JSch()).getSession(args[1], args[0], 22);
        } catch (JSchException ex) {
            printErrorAndUsage("Error opening session to remote server -> " + args[0], ex);
            System.exit(-2);
        }
        sesConnection.setPassword(decrypt(args[2]));
        Properties config = new Properties();
        {
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
        }
        sesConnection.setConfig(config);
        try {
            sesConnection.connect(10000);
        } catch (JSchException ex) {
            printErrorAndUsage("Error connecting to remote server session -> " + args[0], ex);
            System.exit(-3);
        }
        Channel channel = null;
        try {
            channel = sesConnection.openChannel("exec");
        } catch (JSchException ex) {
            sesConnection.disconnect();
            printErrorAndUsage("Error opening channel to remote server -> " + args[0], ex);
            System.exit(-4);
        }
        ((ChannelExec) channel).setCommand(args[3]);
        
        try{
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();
            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = commandOutput.read();
            }
        } catch (IOException ex) {
            sesConnection.disconnect();
            channel.disconnect();
            printErrorAndUsage("IO Error executing command -> " + args[0], ex);
            System.exit(-5);
        } catch (JSchException ex) {
            sesConnection.disconnect();
            channel.disconnect();
            printErrorAndUsage("JSch Error executing command -> " + args[0], ex);
            System.exit(-6);
        }

        channel.disconnect();
        sesConnection.disconnect();

        System.out.println(outputBuffer.toString());
    }

    private static void printErrorAndUsage(String errMsg, Exception e) {
        System.err.println(errMsg);
        System.out.println("Usage: <<Command>> <<Server DNS Name or IP Address>> <<Username>> <<Encrypted Password>> <<Command to Execute>>");
        if (e != null) {
            System.err.println(e.getMessage());
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
