import java.util.*;
import java.io.*;
/*
	peerProcess 
*/

class peerProcess{
	// don't think it needs this at the moment, needs to only know info about its neighbors
	// private PeerInfo _myPeerInfo;
	ArrayList<NeighborInfo> _neighborInfos;
	private byte[] _bitfield;
	private int _peerID;
	private int _numPreferredNeighbors;
	private int _unchokingInterval;
	private int _ouInterval;
	private String _fileName;
	private int _fileSize;
	private int _pieceSize;
	private int _numPieces;


	public peerProcess(int peerID){
		_peerID = peerID;
		_neighborInfos = new ArrayList<NeighborInfo>();
	}

	public peerProcess() {
		_neighborInfos = new ArrayList<NeighborInfo>();
	}

	public void initialize(int peerID) {
		_peerID = peerID;

		// all in one method for parsing the common config file
		parseCommonConfig("Common.cfg");

		// kept out of the function kind of unnecessarily, but fn would still work
		// if out of order config files were expected
		_bitfield = new byte[_fileSize];
		_numPieces = _fileSize / _pieceSize;

		parsePeerConfig("peer_" + _peerID + "/PeerInfo.cfg");

		System.out.println("At bottom of initialize(): _peerID = " + _peerID);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _numPreferredNeighbors);
		System.out.println("At bottom of initialize(): _unchokingInterval = " + _unchokingInterval);
		System.out.println("At bottom of initialize(): _ouInterval = " + _ouInterval);
		System.out.println("At bottom of initialize(): _fileName = " + _fileName);
		System.out.println("At bottom of initialize(): _fileSize = " + _fileSize);
		System.out.println("At bottom of initialize(): _pieceSize = " + _pieceSize);
		System.out.println("At bottom of initialize(): _neighborInfos.size() = " + _neighborInfos.size());
	}

	// don't think we should be able to setPeerID after it is constructed -> commented out for now
	// public void setPeerID(int newID){
	// 	_peerID = newID;
	// }

	public int getPeerID(){
		return _peerID;
	}

	public byte[] getBitfield(){
		return _bitfield;
	}

	private void parseCommonConfig(String configFileName) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFileName));
		    String line;
		    // if theres still a line left keep going
		    while ((line = br.readLine()) != null) {
		    	//split line at every blank space
		    	String [] splitLine = line.split("\\s+");
				//decide what to do on what the particular split part is 
	    	    switch (splitLine[0]) {

			        case "NumberOfPreferredNeighbors":
			            _numPreferredNeighbors = Integer.parseInt(splitLine[1]);
			            break;

			        case "UnchokingInterval":
			            _unchokingInterval = Integer.parseInt(splitLine[1]);
			            break;

			        case "OptimisticUnchokingInterval":
			            _ouInterval = Integer.parseInt(splitLine[1]);
			            break;

			        case "FileName":
			            _fileName = splitLine[1];
			            break;

			        case "FileSize":
			            _fileSize = Integer.parseInt(splitLine[1]);
			            break;

			        case "PieceSize":
			            _pieceSize = Integer.parseInt(splitLine[1]);
			            break;

			        default:
			            throw new IllegalArgumentException("Invalid Common.cfg field: " + splitLine[0]);

		    	} // end switch
	    	} // end while
	    } // end try
	    catch (IOException e) {
	    	System.out.println(e);
	    	System.out.println("Invalid or not found Common.cfg");
	    } // end catch
	} // end parseCommonConfig

	private void parsePeerConfig(String peerInfoFileName) {

		int neighborID;
		String neighborHostName;
		int neighborPortNum;
		boolean neighborFullFile;
		try {

			BufferedReader br = new BufferedReader(new FileReader(peerInfoFileName));
		    String line;
		    while ((line = br.readLine()) != null) {

		    	String [] splitLine = line.split("\\s+");
				
		    	// get variables ready to create a NeighborInfo - kind of unnecessary
				neighborID = Integer.parseInt(splitLine[0]);
				neighborHostName = splitLine[1];
				neighborPortNum = Integer.parseInt(splitLine[2]);
				neighborFullFile = ( Integer.parseInt(splitLine[3]) != 0 ); // if its 0 -> false  else -> true
				
				NeighborInfo ni = new NeighborInfo(neighborID, neighborHostName, 
										neighborPortNum, _fileSize, neighborFullFile);
	    	    _neighborInfos.add(ni);
	    	} // end while

	    } // end try
	    catch (IOException e) {
	    	System.out.println(e);
	    	System.out.println("Invalid or not found PeerInfo.cfg");
	    } // end catch 
	} // end function

	public static void main(String[] args) {

		int peerID;
		try {
			peerID = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException e) {
			System.out.println(args[0] + " is an invalid peerID");
			System.out.println("You must supply the peerID when starting the program. Terminating.");
			return;
		}
		
		// make new peer process
		peerProcess p = new peerProcess();
		// feed it its peerID, and it will know the rest from there
		p.initialize(peerID);
	}
}