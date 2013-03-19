package edu.vanderbilt.cs283;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class RDTServer {

	private static long getFileLength(RDTReceiver recver) {
		byte[] buf = new byte[8];
		recver.recvData(buf);
		
		return ByteBuffer.wrap(buf).getLong();
	}
	
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream("config.properties"));
		
		String REVEIVING_FILENAME = prop.getProperty("REVEIVING_FILENAME");
		int MAXBUF_SIZE = Integer.parseInt(prop.getProperty("MAXBUF_SIZE"));
		int WINDOW_SIZE = Integer.parseInt(prop.getProperty("WINDOW_SIZE"));
		int SERVER_PORT = Integer.parseInt(prop.getProperty("SERVER_PORT"));	
		InetAddress SERVER_IP = InetAddress.getByName(prop.getProperty("SERVER_IP"));
		
		if (SERVER_IP == null){
			System.err.println("No matching IP");
			System.exit(1);
		}
		
		RDTReceiver recver = new RDTReceiver(SERVER_IP, SERVER_PORT);
		
		File recvFile = new File(REVEIVING_FILENAME);
		if (!recvFile.exists()) {
			recvFile.createNewFile();
	        System.out.println(REVEIVING_FILENAME + " is created!");
		}
	      	      
		long fileLength = getFileLength(recver);

		int totalRecvBytes = 0;
		int recvBytes = 0;
		byte [] buf = new byte[MAXBUF_SIZE * WINDOW_SIZE];
		FileOutputStream fileOut = new FileOutputStream(recvFile);
		
		while(totalRecvBytes < fileLength) {
			recvBytes = recver.recvData(buf);
			totalRecvBytes += recvBytes;
			fileOut.write(buf, 0, recvBytes);
			System.out.println("Total Size: " + totalRecvBytes);
 	    }
		
		fileOut.close();
	}
}
