import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.logging.*;

public class FileManager {
	private File _f;
	private RandomAccessFile _file;
	private int _numPieces;
	private int _pieceSize;
	private int _fileSize;
	private FileHandler _fileHandler;

	public FileManager(int numPieces, int pieceSize, int fileSize, String fileName, int peerID, boolean hasFile) throws FileNotFoundException, IOException {
		String directory = "peer_" + peerID + "/";
		File dir = new File(directory);
		// make sure the directory exists where we will write
		if(!dir.exists()) {
			dir.mkdirs();
			System.out.println("Peer#" + peerID + " creating directory " + directory);
		}
		
		_f = new File(directory + fileName);

		// to ensure that the peer has the file if it claims that it does
		if(hasFile && !_f.exists() && !_f.isDirectory()) {
			System.out.println("Peer#" + peerID + " should have the file, but it doesn't");
			System.exit(0);
		}
		_file = new RandomAccessFile(_f, "rw");
		
		_numPieces = numPieces;
		_pieceSize = pieceSize;
		_fileSize = fileSize;

		_fileHandler = new FileHandler(directory + "log_peer_" + peerID + ".log");
	}

	public Pieces getPiece(int index) throws IOException {
		int length = 0;

		// this will serve as a check whether the piece is hte last piece
		// and truncate the size to the remaining length if so
		if(index == _numPieces - 1) {
			length = _fileSize - _pieceSize * index;
		}

		else {
			length = _pieceSize;
		}

		int offSet = index * _pieceSize;
		byte[] byteArray = new byte[length]; 

		_file.seek(offSet);
		for(int i = 0; i < length; i++) {
			byteArray[i] = _file.readByte(); //read all the bytes for the piece into the byteArray
		}

		Pieces piece = new Pieces(index, byteArray); // create a piece from index and byteArray
		return piece;
	}

	public void putPiece(Pieces piece) throws IOException {
		int offSet = piece.getPieceIndex() * _pieceSize; //get the location in the file to read too
		int length = piece.getPieceBytes().length; //get the length of the piece

		byte[] byteArray = piece.getPieceBytes(); 
		_file.seek(offSet);

		for(int i = 0; i < length; i++) {
			_file.writeByte(byteArray[i]); // write to the file's bytes
		}
	}

	public FileHandler getFileHandler() {
		return _fileHandler;
	}
}