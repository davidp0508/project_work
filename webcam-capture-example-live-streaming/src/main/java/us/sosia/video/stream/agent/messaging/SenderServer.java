package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import us.sosia.video.stream.agent.utilities.SystemCommandExecutor;

import com.ericsson.otp.erlang.OtpNode;

public class SenderServer implements Runnable {
	private final String serverName;
	
	public SenderServer(String serverName) {
		this.serverName = serverName;//including the playerId
	}

	@Override
	public void run() {
		IPAddress ip = new IPAddress();
		String myIP = ip.getIPaddress();
		OtpNode node = null;
		
		/* The code below gets the sender_server to run from Java. */
		//TODO belows are hardcode...
		String dir = "src/"; //where you have your beam file stored in relation to where this code is run
		//String erlName = "$(which erl)"; //doesn't work correctly in this setup, but would be more portable
		String erlName = "/usr/local/bin/erl"; //change this path to reflect where you have Erlang installed
		String options = " +K true +P 500000 -name '"; //must be there
		String server = serverName +"@"+myIP;
		String cookiePrefix = "' -setcookie ";
		String cookie = "test"; //may change based on what we plan to do (needs to be changed elsewhere in this Project if so)
		String module = " -s message_passing"; //name of Erlang module
		String function = " start "; //name of function to call
		//TODO problem here
		String cmd = "cd src/main/java/us/sosia/video/stream/agent/messaging ; "+erlName+options+server+cookiePrefix+cookie+module+function+serverName; //TODO concat playerid
//		System.out.println("Command is "+cmd+"\n");
		// build the system command we want to run
	    List<String> commands = new ArrayList<String>();
	    commands.add("/bin/bash");
	    commands.add("-c");
	    commands.add(cmd);

	    // execute the command
	    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
	    int result=0;
		try {
			result = commandExecutor.executeCommand();
			System.out.println("exe cmd " + cmd);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

	    // get the stdout and stderr from the command that was run
	    StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
	    StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();
	    
	    // print the stdout and stderr
	    System.out.println("The numeric result of the command was: " + result);
	    System.out.println("STDOUT:");
	    System.out.println(stdout);
	    System.out.println("STDERR:");
	    System.out.println(stderr);
	}
}