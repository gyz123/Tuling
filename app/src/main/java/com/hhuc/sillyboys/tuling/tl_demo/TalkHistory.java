package com.hhuc.sillyboys.tuling.tl_demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.Constant;
import com.algebra.sdk.entity.HistoryRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TalkHistory {
	public static final String TAG = "talk.history.file";
	private static final byte UNUSED = (byte) 0xff;
	private static final byte PLAYED  = (byte) 0x01;
	private static final byte UNPLAYED = (byte) 0x00;
	
	private int UserId = 0;
	private int SessionId = 0;
	
	private String Path0 = null;
	private String FilePath = "/tourlink/history";
	private RandomAccessFile speechFile = null;
	private RandomAccessFile indexFile = null;
	private RandomAccessFile speechFile2 = null;
	private RandomAccessFile indexFile2 = null;
	
	private static final int IDX_RECORD_LENGTH = 32;
	private static int MAX_RECORDS = 101;
	public static final String TourLinkHistoryInfo = "history.info.tourlink";
	public static final String TourLinkHistoryKey1 = "max_records_length";
	
	public TalkHistory(int uid) {
		this.UserId = uid;
		
		Log.i(TAG, "talk history for "+uid+" is created.");
		Path0 = Environment.getExternalStorageDirectory().getPath() + FilePath;
		File dir = new File(Path0);
		if (!dir.exists()) {
			dir.mkdirs();
		} else {
			clearUserHistory(dir, UserId);
		}
		
		speechFile = null;
		indexFile = null;
	}
	
	public boolean openFiles4Write(int uid, int sid) {
		sid = transEchoId(sid);
		closeFiles();
		return openFiles(uid, sid);
	}

	private boolean openFiles(int uid, int sid) {
		sid = transEchoId(sid);
		synchronized (this) {
			if (uid != UserId)
				return false;
			this.SessionId = sid;
			
			String file1 = Path0 + File.separator + makeFileName(UserId, SessionId, 0) + ".idx";
			String file2 = Path0 + File.separator + makeFileName(UserId, SessionId, 0) + ".dat";
			try {
				this.indexFile = new RandomAccessFile(file1, "rw");
				this.speechFile = new RandomAccessFile(file2, "rw");
			} catch (IOException e) {
				e.printStackTrace();
				this.indexFile = null;
				this.speechFile = null;
				return false;
			}
			
			String file3 = Path0 + File.separator + makeFileName(UserId, SessionId, 1) + ".idx";
			String file4 = Path0 + File.separator + makeFileName(UserId, SessionId, 1) + ".dat";
			File oldFile1 = new File(file1);
			File oldFile2 = new File(file2);
			if (oldFile1.exists() && oldFile2.exists()) {
				try {
					this.indexFile2 = new RandomAccessFile(file3, "rw");
					this.speechFile2 = new RandomAccessFile(file4, "r");
				} catch (FileNotFoundException e) {
				//	e.printStackTrace();
					this.indexFile2 = null;
					this.speechFile2 = null;
					return false;
				}
			}
			return true;
		}
	}
	
	public void closeFiles() {
		synchronized(this) {
			if (this.indexFile != null) {
				try {
					this.indexFile.close();
				} catch (IOException e) {}
				this.indexFile = null;
			}
			if (this.speechFile != null) {
				try {
					this.speechFile.close();
				} catch (IOException e) {}
				this.speechFile = null;
			}
			if (this.indexFile2 != null) {
				try {
					this.indexFile2.close();
				} catch (IOException e) {}
				this.indexFile2 = null;
			}
			if (this.speechFile2 != null) {
				try {
					this.speechFile2.close();
				} catch (IOException e) {}
				this.speechFile2 = null;
			}
		}
	}
	
	public static final int AMR475_PL_SIZE = 12;
	public HistoryRecord[] getAllHistoryRecords(int sid) {
		sid = transEchoId(sid);
		RandomAccessFile idxFile1 = indexFile;
		RandomAccessFile idxFile2 = indexFile2;
		
		synchronized (this) {
			if (sid != SessionId) {
				if (openIdxFiles4Read(UserId, sid)) {
					idxFile1 = idxRF1;
					idxFile2 = idxRF2;
				} else {
					return null;
				}
			}

			int total = howManyTotalRecs(idxFile1, idxFile2);
			HistoryRecord[] records = new HistoryRecord[total];
			for(int i=1;i<=total;i++) {
				byte rec[] = getRecordByIndex(i, idxFile1, idxFile2);
				records[i-1] = new HistoryRecord( 	(rec[0] == PLAYED),
													(int)getLong(rec,6,10),
													(int)getLong(rec,2,6),
													getLong(rec,10,18),
													(int)(getLong(rec,26,30)/AMR475_PL_SIZE)*2);
			}
			
			if (sid != SessionId)
				closeIdxFiles();
			
			return records;
		}
	}
	
	private int howManyTotalRecs(RandomAccessFile idxFile1, RandomAccessFile idxFile2) {
		int recs1 = 0;
		int recs2 = 0;
		if (idxFile1 != null)
			recs1 = howManyRecords(idxFile1);
		if (idxFile2 != null)
			recs2 = howManyRecords(idxFile2);
		int total = recs1 + recs2;
		total = (total > MAX_RECORDS) ? MAX_RECORDS : total;
		return total;
	}
	
	public void setHistoryLength(Context context, int len) {
		MAX_RECORDS = len;
		SharedPreferences sharedPreferences = context.getSharedPreferences(TourLinkHistoryInfo, Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putInt(TourLinkHistoryKey1, len);
		editor.commit();
	}
	
	public int getHistoryLength() {
		return MAX_RECORDS;
	}
	
	public HistoryRecord getHistoryRecord(int sid, int idx) {
		Log.i(TAG, "getHistoryRecord "+sid+" i="+idx);
		sid = transEchoId(sid);
		RandomAccessFile idxFile1 = indexFile;
		RandomAccessFile idxFile2 = indexFile2;
		
		if (idx <= 0 ||idx > MAX_RECORDS)
			return null;
		
		synchronized (this) {
			if (sid != SessionId) {
				if (openIdxFiles4Read(UserId, sid)) {
					idxFile1 = idxRF1;
					idxFile2 = idxRF2;
				} else {
					return null;
				}
			}

			byte rec[] = getRecordByIndex(idx, idxFile1, idxFile2);
			if (rec[0] == UNUSED) {
				if (sid != SessionId)
					closeIdxFiles();
				return null;
			}
			
			if (rec[0] == UNPLAYED)
				setRecordPlayed(idx, idxFile1, idxFile2);
			
			HistoryRecord rRec = new HistoryRecord((rec[0] == (byte)1),
													(int)getLong(rec,6,10),
													(int)getLong(rec,2,6),
													getLong(rec,10,18),
													(int)(getLong(rec,26,30)/AMR475_PL_SIZE)*2);
			if (sid != SessionId)
				closeIdxFiles();
			
			return rRec;
		}
	}
	
	public boolean dumpSpeechBuffer(boolean played, int sid, int speaker, long tag, byte[] inBuffer, int bytes) {
		sid = transEchoId(sid);
		if (sid != SessionId)
			return false;
		
		Log.i(TAG, "talk history dump bytes "+bytes);
		synchronized (this) {
			if (indexFile == null || speechFile == null)
				return false;
			
			try {
				checkFileSize();
				long speechPos = writeSpeechBuffer(inBuffer, bytes);
				writeIndexFile(played, speaker, SessionId, tag, bytes, speechPos);
			} catch (IOException e) {
				indexFile = null;
				speechFile = null;
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public int readSpeechBuffer(int sid, int idx, byte[] outBuffer, int offset) {
		Log.i(TAG, "readSpeechBuffer "+sid+" i="+idx);
		sid = transEchoId(sid);
		RandomAccessFile idxFile1 = indexFile;
		RandomAccessFile idxFile2 = indexFile2;
		RandomAccessFile datFile1 = speechFile;
		RandomAccessFile datFile2 = speechFile2;
		
		byte[] rec = null;
		synchronized (this) {
			if (sid != SessionId) {
				if (openIdxFiles4Read(UserId, sid) && openDatFiles4Read(UserId, sid)) {
					idxFile1 = idxRF1; idxFile2 = idxRF2;
					datFile1 = datRF1; datFile2 = datRF2;
				} else {
					return 0;
				}
			}
				
			rec = getRecordByIndex(idx, idxFile1, idxFile2);
			if (rec[0] == UNUSED) {
				if (sid != SessionId) {
					closeIdxFiles(); closeDatFiles();
				}
				return 0;
			}
			
			int rawBytes = (int)getLong(rec, 26, 30);
			long pos = getLong(rec, 18, 26);
			int bytes = 0;
			if ((int)rec[1] == 1) {
				bytes = readSpeechFile(datFile1, pos, rawBytes, outBuffer, offset);
			} else {
				bytes = readSpeechFile(datFile2, pos, rawBytes, outBuffer, offset);
			}
			if (sid != SessionId) {
				closeIdxFiles(); closeDatFiles();
			}
			return bytes;
		}
	}

	private int readSpeechFile(RandomAccessFile spchFile, long pos, int bytes, byte[] outBuffer, int offset) {
		if (spchFile == null)
			return 0;
		try {
			spchFile.seek(pos+4);
			spchFile.read(outBuffer, offset, bytes);
			return bytes;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	private void checkFileSize() throws IOException {
		if (howManyRecords(indexFile) < MAX_RECORDS)
			return;
		
		indexFile.close();
		speechFile.close();
		if (indexFile2 != null) {
			indexFile2.close();
			speechFile2.close();
		}
		
		String file1 = Path0 + File.separator + makeFileName(UserId, SessionId, 1) + ".idx";
		String file2 = Path0 + File.separator + makeFileName(UserId, SessionId, 1) + ".dat";
		File oldFile1 = new File(file1);
		File oldFile2 = new File(file2);
		if (oldFile1.exists()) {
			oldFile1.delete();
			oldFile2.delete();
		}
		
		String file3 = Path0 + File.separator + makeFileName(UserId, SessionId, 0) + ".idx";
		String file4 = Path0 + File.separator + makeFileName(UserId, SessionId, 0) + ".dat";
		File oldIdxFile = new File(file3);
		File oldSpeechFile = new File(file4);
		oldIdxFile.renameTo(oldFile1);
		oldSpeechFile.renameTo(oldFile2);
		
		this.indexFile = new RandomAccessFile(file3, "rw");
		this.speechFile = new RandomAccessFile(file4, "rw");
		this.indexFile2 = new RandomAccessFile(file1, "rw");
		this.speechFile2 = new RandomAccessFile(file2, "r");
	}
	
	/*
	 * 
	 *	Helper functions 
	 */
	
	private RandomAccessFile datRF1 = null;
	private RandomAccessFile idxRF1 = null;
	private RandomAccessFile datRF2 = null;
	private RandomAccessFile idxRF2 = null;
	private boolean openIdxFiles4Read(int uid, int sid) {
		if (uid != UserId)
			return false;
		Log.i(TAG, "openIdxFiles4Read "+sid);
		String file1 = Path0 + File.separator + makeFileName(uid, sid, 0) + ".idx";
		try {
			this.idxRF1 = new RandomAccessFile(file1, "rw");
		} catch (IOException e) {
			e.printStackTrace();
			this.idxRF1 = null;
			return false;
		}
		
		String file3 = Path0 + File.separator + makeFileName(uid, sid, 1) + ".idx";
		try {
			this.idxRF2 = new RandomAccessFile(file3, "rw");
		} catch (IOException e) {
			e.printStackTrace();
			this.idxRF2 = null;
		}
		
		return true;
	}
	
	private boolean openDatFiles4Read(int uid, int sid) {
		if (uid != UserId)
			return false;
		
		String file2 = Path0 + File.separator + makeFileName(uid, sid, 0) + ".dat";
		try {
			this.datRF1 = new RandomAccessFile(file2, "r");
		} catch (IOException e) {
			e.printStackTrace();
			this.datRF1 = null;
			return false;
		}
		String file4 = Path0 + File.separator + makeFileName(uid, sid, 1) + ".dat";
		try {
			this.datRF2 = new RandomAccessFile(file4, "r");
		} catch (IOException e) {
			e.printStackTrace();
			this.datRF2 = null;
		}
		return true;
	}

	private void closeIdxFiles() {
		if (this.idxRF1 != null) {
			try {
				this.idxRF1.close();
			} catch (IOException e) {}
			this.idxRF1 = null;
		}
		if (this.idxRF2 != null) {
			try {
				this.idxRF2.close();
			} catch (IOException e) {}
			this.idxRF2 = null;
		}
	}
	
	private void closeDatFiles() {
		if (this.datRF1 != null) {
			try {
				this.datRF1.close();
			} catch (IOException e) {}
			this.datRF1 = null;
		}
		if (this.datRF2 != null) {
			try {
				this.datRF2.close();
			} catch (IOException e) {}
			this.datRF2 = null;
		}
	}
	
	private void writeIndexFile(boolean played, int speaker, int sid, long tag, int bytes, long speechPos)
			throws IOException {
		int flag = played ? PLAYED : UNPLAYED;
		long indexPos = indexFile.length();
		indexFile.seek(indexPos);
		
		indexFile.writeByte(flag);		// 0 ->	FF:not_used 00:unplayed 01:played
		indexFile.writeByte(0);			// 1 -> reserved 
		indexFile.writeInt(sid);		// 2,3,4,5
		indexFile.writeInt(speaker);	// 6,7,8,9
		indexFile.writeLong(tag);		// 10,...,17
		indexFile.writeLong(speechPos);	// 18,...,25
		indexFile.writeInt(bytes);		// 26,27,28,29
		indexFile.writeShort(0x55aa);	// 30,31
	}

	private long writeSpeechBuffer(byte[] inBuffer, int bytes)
			throws IOException {
		long speechPos = speechFile.length();
		speechFile.seek(speechPos);
		speechFile.writeInt(bytes);
		speechFile.write(inBuffer, 0, bytes);
		return speechPos;
	}
	
	
	private void clearUserHistory(File dir, int uid) {
		File[] files = dir.listFiles();
		String FPrefix = makeFileName2(uid)+"_";
		for (File file : files) {
			if (!file.getName().toString().contains(FPrefix)) {
				file.delete();
			}
		}
	}
	
/*	private byte[] getRecordByIndex(int idx) {
		byte[] rec = new byte[IDX_RECORD_LENGTH];	// 28 bytes fixed, see writeBuffer()
		rec[0] = (byte)UNUSED;	// read error mask
		rec[1] = (byte)UNUSED;
		
		Log.i(TAG, "talk history getRecordByIndex "+idx);
		if (indexFile == null || idx == 0) 
			return rec;
		
		int recs1 = howManyRecords(indexFile);
		if (idx > recs1) {
			if (indexFile2 != null) {
				int recs2 = howManyRecords(indexFile2);
				if (idx-recs1 <= recs2)
					if (doGetRecordByIndex(indexFile2, idx-recs1, rec))
						rec[1] = (byte)0x2;		// index file No.2
			}
			return rec;
		}
	
		if (doGetRecordByIndex(indexFile, idx, rec))
			rec[1] = (byte)0x1;		// index file No.1
		return rec;
	}	*/
	
	private byte[] getRecordByIndex(int idx, RandomAccessFile idxFile1, RandomAccessFile idxFile2) {
		byte[] rec = new byte[IDX_RECORD_LENGTH];	// 28 bytes fixed, see writeBuffer()
		rec[0] = (byte)UNUSED;	// read error mask
		rec[1] = (byte)UNUSED;
		
		if (idxFile1 == null || idx == 0) 
			return rec;
		
		int recs1 = howManyRecords(idxFile1);
		if (idx > recs1) {
			if (idxFile2 != null) {
				int recs2 = howManyRecords(idxFile2);
				if (idx-recs1 <= recs2)
					if (doGetRecordByIndex(idxFile2, idx-recs1, rec))
						rec[1] = (byte)0x2;		// index file No.2
			}
			return rec;
		}
	
		if (doGetRecordByIndex(idxFile1, idx, rec))
			rec[1] = (byte)0x1;		// index file No.1
		return rec;
	}
	
	private void setRecordPlayed(int idx, RandomAccessFile idxFile1, RandomAccessFile idxFile2) {
		if (idxFile1 == null || idx == 0)
			return;
		
		int recs1 = howManyRecords(idxFile1);
		if (idx <= recs1) {
			doSetRecordPlayed(idxFile1, idx);
		} else {
			if (idxFile2 != null) {
				int recs2 = howManyRecords(idxFile2);
				if (idx-recs1 <= recs2)
					doSetRecordPlayed(idxFile2, idx-recs1);
			}
		}

		return;
	}
	
	private boolean doGetRecordByIndex(RandomAccessFile idxFile, int idx, byte[] recBuf) {
		try {
			idxFile.seek(idxFile.length() - idx * IDX_RECORD_LENGTH);
			idxFile.readFully(recBuf);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void doSetRecordPlayed(RandomAccessFile idxFile, int idx) {
		try {
			idxFile.seek(idxFile.length() - idx * IDX_RECORD_LENGTH);
			idxFile.writeByte(PLAYED);
		} catch (IOException e) {
			indexFile = null;
			speechFile = null;
			indexFile2 = null;
			speechFile2 = null;
			e.printStackTrace();
		}
	}
	
	private int howManyRecords(RandomAccessFile fd) {
		long flength = 0;
		try {
			flength = fd.length();
		} catch (IOException e) {
			flength = 0;
		};
		int recs = (int) (flength / IDX_RECORD_LENGTH);
	//	Log.i(TAG, "file has records "+recs);
		return recs;
	}
	
    private long getLong(byte[] data, int begin, int end) {
        long n = 0;
        for (; begin < end; begin++) {
                n <<= 8;
                n += data[begin] & 0xFF;
        }
        return n;
    }

	private String makeFileName(int uid, int sid, int No) {
		return makeFileName3(uid, sid)+"_"+No+"N";		
	}
	
	private String makeFileName3(int uid, int sid) {
		return makeFileName2(uid)+"_"+sid;
	}
	
	private String makeFileName2(int uid) {
		return "History_"+uid;
	}
	
	private int transEchoId(int cid) {
		CompactID cpt = new CompactID(cid);
		if (cpt.getType() == Constant.SESSION_TYPE_ECHO)
			return (cid & 0xFF000000);
		return cid;
	}
}
