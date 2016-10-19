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


	public peerProcess(int peerID){
		_peerID = peerID;
		// 1000 is a placeholder, will need to take from cfg file
		_bitfield = new byte[1000];
	}

	public peerProcess() {
		_bitfield = new byte[1000];
	}

	public void initialize(int peerID) {
		_peerID = peerID;
		parseCommonConfig("Common.cfg");



		System.out.println("At bottom of initialize(): _peerID = " + _peerID);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _numPreferredNeighbors);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _unchokingInterval);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _ouInterval);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _fileName);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _fileSize);
		System.out.println("At bottom of initialize(): _numPreferredNeighbors = " + _pieceSize);
	}

	// don't think we should be able to setPeerID after it is constructed
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
		    while ((line = br.readLine()) != null) {
		    	String [] splitLine = line.split("\\s+");
				// System.out.println(line);
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
		    	}
	    	}
	    }
	    catch (IOException e) {
	    	System.out.println(e);
	    	System.out.println("Invalid or not found Common.cfg");
	    }
	}

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
		
		peerProcess p = new peerProcess();

		p.initialize(peerID);


	}

}