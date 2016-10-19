
/*
	PeerInfo is meant for a peer to store key attributes about a (seperate, single) connected peer. Attributes herein are what is "known" not necessarily what is true.  
*/
class PeerInfo{
	private byte[] _bitfield;
	public boolean _isOUneighbour; //optomistically unchoked
	public boolean _choked;
	public boolean _interested;
	

	public int _peerID;
	public int _speed;

	public PeerInfo(){
		_bitfield = new byte[];
	}

	public void setPeerID(int newID){
		_peerID = newID;
	}
	public int getPeerID(){
		return _peerID;
	}

	public byte[] getBitfield(){
		return _bitField;
	}



}