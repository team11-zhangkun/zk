package it.istc.pst.platinum.deliberative.app.cli;

/**
 * 
 * @author alessandroumbrico
 *
 */
public enum PlatinumDeliberativeCommandLineCommand 
{
	/**
	 * help command
	 */
	HELP("?", ""),
	
	/**
	 * initialization command
	 */
	INITIALIZE("init", "init file.ddl file.pdl"),
	
	/**
	 * plan command
	 */
	PLAN("plan", "plan Component.timeline.predicate stLb-stUb,etLb-etUb,dLb-dUb par1-n-val,par2-n-val,...,par3-e-val1|val2|..|valN,...,parM-n-val"),
	
	/**
	 * get all or single timeline temporal projections 
	 */
	GET("get", "get all/Component.timeline"),
	
	/**
	 * show domain information
	 */
	SHOW("show", "show components"),
	
	/**
	 * 
	 */
	DISPLAY("display", "display current plan"),
	
	/**
	 * 
	 */
	EXPORT("export", "export <file-path> to export the last created plan into the file"),
	
	/**
	 * exit command
	 */
	EXIT("exit", "");
	
	private String cmd;
	private String usage;
	
	/**
	 * 
	 * @param cmd
	 * @param usage
	 */
	private PlatinumDeliberativeCommandLineCommand(String cmd, String usage) {
		this.cmd = cmd;
		this.usage = usage;
	}
	
	public String getCmd() {
		return cmd.toUpperCase();
	}
	
	public String getHelp() {
		return usage;
	}
}
