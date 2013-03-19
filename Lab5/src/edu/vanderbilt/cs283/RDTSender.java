package edu.vanderbilt.cs283;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Properties;

public class RDTSender {
	
	private int MAXBUF_SIZE; 
	private int SENDER_TIMEOUT_MS;
	private long SEQ_DIFFERENCE;
    
    private static int COUNTER = 0;
    
	private DatagramSocket socket; 
	private InetAddress address;
	private int port;
	
	private int N = 0;
	private int base = 0;
	private int next = 0;
	private int totalLength = 0;
	private long timeStamp = 0;
	private byte[] buffer;
	
	public RDTSender(InetAddress serverIP, int serverPort) throws Exception {
		this.socket = new DatagramSocket();
		this.address = serverIP;
		this.port = serverPort;
		
		Properties prop = new Properties();
		prop.load(new FileInputStream("config.properties"));
		
		this.MAXBUF_SIZE = Integer.parseInt(prop.getProperty("MAXBUF_SIZE"));
		this.SENDER_TIMEOUT_MS = Integer.parseInt(prop.getProperty("SENDER_TIMEOUT_MS"));
		this.SEQ_DIFFERENCE = Long.parseLong(prop.getProperty("SEQ_DIFFERENCE"));
		
		this.timeStamp = new Date().getTime();
	}

	public boolean sendData(byte[] buffer, 
			int totalLength) throws Exception {
		/**
		 *  N: window size
		 */
		this.base = 0;
		this.next = 0;
		this.buffer = buffer;
		this.totalLength = totalLength;
		this.N = totalLength % MAXBUF_SIZE == 0? totalLength / MAXBUF_SIZE : 
					totalLength / MAXBUF_SIZE + 1;
		
		this.timeStamp += SEQ_DIFFERENCE * 4;
		
		System.out.println(" ---------- SNEDING NEW BLOCK: " + (COUNTER ++) + ", " + timeStamp + ", " + SEQ_DIFFERENCE);

		Thread sender = new Thread(new Sender());
		Thread receiver = new Thread(new Receiver());
		
		sender.start(); 
		receiver.start();
		
		sender.join(); 
		receiver.join();
		
		return true;
	}
	
	class Timer implements Runnable {
		private long original;
		private long timeline;
		
		Timer(long original, long timeStamp) {
			this.original = original;
			this.timeline = timeStamp;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(SENDER_TIMEOUT_MS);
				if (original == base && timeline == timeStamp) {
					System.out.println("<" + base + ">: Time Out");
					synchronized (RDTSender.class) {
						next = base;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class Receiver implements Runnable {
		@Override
		public void run() {
			byte[] unit = new byte[MAXBUF_SIZE];
			while (base != N) {
				DatagramPacket receivePacket = new DatagramPacket(unit, unit.length);
				try {
					socket.receive(receivePacket);
					byte[] content = receivePacket.getData();
					byte[] seq = new byte[4];
					byte[] time = new byte[8];
					
					for (int i = 0; i < 4; i++) seq[i] = content[i];
					for (int i = 4; i < 12; i++) time[i-4] = content[i];
					
					long recvTime = ByteBuffer.wrap(time).getLong();
					
					synchronized (RDTSender.class) {
						if (recvTime == timeStamp - SEQ_DIFFERENCE * 2) {
							base = N;
						} else if (recvTime == timeStamp - SEQ_DIFFERENCE) {
						    base = ByteBuffer.wrap(seq).getInt();
						    if (base < next) {
						    	new Thread(new Timer(base, timeStamp)).start();
						    }
						}
					}
					System.out.println("After Receiving ACK, Base: " + base + ", " + recvTime + ", " + timeStamp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	class Sender implements Runnable {
		@Override
		public void run() {
			/**
			 *  The first 4 bytes is the sequence number of the block
			 *  The next 4 bytes is tag indicating whether the block is the last of N 
			 *  The following 8 bytes is timestamp
			 *  is the last one.
			 */
			byte[] unit = new byte[MAXBUF_SIZE + 16];
			while (base != N) {
				synchronized (RDTSender.class) {
					next = next < base ? base : next;
				}
				
				if (next < N) {
					// The last block in the Window
					int flag = (next == N - 1) ? 1 : 0;
					
					byte[] byteNext = ByteBuffer.allocate(4).putInt(next).array();
					for (int i = 0; i < 4; i ++) unit[i] = byteNext[i];
					
					byte[] byteFlag = ByteBuffer.allocate(4).putInt(flag).array();
					for (int i = 4; i < 8; i ++) unit[i] = byteFlag[i - 4];
					
					byte[] timeTag = ByteBuffer.allocate(8).putLong(timeStamp).array();
					for (int i = 8; i < 16; i ++) unit[i] = timeTag[i - 8];
					
					for (int i = 16, j = next * MAXBUF_SIZE; 
								i < MAXBUF_SIZE + 16 && j < totalLength;)
						unit[i++] = buffer[j++];
					int length = (next + 1) * MAXBUF_SIZE > totalLength ? 
								totalLength % MAXBUF_SIZE : MAXBUF_SIZE;
					
					DatagramPacket sendPacket = 
							new DatagramPacket(unit, length + 16, address, port);
				    try {
						socket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				    
				    if (base == next) {
				    	new Thread(new Timer(base, timeStamp)).start();
				    }
				    
				    synchronized (RDTSender.class) {
				    	next += 1;
				    }
				    
					System.out.println("next: " + next + "; " + "base: " + base + ", " + length);
				}
			}
		}
	}
}
