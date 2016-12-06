public class Pieces {
	private int pieceIndex;
	private byte[] pieceBytes;

	public Pieces(int pieceIndex, byte[] pieceBytes) {
		this.pieceIndex = pieceIndex;
		this.pieceBytes = pieceBytes;
	}

	public byte[] getPieceBytes() {
		return pieceBytes;
	}

	public int getPieceIndex() {
		return pieceIndex;
	} 
}