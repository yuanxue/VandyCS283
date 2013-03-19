package edu.vanderbilt.cs283;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Properties;

public class RDTClient {

	private static void sendFileLength(RDTSender sender, long fileLength) {
		// CS283 Lab 5 Assignment. Please implement
		byte[] bytes = ByteBuffer.allocate(8).putLong(fileLength).array();
		
		try {
			sender.sendData(bytes, bytes.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream("config.properties"));
		
		String SENDING_FILENAME = prop.getProperty("SENDING_FILENAME");
		int MAXBUF_SIZE = Integer.parseInt(prop.getProperty("MAXBUF_SIZE"));
		int WINDOW_SIZE = Integer.parseInt(prop.getProperty("WINDOW_SIZE"));
		int SERVER_PORT = Integer.parseInt(prop.getProperty("SERVER_PORT"));
		InetAddress SERVER_IP = InetAddress.getByName(prop.getProperty("SERVER_IP"));
		
		RDTSender sender = new RDTSender(SERVER_IP, SERVER_PORT);		
		
		File sendFile = new File(SENDING_FILENAME);
		if (!sendFile.exists()) {
		    System.out.println(SENDING_FILENAME +  " missing");
			System.exit(1);
		}
		
		long fileLength = sendFile.length();
		System.out.println("Client, File Size: " + fileLength);
		
		sendFileLength(sender, sendFile.length());

		System.out.println("Sending File...");
		int totalSentBytes = 0;
		int readBytes;
		byte[] buf = new byte[MAXBUF_SIZE * WINDOW_SIZE];
		FileInputStream fileIn = new FileInputStream(sendFile);
		
		while(totalSentBytes < fileLength) {
			readBytes = fileIn.read(buf, 0, MAXBUF_SIZE * WINDOW_SIZE);
			sender.sendData(buf, readBytes);
			totalSentBytes += readBytes;	
	 	}
		
		fileIn.close();
	}
}
