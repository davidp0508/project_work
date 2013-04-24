package messagePassing;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.otp.erlang.*;

public class SenderServer implements Runnable {

	@Override
	public void run() {
		IPAddress ip = new IPAddress();
		String myIP = ip.getIPaddress();
		String sender_server = "sender_server"; 
		OtpNode node = null;
		
		
		/* The code below gets the sender_server to run from Java. */
		String dir = "src/"; //where you have your beam file stored in relation to where this code is run
		//String erlName = "$(which erl)"; //doesn't work correctly in this setup, but would be more portable
		String erlName = "/opt/local/bin/erl"; //change this path to reflect where you have Erlang installed
		String options = " +K true +P 500000 -name '"; //must be there
		String server = sender_server+"@"+myIP;
		String cookiePrefix = "' -setcookie ";
		String cookie = "test"; //may change based on what we plan to do (needs to be changed elsewhere in this Project if so)
		String module = " -s message_passing"; //name of Erlang module
		String function = " start "; //name of function to call

		String cmd = "cd src/ ; "+erlName+options+server+cookiePrefix+cookie+module+function+sender_server;
		System.out.println("Command is "+cmd+"\n");
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
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
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