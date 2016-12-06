import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/*
	NeighborInfo is meant for a peer to store key attributes about a (seperate, single) connected peer. Attributes herein are what is "known" not necessarily what is true.  
*/

class NeighborInfo implements Comparable<NeighborInfo> {
	public int _peerID;
	public String _hostName;
	public int _portNum;
	public boolean _fullFile;

	public int _speed;
	// public byte[] _bitfield;
	public BitField _bitfield;

	public Socket socket;
	public DataInputStream inStream;
	public DataOutputStream outStream;

	public boolean _isOUneighbour; 	// optimistically unchoked
	public boolean _isChoked;
	public boolean _handshakeSent;
	public boolean _amIInterested;
	public boolean _isInterested;

	



	public NeighborInfo(int peerID, String hostName, 
						int portNum, int fileSize, boolean fullFile) {

		_peerID = peerID;
		_hostName = hostName;
		_portNum = portNum;
		// _bitfield = new byte[fileSize];
		_bitfield = new BitField(fileSize);
		_fullFile = fullFile;

		System.out.println("\t----new NeighborInfo created.\n\t\t_peerID = " + _peerID + "\n\t\t_hostName = " + hostName
							+ "\n\t\t_portNum = " + _portNum + "\n\t\t_bitfield size = " + fileSize + "\n\t\t_fullFile = " + _fullFile
							+ "\n\t----end of new NeighborInfo creation.");

	}

	public NeighborInfo() {

	}

	public void setPeerID(int newID) {
		_peerID = newID;
	}

	public int getPeerID() {
		return _peerID;
	}

	public int getPortNum() {
		return _portNum;
	}
	
	public String getHostName() {
		return _hostName;
	}

	// public byte[] getBitfield() {
	public BitField getBitfield() {
		return _bitfield;
	}
	
	public int compareTo(NeighborInfo other) {
		return this._peerID - other._peerID;
	}
}
