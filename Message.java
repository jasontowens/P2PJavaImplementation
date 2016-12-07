// an abstract class for which messages will inherit, includes basic data members and methods
// message type is an enum
// just as the project specifications state:
//		0 - choke
//		1 - unchoke
//		2 - interested
//		3 - not interested
//		4 - have
// 		5 - bitfield
//		6 - request
//		7 - piece

import java.io.*;
import java.net.*;
import java.nio.*;

public class Message 
{
	
	// 32 bit int represents the length of the message
	// "not including the length of the msg length field itself"
	// as the project description requires
	private final int _msgLenField = 4;
	// private ByteBuffer _data;
	private byte [] _data;
	private MessageType _messageType;
	private int _pieceSize;
	private int _msgLength;

	public Message(ByteBuffer bb, MessageType mt) 
	{
		// _data = bb;
		_messageType = mt;

		// resets the position to 0 
		// _data.rewind();
	}

	public Message()
	{
		//empty constructor, may want to remove later
	}

	public enum MessageType 
	{
		CHOKE(0),
		UNCHOKE(1),
		INTERESTED(2),
		NOTINTERESTED(3),
		HAVE(4),
		BITFIELD(5),
		REQUEST(6),
		PIECE(7),
		HANDSHAKE(8); // added hand shake so we can parse all messages without invalidating enum

		// what type of message is it
		private byte _id;

		// constructor so that enum functions take it from an int to a byte
		MessageType(int id) 
		{
			// cast the incoming int as a byte and assign it
			_id = (byte) id;
		}

		// this takes in a byte because MessageType in the description is
		// 8 bits
		// static because we want to be able to just call MessageType.get(7)
		// and know that it means PIECE
		public static MessageType get(byte c) 
		{
			for (MessageType mt : MessageType.values()) 
			{
				System.out.println("mt = " + mt + ". c = " + c);
				if (mt.getID() == c) 
				{
					return mt;	
				}
			}
			//else no match
			return null;
		}

		public byte getTypeByte() 
		{
			return _id;
		}

		public byte getID() {
			return _id;
		}



	};  // end enum

	public MessageType getMessageType() 
	{
		return _messageType;
	}

	public void setMessageType(MessageType mt) {
		_messageType = mt;
	}

	public void setMessageType(byte b) {
		_messageType = MessageType.get(b);
	}

	public void setData(byte[] data){
		_data = data;
	}

	public void setPieceSize(int pieceSize) {
		_pieceSize = pieceSize;
	}

	public void wipe() {
		_messageType = null;
		_data = null;
		_msgLength = 0;
	}
	// public ByteBuffer getByteBuffer() {
	// 	return _data.duplicate();
	// }

	public byte[] getData() {
		return _data;
	}

	// will work with multithreaded parts
	// 
	public synchronized void readMessage(NeighborInfo peer, int myID) throws IOException {
		byte [] temp = new byte[5];
		int numBytesRecvd = 0;
		int totalBytesRecvd = 0;
		while (totalBytesRecvd < 5) {
			// 	arguments: byte array stored in, offset, length
			// from java docs:
			// Reads up to len bytes of data from the contained input stream into an array of bytes.
			numBytesRecvd = peer._inStream.read(temp, totalBytesRecvd, 5 - totalBytesRecvd);
			totalBytesRecvd += numBytesRecvd;
		}

		// first thing is first: we need to see if it is a handshake
		// testing to see if the 5th byte is I is sufficient because the 5th byte in all other messages
		// is an integer representing MessageType
		if (temp[4] == (byte)'I') { 

			//we're going to need to get the next 27 bytes to get all the way to the peerID which is the last 4
			byte [] temp1 = new byte[27];
			totalBytesRecvd = 0;
			while (totalBytesRecvd < 27) {
				// arguments: byte array stored in, offset, length
				// from java docs:
				// Reads up to len bytes of data from the contained input stream into an array of bytes.
				numBytesRecvd = peer._inStream.read(temp, totalBytesRecvd, 5 - totalBytesRecvd);
				totalBytesRecvd += numBytesRecvd;
			}			
			byte[] peerIdToBe = { temp1[23], temp1[24], temp1[25], temp1[26] };
			String shakeId = new String(peerIdToBe);
			int id = Integer.parseInt(shakeId);

			if (id != peer._peerID) {
				System.out.println("Error in Message::readMessage().  peerID received is not the peerID expected.");
				System.exit(0);
				
			}
			//indicate that this is a handshake
			_messageType = MessageType.get((byte)8);
		}
		else {
			ByteBuffer bb = ByteBuffer.wrap(temp);

			_msgLength = bb.getInt();
			_messageType = MessageType.get(temp[4]);
			// to do, see if 0 works
			if (_msgLength > 1) {
				_data = new byte[_msgLength - 1];
				totalBytesRecvd = 0;
				while( totalBytesRecvd < _msgLength -  1) {
					numBytesRecvd = peer._inStream.read(_data, totalBytesRecvd, _msgLength - totalBytesRecvd - 1);
					totalBytesRecvd += numBytesRecvd;
				}
			}

			System.out.println("In message::readMessage() - peerID:" + myID 
				+ " received a messageType = " + _messageType + " of length " 
				+ _msgLength + " from Peer:" + peer._peerID);
		}
	}

