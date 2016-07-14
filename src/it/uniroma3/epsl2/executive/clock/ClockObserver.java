package it.uniroma3.epsl2.executive.clock;

/**
 * 
 * @author anacleto
 *
 */
public interface ClockObserver {

	/**
	 * 
	 * @param tick
	 * @throws InterruptedException
	 */
	public void clockUpdate(long tick) 
			throws InterruptedException;

	/**
	 * 
	 * @throws InterruptedException
	 */
	public void waitReady() 
			throws InterruptedException;
}
