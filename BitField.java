import java.util.Random;

public class BitField {
	
	private boolean finished;
	private int piecesCountDowned;
	private boolean[] bitField;
	private final int piecesCount;
	
	public BitField(int piecesCount) {	
		finished = false;
		piecesCountDowned = 0;
		this.piecesCount = piecesCount;
		bitField = new boolean[piecesCount];
		for (int i = 0; i < piecesCount; i++) {
			bitField[i] = false;
		}
	}

	public void turnOnBit(int which) {
		
		if (bitField[which] == false) {
			bitField[which] = true;
			piecesCountDowned++;
			if (piecesCountDowned == piecesCount) {
				finished = true;
			}
		}

	}
	
	public void turnOnAll() {
		for (int i = 0; i < piecesCount; i++) {
			bitField[i] = true;
		}
		piecesCountDowned = piecesCount;
		finished = true;
	}
	
	public boolean isFinished() {
		return finished;
	}

	public byte[] toBytes() {
		
		int numBytes;
		if (piecesCount%8 == 0 ) {
			numBytes = piecesCount/8;
		} else {
			numBytes = piecesCount/8 + 1;
		}
		
		byte[] bytes = new byte[numBytes];
		for (int i = 0; i < numBytes; i++) {
			bytes[i] = (byte)0;
		}
		for (int i = 0; i < piecesCount; i++) {
			int whichByte = i/8;
			int whichBit = i%8;
			if (bitField[i] == true) {
				bytes[whichByte] = (byte) (bytes[whichByte] | (1 << whichBit));
			} else {
				bytes[whichByte] = (byte) (bytes[whichByte] & ~(1 << whichBit));
			}
		}
		
		return bytes;
	}
	
	public void setBitField(byte[] bytes) {
		
		piecesCountDowned = 0;
		for (int i = 0; i < piecesCount; i++) {
			int whichByte = i/8;
			int whichBit = i%8;
			if ((bytes[whichByte] & (1 << whichBit)) == 0) {
				bitField[i] = false;
			} else {
				bitField[i] = true;
				piecesCountDowned++;
			}
		}
		
		if (piecesCountDowned == piecesCount) {
			finished = true;
		}
	} 
 	
	public int getInterestingIndex(BitField b) {
		int index = -1 ; 
		for (int i = 0; i < piecesCount; i++) {
			if ((bitField[i] == false) && b.bitField[i] == true) {
				return i;
			} 
		}
		return index;
	}

	public int getRandomNeededIndex(BitField b) {
		int[] indexList = new int[piecesCount];
		int j = 0;
		for (int i = 0; i < piecesCount; i++) {
			if ((bitField[i] == false) && b.bitField[i] == true) {
				indexList[j] = i;
				j++; 
			} 
		}
		System.out.println("Number of uncommon stuff " + j);
		Random rand = new Random();
		int randIndex = rand.nextInt(j+1); 

		if (j!=0) {
			return indexList[randIndex];
		}
		else {
			return -1;
		}
	}

	public String getText() {
		StringBuffer text = new StringBuffer();
		for (int i = 0 ; i < this.piecesCount; i++) {
			if (bitField[i]) {
				text.append("1");
			} else {
				text.append("0");
			}
		}
		return text.toString();
	}

	public int getPiecesCountDowned() {
		return piecesCountDowned;
	}
}
