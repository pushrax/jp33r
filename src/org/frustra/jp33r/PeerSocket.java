package org.frustra.jp33r;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class PeerSocket {

	private String ip;
	private int port;
	private int lport;
	private boolean connecting;
	private boolean running;
	private boolean server;
	private DatagramSocket socket;
	private long lastPacket = 0, timeout = 10000;
	private ArrayList<PacketListener> listeners = new ArrayList<PacketListener>();
	protected static ArrayList<PacketEncryption> crypts = new ArrayList<PacketEncryption>();

	public PeerSocket() throws SocketException {
		this("", -1, -1, 10000);
	}
	
	public PeerSocket(int port) throws SocketException {
		this("", -1, port, 10000);
	}
	
	public PeerSocket(int port, long timeout) throws SocketException {
		this("", -1, port, timeout);
	}
	
	public PeerSocket(String rip, int rport) throws SocketException {
		this(rip, rport, -1, 10000);
	}
	
	public PeerSocket(String rip, int rport, long timeout) throws SocketException {
		this(rip, rport, -1, timeout);
	}
	
	public PeerSocket(String rip, int rport, int port) throws SocketException {
		this(rip, rport, port, 10000);
	}
	
	public PeerSocket(String rip, int rport, int port, long timeout) throws SocketException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		this.ip = rip;
		this.port = rport;
		if (port >= 0) {
			this.lport = port;
		} else {
			this.socket = new DatagramSocket();
			this.lport = this.socket.getLocalPort();
			this.socket.close();
		}
		this.connecting = false;
		this.server = false;
		this.timeout = timeout;
	}
	
	public boolean connect() throws IOException {
		if (this.ip.length() <= 0 || this.port < 0) throw new UnknownHostException("Hostname or port undefined");
		return connect(this.ip, this.port);
	}
	
	public boolean connect(String ip, int port) throws IOException {
		return connect(ip, port, -1);
	}
	
	public boolean connect(String ip, int port, final long connectTimeout) throws IOException {
		this.ip = ip;
		this.port = port;
		
		connecting = true;
		running = true;

		this.socket = new DatagramSocket(lport);
		
		new Thread() {
			{ this.setDaemon(true); }
			
			public void run() {
				long timer = System.currentTimeMillis();
				while (connecting && running) {
					try {
						send(PacketType.CONN, (byte) 0x00);
					} catch (IOException e) {
						e.printStackTrace();
						connecting = false;
						running = false;
						return;
					}
					if (connectTimeout >= 0 && System.currentTimeMillis() - timer > connectTimeout) {
						try {
							close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						connecting = false;
						running = false;
						return;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						connecting = false;
						running = false;
						return;
					}
				}
			}
		}.start();
		
		long start = System.currentTimeMillis();
		final byte[] time = new byte[] {(byte) ((start >> 56) & 0x00FF), (byte) ((start >> 48) & 0x00FF), (byte) ((start >> 40) & 0x00FF), (byte) ((start >> 32) & 0x00FF), (byte) ((start >> 24) & 0x00FF), (byte) ((start >> 16) & 0x00FF), (byte) ((start >> 8) & 0x00FF), (byte) (start & 0x00FF)};
		
		while (running) {
			try {
				PeerPacket p = receive(8);
				
				//String data = new String(p.getData()).trim();
				if (p.getType() == PacketType.CONN) {
					send(PacketType.CONN2, (byte) 0x00, time);
				} else if (p.getType() == PacketType.CONN2) {
					byte[] other = p.getData();
					for (int i = 0; i < 8; i++) {
						if ((other[i] & 0x00FF) != (time[i] & 0x00FF)) {
							if ((other[i] & 0x00FF) < (time[i] & 0x00FF)) server = true;
							break;
						}
					}
					send(PacketType.CONN2, (byte) 0x00, time);
					connecting = false;
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					connecting = false;
					running = false;
					return false;
				}
			} catch (IOException e) {
				if (!(e instanceof SocketException) || !e.getMessage().equalsIgnoreCase("socket closed")) e.printStackTrace();
				connecting = false;
				running = false;
				return false;
			}
		}
		lastPacket = System.currentTimeMillis();
		new Thread() {
			{ this.setDaemon(true); }
			
			public void run() {
				try {
					while (running) {
						try {
							send(PacketType.PING, (byte) 0x00);
							
							if (System.currentTimeMillis() - lastPacket > timeout) {
								System.out.println("Socket timed out");
								for (PacketListener listen : listeners) {
									listen.receive(null);
								}
								return;
							}
						} catch (IOException e) {
							if (!(e instanceof SocketException) || !e.getMessage().equalsIgnoreCase("socket closed")) e.printStackTrace();
							running = false;
							return;
						}
						try {
							Thread.sleep(timeout / 10);
						} catch (InterruptedException e) {
							running = false;
							return;
						}
					}
				} finally {
					try {
						close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		new Thread() {
			{ this.setDaemon(true); }
			
			public void run() {
				try {
					while (running) {
						try {
							PeerPacket p = receive();
							lastPacket = System.currentTimeMillis();
	
							//byte[] data = p.getData();
							//String data2 = new String(p.getData());
							if (p.getType() == PacketType.EXIT) {
								for (PacketListener listen : listeners) {
									listen.receive(null);
								}
								return;
							} else if (p.getType() != PacketType.CONN && p.getType() != PacketType.CONN2 && p.getType() != PacketType.PING) {
								for (PacketListener listen : listeners) {
									listen.receive(p);
								}
							}
						} catch (IOException e) {
							if (!(e instanceof SocketException) || !e.getMessage().equalsIgnoreCase("socket closed")) e.printStackTrace();
							running = false;
							return;
						}
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							running = false;
							return;
						}
					}
				} finally {
					try {
						close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		if (connecting || !running) {
			running = false;
			connecting = false;
			return false;
		} else return true;
	}
	
	public int getPort() {
		return lport;
	}
	
	public boolean isServerSocket() {
		return server;
	}
	
	public void addPacketListener(PacketListener listener) {
		listeners.add(listener);
	}
	
	public void removePacketListener(PacketListener listener) {
		listeners.remove(listener);
	}
	
	public void addPacketEncryption(PacketEncryption crypt) {
		crypts.add(crypt);
	}
	
	public void removePacketEncryption(PacketEncryption crypt) {
		crypts.remove(crypt);
	}
	
	public PeerPacket receive() throws IOException {
		return receive(256);
	}
	
	public PeerPacket receive(int length) throws IOException {
		DatagramPacket pp = new DatagramPacket(new byte[length+4], length+4);
		socket.receive(pp);
		return new PeerPacket(pp);
	}
	
	public void send(byte packetType, byte flags) throws IOException {
		this.send(packetType, (byte) 0x00, flags, new byte[0]);
	}
	
	public void send(byte packetType, byte flags, byte[] pdata) throws IOException {
		this.send(packetType, pdata.length, flags, pdata);
	}
	
	public void send(byte packetType, int length, byte flags, byte[] pdata) throws IOException {
		if (pdata != null) {
			for (int i = 0; i < crypts.size(); i++) {
				pdata = crypts.get(i).encrypt(pdata);
			}
		}
		if (length > 256) throw new IOException("Packet data too long (" + length + ")");
		byte[] send = new byte[length + 4];
		send[0] = packetType;
		send[1] = (byte) (length & 0x00FF);
		send[2] = flags;
		send[3] = (byte) 0x00;
		if (pdata != null)
			System.arraycopy(pdata, 0, send, 4, length);
		socket.send(new DatagramPacket(send, length + 4, new InetSocketAddress(ip, port)));
	}
	
	public void close() throws IOException {
		running = false;
		if (socket != null && !socket.isClosed() && ip.length() > 0 && port >= 0) {
			send(PacketType.EXIT, (byte) 0x00);
			socket.close();
		}
	}
	
}
