package org.frustra.jp33r;
import java.net.DatagramPacket;
import java.net.InetAddress;


public class PeerPacket {
	private byte[] data = null;
	private int length = 0;
	private byte type;
	private byte flags = 0;
	private byte datalength = 0;
	private InetAddress address = null;
	private int port = 0;
	
	public PeerPacket(byte packetType, byte length, byte flags, byte[] pdata) {
		this.datalength = length;
		this.length = length+4;
		
		this.type = packetType;
		this.datalength = length;
		this.flags = flags;
		
		data = pdata;
	}
	
	public PeerPacket(DatagramPacket pp) {
		this(pp.getData(), pp.getLength(), pp.getAddress(), pp.getPort());
	}
	
	public PeerPacket(byte[] pdata) {
		this(pdata, pdata.length, null, 0);
	}
	
	public PeerPacket(byte[] pdata, int length) {
		this(pdata, length, null, 0);
	}
	
	private PeerPacket(byte[] pdata, int length, InetAddress address, int port) {
		this.length = length;
		this.address = address;
		this.port = port;
		
		type = pdata[0];
		datalength = pdata[1];
		flags = pdata[3];
		
		data = new byte[length-4];
		System.arraycopy(pdata, 4, data, 0, length-4);

		for (int i = PeerSocket.crypts.size() - 1; i >= 0; i--) {
			data = PeerSocket.crypts.get(i).decrypt(data);
		}
	}
	
	public PeerPacket() {
		
	}

	public byte[] getData() {
		return data;
	}
	
	public byte[] getSendData() {
		byte[] ret = new byte[length];
		ret[0] = type;
		ret[1] = datalength;
		ret[2] = flags;
		System.arraycopy(data, 0, ret, 4, datalength);
		return ret;
	}
	
	public byte getFlags() {
		return flags;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public byte getLength() {
		return datalength;
	}

	public int getFullLength() {
		return length;
	}
	
	public byte getType() {
		return type;
	}
}
