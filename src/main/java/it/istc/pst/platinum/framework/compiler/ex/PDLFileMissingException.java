package it.istc.pst.platinum.framework.compiler.ex;

/**
 * 
 * @author anacleto
 *
 */
public class PDLFileMissingException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param msg
	 */
	public PDLFileMissingException(String msg) {
		super(msg);
	}
}