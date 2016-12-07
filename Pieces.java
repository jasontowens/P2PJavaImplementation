// Pieces are simply a number and a bytearray
// used to know where within a file the piece is and what its actual data is
public class Pieces {
	private int _pieceIndex;
	private byte[] _pieceBytes;

	public Pieces(int pieceIndex, byte[] pieceBytes) {
		_pieceIndex = pieceIndex;
		_pieceBytes = pieceBytes;
	}

	public int getPieceIndex() {
		return _pieceIndex;
	} 
	
	public byte[] getPieceBytes() {
		return _pieceBytes;
	}
}