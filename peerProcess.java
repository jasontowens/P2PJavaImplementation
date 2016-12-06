import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class peerProcess implements Runnable{
	// don't think it needs this at the moment, needs to only know info about its neighbors
	// private PeerInfo _myPeerInfo;
	ArrayList<NeighborInfo> _neighborInfos;
	private Server _server; // todo brian replace with proper server type
	private Client _client; // todo brian replace with proper client type
	// private byte[] _bitfield;
	private BitField _bitfield;
	private int _peerID;
	private int _portNum;
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
			
				// does not incorporate error checking right now
				// meaning that it will add itself into a _neighborInfos
				// I know this logically doesn't make sense, but it makes it easier to gather
				// info about this peer that can not be gathered from Common.cfg 
				// note: later we may have to make sure that when we are looking for eligible
				// 			neighbors that we do not choose ourself
				NeighborInfo ni = new NeighborInfo(neighborID, neighborHostName, 
										neighborPortNum, _fileSize, neighborFullFile);
   				_neighborInfos.add(ni);		
					
					if (ni._peerID == _peerID) {
						_portNum = neighborPortNum;
					}		

   				// this version will only add the neighbor to _neighborInfos
   				// if it is not THIS peer (aka peerID doesnt match neighborID)
				// if(_peerID != neighborID) {
				// 	NeighborInfo ni = new NeighborInfo(neighborID, neighborHostName, 
				// 							neighborPortNum, _fileSize, neighborFullFile);
	   			//	_neighborInfos.add(ni);
	   //  	    } // end if
	    	} // end while

	    } // end try
	    catch (IOException e) {
	    	System.out.println(e);
	    	System.out.println("Invalid or not found PeerInfo.cfg");
	    } // end catch 
	} // end function
	
	public void setupConnections() {
		//get the peerMap and sort it by peerID
		List<NeighborInfo> sortedNeighbors = _neighborInfos;
		Collections.sort(sortedNeighbors);
		sortedNeighbors.remove(Integer.valueOf(this._peerID)); //ensure my peer info isn't in the list

		for (NeighborInfo neighbor: sortedNeighbors) 
		{
			//if we appear first we are a server
			if(_peerID < neighbor._peerID) {
				this.setupServer();
			} 			
			//if we appear second we are a client
			else if(_peerID > neighbor._peerID) {
				this.setupClient();
			}
		}
	}

	public void setupServer() {
		System.out.println("\t\t\t----Setting up server for peerProcess: " + _peerID);
		System.out.println("\t\t\t\tTrying to get this thing's own port number.");
		NeighborInfo ni = getNeighborInfo(_peerID);
		int portNum = ni.getPortNum();
		System.out.println("\t\t\t\tni.getPortNum() = " + portNum);

		_server = new Server(this);
		_server.run(portNum);
		// majority of this code is from Server.java main()
		// ServerSocket listener = new ServerSocket(portNum);
		// int clientNum = 1;
		// try {
		// 	while(true) {
		// 		new Handler(listener.accept(),clientNum).start();
		// 		System.out.println("Client "  + clientNum + " is connected!");
		// 		clientNum++;
		// 	}
		// } finally {
		// 	listener.close();
		// }
		System.out.println("\t\t\t----Done setting up server for peerProcess: " + _peerID);
	}

	public void setupClient() {
		System.out.println("\t\t\t----Setting up client for peerProcess: " + _peerID);
		
		NeighborInfo ni = getNeighborInfo(_peerID);
		String hostName = ni.getHostName();

		_client = new Client(this);
		_client.run(hostName, _portNum);

		System.out.println("\t\t\t----Done setting up client for peerProcess: " + _peerID);
	}

	public NeighborInfo getNeighborInfo(int peerID) {
		for (int i = 0; i < _neighborInfos.size(); i++) {
			if (peerID == _neighborInfos.get(i).getPeerID()) {
				return _neighborInfos.get(i);
			}
		}
		return null;
	}

	public void run() {
	// 	try {
	// 		initConnections();
	// 		System.out.println("Connections started");

	// 		long unchokeTime = System.currentTimeMillis();
	// 		long optTime = System.currentTimeMillis();

	// 		List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());

	// 		while(true){
	// 			handleMessages(peerList);

	// 			if(System.currentTimeMillis() > unchokeTime + 1000*config.getUnchokingInterval()) {
	// 				unchokingUpdate();
	// 				unchokeTime = System.currentTimeMillis();
	// 			}

	// 			if(System.currentTimeMillis() > optTime + 1000*config.getOptomisticUnChokingInterval()) {
	// 				optomisticUnchokingUpdate();
	// 				optTime = System.currentTimeMillis();
	// 			}
	// 		}
	// 	} 
	// 	catch (Exception e) {
	// 		e.printStackTrace();
	// 	}
	}

	public synchronized void handleMessages(ArrayList<NeighborInfo> peers) throws Exception {
		for(NeighborInfo peer: peers) {
			// check to see if the peer has enough data to warrant a read
			if(peer._inStream.available() >= 5) {
				Message receivedMessage = new Message();
				receivedMessage.setPieceSize(_pieceSize);
				receivedMessage.readMessage(peer,_peerID); //read message
		
				switch (receivedMessage.getMessageType()) {
					case HANDSHAKE:
						handleHandshake(peer,receivedMessage);
						break;
		
					case BITFIELD:
						handleBitfield(peer,receivedMessage);
						break;
		
					case INTERESTED:
						peer._isInterested = true; //update my record of the peer saying it's interested in my
						// myLogger.logReceiveInterested(peer.peerID); //log the received interested message
						break;
		
					case NOTINTERESTED:
						peer._isInterested = false; 
						// myLogger.logNotInterested(peer.peerID);
						break;
		
					case CHOKE:
						// myLogger.logChoking(peer.peerID); //log the received notinterested message
						break;
					
					case UNCHOKE:
						handleUnchoke(peer,receivedMessage);
						break;
		
					case REQUEST:
						handleRequest(peer,receivedMessage);
						break;
		
					case PIECE:
						handlePiece(peer,receivedMessage);
						break;
						
					case HAVE:
						handleHave(peer,receivedMessage);
						break;
						
					default:
						break;
				}
			
			}
		}
	}

	public void handleHandshake(NeighborInfo peer, Message receivedMessage) throws Exception {
		System.out.println("Peer:" + _peerID + " got handshake from Peer:" + peer._peerID);
		if(peer._handshakeSent) {
			System.out.println("Peer:" + _peerID + " has already sent a HANDSHAKE to Peer:" + peer._peerID);
			System.out.println("Peer:" + _peerID + " sending bitfield to Peer:" + peer._peerID);
			// myLogger.logTCPConnTo(peer.peerID);
			receivedMessage.wipe();
			receivedMessage.sendBitField(peer,_bitfield);
		}
		else {
			System.out.println("Peer:" + _peerID + " has not yet sent a HANDSHAKE to Peer:" + peer._peerID);
			System.out.println("Peer:" + _peerID + " sending handshake 2 to Peer:" + peer._peerID);
			// myLogger.logTCPConnFrom(peer.peerID);
			receivedMessage.wipe();
			receivedMessage.sendHandShake(peer, _peerID);
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
		
		// make new peer process
		peerProcess p = new peerProcess();
		// feed it its peerID, and it will know the rest from there
		p.initialize(peerID);

		// every single peer needs to first set up its listening port / server
		p.setupConnections();

		// testing grounds

		// Message.MessageType mt1 = Message.MessageType.HAVE;
		// System.out.println("main: mt1 = " + mt1);
		ByteBuffer bb = ByteBuffer.allocate(9);
		bb.putInt(500);
		bb.put((byte)6);
		bb.putInt(325);
		Message testMsg = Message.parseMessage(bb);
		System.out.println("main: testMsg.getMessageType() = " + testMsg.getMessageType());



		// if it is a HAVE or REQUEST message, use this block to test the input pieceIndex
		// ByteBuffer testbb = testMsg.getByteBuffer();
		// testbb.rewind();
		// System.out.println("testbb.getInt() = " + testbb.getInt());
		//


		// end testing grounds

	}
}
