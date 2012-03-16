package org.frustra.jp33r.example;


import java.io.IOException;
import java.util.Scanner;

import org.frustra.jp33r.PacketEncryption;
import org.frustra.jp33r.PacketListener;
import org.frustra.jp33r.PeerPacket;
import org.frustra.jp33r.PeerSocket;

public class Example {

	public static final byte PACKET_MSG = 0x04;

	public static void main(String[] args) throws IOException {
		System.out.println("P2P Test Chat");
		
		/*
		 * Change the IP to your peers IP
		 * If you are running this locally twice, make sure the two ports are different, or else two sockets will be bound to the same port
		 */
		PeerSocket sender = new PeerSocket(4444);
		System.out.println("Socket created, connecting...");
		if (sender.connect("127.0.0.1", 4444, 5000)) {
			System.out.println("Connected Tunnel");
			
			sender.addPacketListener(new PacketListener() {
				public void receive(PeerPacket p) {
					if (p == null) {
						System.out.println("Disconnected");
						System.exit(0);
						return;
					}
					byte[] data = p.getData();
					//String data2 = new String(p.getData(), 1, ((int) data[0]) & 0x000000FF);
					System.out.println(new String(data));
				}
			});
			sender.addPacketEncryption(new PacketEncryption() {
				public byte[] encrypt(byte[] data) {
					for (int i = 0; i < data.length; i++) {
						data[i] ^= 5;
					}
					return data;
				}
	
				public byte[] decrypt(byte[] data) {
					for (int i = 0; i < data.length; i++) {
						data[i] ^= 5;
					}
					return data;
				}
			});
	
			Scanner scan = new Scanner(System.in);
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				byte[] data = line.getBytes();
				sender.send(PACKET_MSG, (byte) 0x00, data);
			}
			sender.close();
		} else {
			System.out.println("Connection timed out");
		}
	}

}
