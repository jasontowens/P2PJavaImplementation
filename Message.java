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


import java.nio.*;

public abstract class Message 
{
	
	// 32 bit int represents the length of the message
	// "not including the length of the msg length field itself"
	// as the project description requires
	private final int _msgLenField = 4;
	private ByteBuffer _data;
	private MessageType _messageType;

	public Message(ByteBuffer bb, MessageType mt) 
	{
		_data = bb;
		_messageType = mt;
		// todo brian _data.rewind()
		// resets the position to 0 
		_data.rewind();
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
		PIECE(7);

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

	public MessageType getType() 
	{
		return _messageType;
	}

	// this returns a Message because once returned as a Message,
	// one can call Message.getType().getTypeByte which will be 
	// interpretable, and you can cast it to whatever you need if you need
	public static Message parseMessage(ByteBuffer bb) 
	{
		bb.rewind();
		// reads next 32 bits and returns its as an int, exactly what we need to do
		int messageLength = bb.getInt();
		System.out.println("messageLength = " + messageLength);
		byte messageTypeByte = bb.get();
		System.out.println("messageTypeByte = " + (int)messageTypeByte);
		// MessageType messageType = MessageType.messageTypeByte;
		MessageType messageType = MessageType.get(messageTypeByte);
		System.out.println("messageType = " + messageType);

		switch (messageType) {

			case CHOKE:
				return ChokeMessage.parseMessage((bb).slice());

			// case UNCHOKE:
			// 	return UnchokeMessage.parseMessage((bb).slice());

			// case INTERESTED:
			// 	return InterestedMessage.parseMessage((bb).slice());

			// case NOT_INTERESTED:
			// 	return NotInterestedMessage.parseMessage((bb).slice());

			// case HAVE:
			// 	return HaveMessage.parseMessage((bb).slice());

			// case BITFIELD:
			// 	return BitfieldMessage.parseMessage((bb).slice());

			// case REQUEST:
			// 	return RequestMessage.parseMessage((bb).slice());

			// case PIECE:
			// 	return PieceMessage.parseMessage((bb).slice());

			default:
				System.out.println("Error, MessageType of parsed Message is not valid");
				// todo Brian add logging to this statement		
				throw new IllegalArgumentException("Invalid ByteBuffer fed into parseMessage");

		}
	}

	// nested class for piece contents
	class PieceMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
		//      index: integer specifying the zero-based piece index
    	//		begin: integer specifying the zero-based byte offset within the piece
    	//		block: block of data, which is a subset of the piece specified by index.

		private int _pieceIndex;
		private int _offset;
		private ByteBuffer block;
	}

	class RequestMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
	    // 		index: integer specifying the zero-based piece index
	    // 		begin: integer specifying the zero-based byte offset within the piece
	    // 		length: integer specifying the requested length.

		private int _pieceIndex;
		private int _offset;
		private int _length;


	}

	class BitfieldMessage extends Message 
	{
		// todo brian figure out the representation of bitfield and how we
		// want to manage both pieces and actual data
	}

	class HaveMessage extends Message 
	{
		// from https://wiki.theory.org/BitTorrentSpecification
		//The have message is fixed length. The payload is the zero-based 
		//index of a piece that has just been successfully downloaded and verified via the hash. 

		private int _pieceIndex;
	}

	class NotInterestedMessage extends Message 
	{
		// no payload

	}

	class InterestedMessage extends Message 
	{
		// no payload
		
	}

	class UnchokeMessage extends Message 
	{
		// no payload
		
	}

	class ChokeMessage extends Message 
	{
		// no payload

		public ChokeMessage() {
			// add one because it needs a byte for the MessageType (CHOKE)
			ByteBuffer bb = ByteBuffer.allocate(_msgLenField + 1);
			// message length = 1
			bb.putInt(1); 
			byte mt = Message.MessageType.CHOKE.getTypeByte();
			// bb.put(Message.MessageType.CHOKE.getTypeByte);
			bb.put(mt);
			_data = bb;
			_messageType = Message.MessageType.CHOKE;
			// todo brian _data.rewind()
			// resets the position to 0 
			_data.rewind();
		}

	}

} // end class Message


