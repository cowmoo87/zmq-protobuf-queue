package utils;

public class Utils {
	
	public static final int HASH_INITVAL = 0x31415926;
	
	/**
	 * Given a string, hashes it to a long 
	 * @param content string
	 * @return a hash-value in long
	 */
	public static long hashString(String content) {
		byte[] byteArray = content.getBytes();
		return JenkinsHash.getInstance().hash(byteArray, byteArray.length, HASH_INITVAL) & 0x00000000ffffffffL;
	}
}
