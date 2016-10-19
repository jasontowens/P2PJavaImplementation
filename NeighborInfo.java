
/*
	NeighborInfo is meant for a peer to store key attributes about a (seperate, single) connected peer. Attributes herein are what is "known" not necessarily what is true.  
*/

class NeighborInfo{
	private byte[] _bitfield;
	public boolean _isOUneighbour; 	// optimistically unchoked
	public boolean _choked;
	public boolean _interested;
	

	public int _peerID;
	public int _speed;

	public NeighborInfo(){
		// 1000 is a placeholder, will need to take from cfg file
		_bitfield = new byte[1000];
	}

	public void setPeerID(int newID){
		_peerID = newID;
	}
	public int getPeerID(){
		return _peerID;
	}

	public byte[] getBitfield(){
		return _bitfield;
	}



}