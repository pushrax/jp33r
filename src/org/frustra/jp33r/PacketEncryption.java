package org.frustra.jp33r;

public interface PacketEncryption {
	public byte[] encrypt(byte[] data);
	public byte[] decrypt(byte[] data);
}
