package us.sosia.video.stream.agent.messaging;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class IPAddress {

	public String getIPaddress()
	{
		String ip = null;
		
		Enumeration e = null;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        while(e.hasMoreElements())
        {
            NetworkInterface n=(NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while(ee.hasMoreElements())
            {
            	InetAddress i= (InetAddress) ee.nextElement();
            	if (! (i instanceof Inet6Address)) //we only deal with IPv4 in our app
            	{
            		//InetAddress i= (InetAddress) ee.nextElement();
            		if(!i.getHostAddress().startsWith("127"))
            		{
            			//System.out.println(i.getHostAddress());
            			ip = i.getHostAddress(); //will need to do more checks to try to get 128 vs. 192 IP
            		}
            	}
            }
        }
		return ip;
	}
}
