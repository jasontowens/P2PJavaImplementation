import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

//peerProcess is our largest class.  it is responsible for having a list of NeighborInfo objects which
// in turn store a lot of information (see NeighborInfo.java) on peers that it knows about.  
// Responsibilites include: managing its server and client functionalities, handling messages and updating
// its _neighborInfos with the meaning of the message, creating messages and sending them.


public class peerProcess implements Runnable{
	// don't think it needs this at the moment, needs to only know info about its neighbors
	// private PeerInfo _myPeerInfo;
	ArrayList<NeighborInfo> _neighborInfos;
	private FileManager _fileManager;
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
		//calculate pieceCount
		
		_numPieces = _fileSize / _pieceSize;
		_bitfield = new BitField(_numPieces);

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
	public void setPeerID(int newID){
		_peerID = newID;
	}

	public int getPeerID(){
		return _peerID;
	}

	public BitField getBitfield(){
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
		
		int count = 0;
		for (NeighborInfo peer: sortedNeighbors) {
			if (peer._peerID == _peerID) {
				break;
			}
			
			count++;
		}
		
		sortedNeighbors.remove(count); //ensure my peer info isn't in the list
		
		for (NeighborInfo peer: sortedNeighbors) {
			//if we appear first we are a server
			if(_peerID < peer._peerID) {
				try {
					String hostName = peer.getHostName();
					
					System.out.println("Peer:" + _peerID + " listening for hostname " + hostName + " via socket " + _portNum);
					ServerSocket serv = new ServerSocket(_portNum); //create server socket
					Socket socket = serv.accept();	//now listen for requests
					serv.close(); //close the server socket now that it is not needed
					System.out.println("Peer:" + _peerID + " heard news from " + socket.getInetAddress().toString());

					//create input and output data streams, and save them in the peer
					DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
					System.out.println("Here is the outStream: " + outStream);
					outStream.flush();
					
					DataInputStream inStream = new DataInputStream(socket.getInputStream());
					System.out.println("Here is the inStream: " + inStream);
					
					peer._inStream = inStream;
					peer._outStream = outStream;
					peer._socket = socket;
					
					int peerLoop = 0;
					for (NeighborInfo peerToUpdate: _neighborInfos) {
						if (peerToUpdate._peerID == peer._peerID) {
							_neighborInfos.set(peerLoop, peer);
							break;
						}
						
						peerLoop++;
					}
					
					Message handShake = new Message();
					handShake.setPieceSize(_pieceSize);					

					System.out.println("Calling readMessage");
					handShake.readMessage(peer, _peerID);
					
					System.out.println("Calling handleHandshake");
					handleHandshake(peer, handShake);
					
					System.out.println("Finished calling handleHandshake");
				}
				catch (Exception e) {
					System.out.println("BAD in Server.run()");
					StackTraceElement[] elements = e.getStackTrace();  
					for (int iterator=1; iterator<=elements.length; iterator++)  {
						System.out.println("Class Name:"+elements[iterator-1].getClassName()+" Method Name:"+elements[iterator-1].getMethodName()+" Line Number:"+elements[iterator-1].getLineNumber());
					}
				}
			} 			
			//if we appear second we are a client
			else if(_peerID > peer._peerID) {
				try {
					String hostName = peer.getHostName();
					int portNum = peer.getPortNum();

					System.out.println("Peer:" + _peerID + " trying to connect to " + hostName + " via socket " + portNum);
					Socket socket = new Socket(hostName, portNum);
					
					DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
					outStream.flush();
					DataInputStream inStream = new DataInputStream(socket.getInputStream());
					
					peer._inStream = inStream;
					peer._outStream = outStream;
					peer._socket = socket;

					int peerLoop = 0;
					for (NeighborInfo peerToUpdate: _neighborInfos) {
						if (peerToUpdate._peerID == peer._peerID) {
							_neighborInfos.set(peerLoop, peer);
							break;
						}
						
						peerLoop++;
					}

					//create input and output data streams, and save them in the peer
					Message handShake = new Message();
					handShake.setPieceSize(_pieceSize);
					handShake.sendHandShake(peer, _peerID);
					peer._handshakeSent = true;
					System.out.println("Peer:" + _peerID + " sent handshake to Peer:" + peer._peerID);
				}
				catch (ConnectException e) {
					System.err.println("Connection refused. You need to initiate a server first.");
				} 
				catch(UnknownHostException unknownHost){
					System.err.println("You are trying to connect to an unknown host!");
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
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

	public synchronized void handleMessages() throws Exception {		
		for(NeighborInfo peer : _neighborInfos) {			
			// check to see if the peer has enough data to warrant a read
			if(peer._inStream.available() >= 5) {
				Message receivedMessage = new Message();
				receivedMessage.setPieceSize(_pieceSize);
				receivedMessage.readMessage(peer,_peerID); //read message
				
				System.out.println("Received message of type inside handleMessages: " + receivedMessage.getMessageType());

				switch (receivedMessage.getMessageType()) {
					case HANDSHAKE:
						handleHandshake(peer,receivedMessage);
						break;
		
					case BITFIELD:
						System.out.println("Calling handleBitfield");
						handleBitfield(peer,receivedMessage);
						break;
		
					case INTERESTED:
						peer._isInterested = true; //update my record of the peer saying it's interested in my
						break;
		
					case NOTINTERESTED:
						peer._isInterested = false; 
						// myLogger.logNotInterested(peer._peerID);
						break;
		
					case CHOKE:
						// if you are choked, it pretty much means you want to do nothing to the 
						// neighbor that choked you, because it is useless
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

			int peerLoop = 0;
			for (NeighborInfo peerToUpdate: _neighborInfos) {
				if (peerToUpdate._peerID == peer._peerID) {
					_neighborInfos.set(peerLoop, peer);
					break;
				}
				
				peerLoop++;
			}
		}
	}

	// when you are unchoked you will proceed to request to the peer that unchoked you
	public void handleUnchoke(NeighborInfo peer, Message receivedMessage) throws Exception {

		int pieceIndex = _bitfield.getRandomNeededIndex(peer._bitfield); // get a pieceIndex i am interested in
		if(pieceIndex != -1) {

			receivedMessage.wipe();
			receivedMessage.sendRequest(peer,pieceIndex);

			System.out.println("Peer#" + _peerID + " sent request for piece " + pieceIndex + " to Peer3" + peer._peerID);
		}
	}

	// when 
	public void handleHave(NeighborInfo peer, Message receivedMessage) throws IOException {
		byte[] data = receivedMessage.getData(); 
		ByteBuffer bb = ByteBuffer.wrap(data); 
		int pieceIndex = bb.getInt(0); //get the piece index which is 0 offset from the start

		// myLogger.logReceiveHave(peer._peerID,pieceIndex); //log receiving the have message
		
		peer._bitfield.turnOnBit(pieceIndex); 

		int interest = _bitfield.getInterestingIndex(peer._bitfield);
		receivedMessage.wipe();

		// only send if changed
		if(interest == -1 && peer._amIInterested) {
			peer._amIInterested = false; 					// record that I am not interested
			receivedMessage.sendNotInterested(peer);
		}

		else if (interest > -1 && !peer._amIInterested) {
			peer._amIInterested = true; // record that I am interested
			receivedMessage.sendInterested(peer);
		}
		
		int peerLoop = 0;
		for (NeighborInfo peerToUpdate: _neighborInfos) {
			if (peerToUpdate._peerID == peer._peerID) {
				_neighborInfos.set(peerLoop, peer);
				break;
			}
			
			peerLoop++;
		}

	}

	public void handleBitfield(NeighborInfo peer, Message receivedMessage) throws Exception {
		peer._bitfield.setBitField(receivedMessage.getData()); //make the peers bitfield same as received
		int peerLoop = 0;
		for (NeighborInfo peerToUpdate: _neighborInfos) {
			if (peerToUpdate._peerID == peer._peerID) {
				_neighborInfos.set(peerLoop, peer);
				break;
			}
			
			peerLoop++;
		}
		
		if(!peer._handshakeSent) {
			System.out.println("Peer#" + _peerID + " sending bitfield to Peer#" + peer._peerID);
			receivedMessage.wipe();
			receivedMessage.sendBitField(peer,_bitfield);
		}
		else {
			int interestingIndex = _bitfield.getInterestingIndex(peer._bitfield); //get interesting index compared to my bitfield
			System.out.println("Here is the intersting index: " + interestingIndex);
			
			receivedMessage.wipe();
			if (interestingIndex != -1) {
				System.out.println("Peer#" + _peerID + " is interested in Peer#" + peer._peerID);
				
				peer._amIInterested = true; // record that I am interested

				receivedMessage.sendInterested(peer);
			}
			else {
				System.out.println("Peer#" + _peerID + " is not interested in Peer#" + peer._peerID);

				peer._amIInterested = false; // record that I am not interested

				receivedMessage.sendNotInterested(peer);
			}
		}
		
		peerLoop = 0;
		for (NeighborInfo peerToUpdate: _neighborInfos) {
			if (peerToUpdate._peerID == peer._peerID) {
				_neighborInfos.set(peerLoop, peer);
				break;
			}
			
			peerLoop++;
		}
	}	

	public void handleRequest(NeighborInfo peer, Message receivedMessage) throws Exception {
		ByteBuffer bb = ByteBuffer.wrap(receivedMessage.getData());
		// find out what piece the peer requested
		int pieceIndex = bb.getInt(0);
		
		if(pieceIndex != -1) {
			System.out.println("Peer#" + _peerID + " received request for piece " + pieceIndex + " from Peer#" + peer._peerID);
			// clear out the message so it is reusable
			receivedMessage.wipe();
			Pieces piece = _fileManager.getPiece(pieceIndex); // get the piece at the index
			receivedMessage.sendPiece(peer, piece);
			System.out.println("Peer#" + _peerID + " sent piece " + pieceIndex + " to Peer#" + peer._peerID);
		}
		else {
			System.out.println("Peer#" + _peerID + " received a request from Peer#" + peer._peerID + " but they already have that piece");
		}
	}

	public void handlePiece(NeighborInfo peer, Message receivedMessage) throws Exception {
		byte[] data = receivedMessage.getData(); 
		ByteBuffer bb = ByteBuffer.wrap(data);
		int pieceIndex = bb.getInt(0);

		byte[] pieceBytes = new byte[data.length - 4];
		// an array copy of the piece (located at offset 4) into the pieceBytes 
		System.arraycopy(data, 4, pieceBytes, 0, data.length - 4); 
		Pieces piece = new Pieces(pieceIndex, pieceBytes); // create a concrete piece from the data

		System.out.println("Peer#" + _peerID + " received and downloaded piece " + pieceIndex + " from Peer#" + peer._peerID);

		_fileManager.putPiece(piece); //place piece in file
		_bitfield.turnOnBit(pieceIndex); // change the bitfield so it is not requested again

		// myLogger.logPieceDownload(peer._peerID,pieceIndex,_bitfield.getPiecesCountDowned()); //log the piece download

		// todo brian clean up
		for(NeighborInfo neighbor : _neighborInfos) {
			receivedMessage.wipe();
			receivedMessage.sendHave(neighbor, pieceIndex);
		}
		
		if(_bitfield.isFinished()) {
			System.out.println("Peer#" + _peerID + " is finished");
			// myLogger.logDownloadComp(); 
			// readyToExit();
			// todo brian impl ready to exit

		}
		// when you get the piece and you are not finished, you want to immediately send another request.
		// todo brian, perhaps move the message creation to Message.java
		else {
			int newPieceIndex = _bitfield.getRandomNeededIndex(peer._bitfield);	
			bb = ByteBuffer.allocate(4); // setup byte buffer for data
			byte[] msg = bb.putInt(newPieceIndex).array(); // put pieceIndex in byte[]
			Message requestMsg = new Message();
			requestMsg.setPieceSize(_pieceSize);
			requestMsg.setMessageType((byte) 6);
			requestMsg.setData(msg);
			requestMsg.sendMessage(peer);
			
			System.out.println("Peer#" + _peerID + " sent request for piece " + newPieceIndex + " to Peer#" + peer._peerID);		
		}

		peer._speed++; //updated how many pieces i got from last unchoking round	
		
		int peerLoop = 0;
		for (NeighborInfo peerToUpdate: _neighborInfos) {
			if (peerToUpdate._peerID == peer._peerID) {
				_neighborInfos.set(peerLoop, peer);
				break;
			}
			
			peerLoop++;
		}
	}

	// from project specs:
	// bitfield is only sent as the first message right after handshaking is done and connection is
	// established.
	public void handleHandshake(NeighborInfo peer, Message receivedMessage) throws Exception {
		System.out.println("Peer#" + _peerID + " got handshake from Peer#" + peer._peerID);
		// check if this peer has also sent a hand shake
		if(peer._handshakeSent) {
			System.out.println("Peer#" + _peerID + " has already sent a HANDSHAKE to Peer#" + peer._peerID);
			System.out.println("Peer#" + _peerID + " sending bitfield to Peer#" + peer._peerID);
			receivedMessage.wipe();
			receivedMessage.setPieceSize(_pieceSize);
			receivedMessage.sendBitField(peer,_bitfield);
		}
		else {
			System.out.println("Peer#" + _peerID + " has not yet sent a HANDSHAKE to Peer#" + peer._peerID);
			System.out.println("Peer#" + _peerID + " sending handshake 2 to Peer#" + peer._peerID);
			receivedMessage.wipe();
			receivedMessage.setPieceSize(_pieceSize);
			receivedMessage.sendHandShake(peer, _peerID);
		}
	}



	public void run() {
		try {
			// todo remove parameter from function
			initialize(_peerID);
			setupConnections();
			System.out.println("Connections started");

			// long unchokeTime = System.currentTimeMillis();
			// long optTime = System.currentTimeMillis();

			// List<PeerRecord> peerList = new ArrayList<PeerRecord>(peerMap.values());

			while(true){
				// todo remove parameter from function
				handleMessages();

				// if(System.currentTimeMillis() > unchokeTime + 1000*config.getUnchokingInterval()) {
				// 	unchokingUpdate();
				// 	unchokeTime = System.currentTimeMillis();
				// }

				// if(System.currentTimeMillis() > optTime + 1000*config.getOptomisticUnChokingInterval()) {
				// 	optomisticUnchokingUpdate();
				// 	optTime = System.currentTimeMillis();
				// }
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}




	public static void main(String[] args) {

		int peerID;
		try {
			peerID = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException | ArrayIndexOutOfBoundsException e ) {
			System.out.println(args[0] + " is an invalid peerID");
			System.out.println("You must supply the peerID when starting the program. Terminating.");
			return;
		}
		
		// make new peer process
		// peerProcess p = new peerProcess();
		// feed it its peerID, and it will know the rest from there
		// p.initialize(peerID);

		// every single peer needs to first set up its listening port / server
		// p.setupConnections();

		peerProcess p = new peerProcess();
		p.setPeerID(peerID);
		p.run();

		// testing grounds

		// end testing grounds

	}
}
