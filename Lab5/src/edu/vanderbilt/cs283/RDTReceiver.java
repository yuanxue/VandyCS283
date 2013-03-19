package edu.vanderbilt.cs283;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;

public class RDTReceiver {
	
	private int MAXBUF_SIZE; 
	private int WINDOW_SIZE;
	private int SEQ_DIFFERENCE;
	
	private int total = 0;
	private int[] RECEIVED;
	private DatagramSocket socket;
	
	private long timeStamp = -3;
	
	public RDTReceiver(InetAddress serverIP, int serverPort) throws Exception {
		// CS283 Lab 5 Assignment. Please implement
		
		Properties prop = new Properties();
		prop.load(new FileInputStream("config.properties"));
	
		MAXBUF_SIZE = Integer.parseInt(prop.getProperty("MAXBUF_SIZE"));
		WINDOW_SIZE = Integer.parseInt(prop.getProperty("WINDOW_SIZE"));
		SEQ_DIFFERENCE = Integer.parseInt(prop.getProperty("SEQ_DIFFERENCE"));
		
		RECEIVED = new int[WINDOW_SIZE];
		for (int i = 0; i < RECEIVED.length; i++) {
			RECEIVED[i] = 0;
		}
		
		socket = new DatagramSocket(serverPort);
	}

	public int recvData(byte[] buffer) {
		byte[] unit = new byte[MAXBUF_SIZE + 16];
		DatagramPacket receivePacket = new DatagramPacket(unit, unit.length);
		
		total = 0;
		while (!isFull()) {
			try {
				socket.receive(receivePacket);
				unit = receivePacket.getData();
				
				byte[] byteNext = new byte[4];
				byte[] byteFlag = new byte[4];
				byte[] byteTime = new byte[8];
				for (int i = 0; i < 4; i ++) byteNext[i] = unit[i];
				for (int i = 4; i < 8; i ++) byteFlag[i-4] = unit[i];
				for (int i = 8; i < 16; i ++) byteTime[i-8] = unit[i];
				
				int next = ByteBuffer.wrap(byteNext).getInt();
				int flag = ByteBuffer.wrap(byteFlag).getInt();
				long time = ByteBuffer.wrap(byteTime).getLong();
				int length = receivePacket.getLength();

				if (timeStamp == time - SEQ_DIFFERENCE * 2) {
					byte[] ack = getACKMessage();
					DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, 
							receivePacket.getAddress(), receivePacket.getPort());
					socket.send(sendPacket);
					continue;
				}
				
				if (total == 0) {
					timeStamp = time - SEQ_DIFFERENCE;
				} 
				
				if (RECEIVED[next] == 0) {
					for (int i = 0; i < length - 16; i++)
						buffer[next * MAXBUF_SIZE + i] = unit[i + 16];
					total += length - 16;
					
					if (flag == 1) 
						RECEIVED[next] = 2;
					else 
						RECEIVED[next] = 1;
				}
				
				/**
				 *  Just send 1 ack for every 8 package
				 *  Imitate Data loss .10
				 */
				if ((new Random().nextInt(10)) != 7) {
					byte[] ack = getACKMessage();
					DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, 
							receivePacket.getAddress(), receivePacket.getPort());
					socket.send(sendPacket);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		clear();
		timeStamp = timeStamp - SEQ_DIFFERENCE;
		
		return total;
	}
	
	private byte[] getACKMessage() {
		byte[] ack = ByteBuffer.allocate(4).putInt(getAck()).array();
		byte[] seq = ByteBuffer.allocate(8).putLong(timeStamp).array();
		
		byte[] message = new byte[12];
		for (int i = 0; i < 4; i++) message[i] = ack[i];
		for (int i = 4; i < 12; i++) message[i] = seq[i - 4];
		
		return message;
	}
	private int getAck() {
		int result = 0;
		for (int i = 0; i < RECEIVED.length; i++) {
			if (RECEIVED[i] == 0) {
				result = i;
				break;
			}
			if (RECEIVED[i] == 2) {
				result = i + 1;
				break;
			}
		}
		
		return result;
	}
	
	private boolean isFull() {
		boolean result = false;
		for (int i = 0; i < RECEIVED.length; i++) {
			if (RECEIVED[i] == 0) 
				break;
			if (RECEIVED[i] == 2) {
				result = true;
				break;
			}
		}

		return result;
	}
	
	private void clear() {
		for (int i = 0; i < RECEIVED.length; i++) {
			RECEIVED[i] = 0;
		}
	}
}