	// this returns a Message because once returned as a Message,
	// one can call Message.getMessageType().getTypeByte which will be 
	// interpretable, and you can cast it to whatever you need if you need
	public static Message parseMessage(ByteBuffer bb) 
	{
		bb.rewind();
		// reads next 32 bits and returns its as an int, exactly what we need to do
		int messageLength = bb.getInt();

		// grab the message type, it is going to signal what type of message we are reading
		byte messageTypeByte = bb.get();
		
		// parse the byte we just gathered into our new enum MessageType
		MessageType messageType = MessageType.get(messageTypeByte);

		// System.out.println("messageLength = " + messageLength);
		// System.out.println("messageTypeByte = " + (int)messageTypeByte);
		// MessageType messageType = MessageType.messageTypeByte;
		// System.out.println("messageType = " + messageType);

		switch (messageType) {

			case CHOKE:
				return ChokeMessage.parseMessage((bb).slice());
				// _data = bb;
				// _messageType = messageType;
				// return this;

			case UNCHOKE:
				return UnchokeMessage.parseMessage((bb).slice());

			case INTERESTED:
				return InterestedMessage.parseMessage((bb).slice());

			case NOTINTERESTED:
				return NotInterestedMessage.parseMessage((bb).slice());

			case HAVE:
				return HaveMessage.parseMessage((bb).slice());

			// case BITFIELD:
			// 	return BitfieldMessage.parseMessage((bb).slice());

			case REQUEST:
				return RequestMessage.parseMessage((bb).slice());

			// case PIECE:
			// 	return PieceMessage.parseMessage((bb).slice());

			default:
				System.out.println("Error, MessageType of parsed Message is not valid");
				// todo Brian add logging to this statement		
				throw new IllegalArgumentException("Invalid ByteBuffer fed into parseMessage");

		}
	}



	public synchronized void sendMessage(NeighborInfo peer) throws IOException {
		if(_data == null){
			_msgLength = 1; 
		}
		else{
			_msgLength = _data.length + 1; 
		}

		byte[] outgoingMessage = new byte[5];
		ByteBuffer bb = ByteBuffer.wrap(outgoingMessage);
		bb.putInt(_msgLength);
		outgoingMessage[4] = _messageType.getTypeByte();
		
		try {
			peer._outStream.write(outgoingMessage, 0, outgoingMessage.length);
			if(_data != null){
				peer._outStream.write(_data, 0, _data.length);
			}
			
			
		}
		catch(IOException e) {
			System.out.println(e.toString());
		}
		peer._outStream.flush();
	}

	public void sendHandShake(NeighborInfo peer, int myID) throws IOException {
		
		try {
			byte[] handshake = new byte[32];
			
			String handshakeHeader = "P2PFILESHARINGPROJ";
			String zeroBits = "0000000000";
			String id = String.valueOf(myID);
			// while(id.length() != 4){
			// 	id = "0" + id;
			// }
			String combinedHandshake = handshakeHeader + zeroBits + id;
			handshake = combinedHandshake.getBytes();

			peer._outStream.write(handshake, 0, handshake.length);
		}
		catch(IOException e) {
			System.out.println("Error with sending handshake. Could not write to stream");
		}

		peer._outStream.flush();
	}

	public synchronized void sendInterested(NeighborInfo peer) throws IOException {
		_messageType = MessageType.get((byte)2);
		sendMessage(peer);
	}

	public synchronized void sendNotInterested(NeighborInfo peer) throws IOException {
		_messageType = MessageType.get((byte)3);
		sendMessage(peer);
	}

	public synchronized void sendHave(NeighborInfo peer, int pieceIndex) throws IOException {
		_messageType = MessageType.get((byte)4);
		ByteBuffer bb = ByteBuffer.allocate(4); 
		_data = bb.putInt(pieceIndex).array();
		sendMessage(peer);
	}	

	public synchronized void sendBitField(NeighborInfo peer, BitField bitfield) throws IOException {
		_messageType = MessageType.get((byte)5);
		_data = bitfield.toBytes(); //set the payload bitfield bytes
		sendMessage(peer); //send bitfield
	}

	public synchronized void sendRequest(NeighborInfo peer, int pieceIndex) throws IOException {
		_messageType = MessageType.get((byte)6);
		
		ByteBuffer bb = ByteBuffer.allocate(4); // setup byte buffer for data which is 4 byte aka 32bit int
		_data = bb.putInt(pieceIndex).array(); // put pieceIndex into the bytebuffer, and then make it a byte array

		sendMessage(peer);
	}


