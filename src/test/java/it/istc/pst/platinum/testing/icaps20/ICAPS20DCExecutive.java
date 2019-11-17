package it.istc.pst.platinum.testing.icaps20;

import it.istc.pst.platinum.executive.dc.DCDispatcher;
import it.istc.pst.platinum.executive.dc.DCExecutive;
import it.istc.pst.platinum.executive.dc.DCMonitor;
import it.istc.pst.platinum.framework.microkernel.annotation.cfg.FrameworkLoggerConfiguration;
import it.istc.pst.platinum.framework.microkernel.annotation.cfg.executive.DispatcherConfiguration;
import it.istc.pst.platinum.framework.microkernel.annotation.cfg.executive.MonitorConfiguration;
import it.istc.pst.platinum.framework.utils.log.FrameworkLoggingLevel;


/**
 * 
 * @author alessandroumbrico
 *
 */
@FrameworkLoggerConfiguration(
		level = FrameworkLoggingLevel.OFF
)
@MonitorConfiguration(
		monitor = DCMonitor.class
)
@DispatcherConfiguration(
		dispatcher = DCDispatcher.class
)
public class ICAPS20DCExecutive extends DCExecutive 
{
	/**
	 * 
	 */
	protected ICAPS20DCExecutive() {
		super();
	}
}	