	// todo brian rename some stuff here
	public synchronized void sendPiece(NeighborInfo peer, Pieces piece) throws IOException {
		_messageType = MessageType.get((byte)7);
		byte[] msg = new byte[piece.getPieceBytes().length + 4]; // create byte array for data
		ByteBuffer bb = ByteBuffer.wrap(msg); // create byte buffer for payload
		byte[] index = bb.putInt(piece.getPieceIndex()).array(); // put pieceIndex in the the first 4 bytes of the payload
		
		System.arraycopy(index, 0, msg, 0, index.length);
		System.arraycopy(piece.getPieceBytes(), 0, msg, 4, piece.getPieceBytes().length); //copy the piece byte array to the payload array 
		
		
		_data = msg; // set the payload to msg we just worked to create
		sendMessage(peer);
	}







	// nested class for piece contents
	public static class PieceMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
		//      index: integer specifying the zero-based piece index
    	//		begin: integer specifying the zero-based byte offset within the piece
    	//		block: block of data, which is a subset of the piece specified by index.

		private int _pieceIndex;
		private int _offset;
		private ByteBuffer block;
	}

	public static class RequestMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
	    // 		index: integer specifying the zero-based piece index
	    // 		begin: integer specifying the zero-based byte offset within the piece
	    // 		length: integer specifying the requested length.

		
		//our specifications are different from bittorrent so diregard begin and length
		// this makes it very similar to HaveMessage
		private int _pieceIndex;


		public RequestMessage(ByteBuffer bb, int pieceIndex) 
		{
			super(bb, MessageType.REQUEST);
			_pieceIndex = pieceIndex;
		}

		public static RequestMessage parseMessage(ByteBuffer bb)
		{
			// okay to getInt() from the buffer because the super class's
			// parseMessage() has already removed the messageLength and MessageType
			// fields
			int pieceIndex = bb.getInt();
			return (RequestMessage) new RequestMessage (bb, pieceIndex);
		}
	}

	public static class BitfieldMessage extends Message 
	{
		// todo brian figure out the representation of bitfield and how we
		// want to manage both pieces and actual data
	}

	public static class HaveMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
		//The have message is fixed length. The payload is the zero-based 
		//index of a piece that has just been successfully downloaded and verified via the hash. 

		private int _pieceIndex;

		public HaveMessage(ByteBuffer bb, int pieceIndex) 
		{
			super(bb, MessageType.HAVE);
			_pieceIndex = pieceIndex;
		}

		public static HaveMessage parseMessage(ByteBuffer bb)
		{
			// okay to getInt() from the buffer because the super class's
			// parseMessage() has already removed the messageLength and MessageType
			// fields
			int pieceIndex = bb.getInt();
			return (HaveMessage) new HaveMessage (bb, pieceIndex);
		}


	}

	public static class NotInterestedMessage extends Message 
	{
		// no payload
		public NotInterestedMessage(ByteBuffer bb)
		{
			super(bb, MessageType.NOTINTERESTED);
		}

		public static NotInterestedMessage parseMessage(ByteBuffer bb)
		{
			System.out.println("here 3");
			return (NotInterestedMessage) new NotInterestedMessage(bb);
		}
	}

	public static class InterestedMessage extends Message 
	{
		// no payload
	
		public InterestedMessage(ByteBuffer bb)
		{
			super(bb, MessageType.INTERESTED);
		}

		public static InterestedMessage parseMessage(ByteBuffer bb)
		{
			return (InterestedMessage) new InterestedMessage(bb);
		}

	}

	public static class UnchokeMessage extends Message 
	{
		// no payload
		public UnchokeMessage(ByteBuffer bb)
		{
			super(bb, MessageType.UNCHOKE);
		}

		public static UnchokeMessage parseMessage(ByteBuffer bb) 
		{
			System.out.println("here 1");
			return (UnchokeMessage) new UnchokeMessage(bb);
		}

	}

	public static class ChokeMessage extends Message 
	{
		// no payload

		public ChokeMessage(ByteBuffer bb) {
			// // add one because it needs a byte for the MessageType (CHOKE)
			// ByteBuffer bb = ByteBuffer.allocate(_msgLenField + 1);
			// // message length = 1
			// bb.putInt(1); 
			// byte mt = Message.MessageType.CHOKE.getTypeByte();
			// // bb.put(Message.MessageType.CHOKE.getTypeByte);
			// bb.put(mt);
			// _data = bb;
			// _messageType = Message.MessageType.CHOKE;
			// // todo brian _data.rewind()
			// // resets the position to 0 
			// _data.rewind();
			super(bb, MessageType.CHOKE);
		}

		public  static ChokeMessage parseMessage(ByteBuffer bb)
		{
			return (ChokeMessage) new ChokeMessage(bb);
		}

	}

} // end class Message


