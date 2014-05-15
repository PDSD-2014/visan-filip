package com.pdsd.pixchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import android.app.Activity;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;


public class UDPListener extends Thread {
	
  protected static final int DISCOVERY_PORT = 9080;
  protected static final int TIMEOUT_MS = 2000;
  protected static final String TAG = "tag";
  protected boolean isRunning = true;
  protected DatagramSocket socket = null;
  private static WifiManager mWifi;

  public UDPListener(WifiManager wifi) {
	  mWifi = wifi;
  }
  
  
  public void run() {
	  while (true) {
		  try {
			socket = new DatagramSocket(DISCOVERY_PORT);
		    socket.setBroadcast(true);
			listenForResponses(socket);
			sendUDPBroadcast();
		  } catch (IOException e) {
			e.printStackTrace();
		  }
	  }
  }

  protected void sendUDPBroadcast() {
	  try {
	      isRunning = true;
	      socket = new DatagramSocket(DISCOVERY_PORT);
	      socket.setBroadcast(true);
	      socket.setSoTimeout(TIMEOUT_MS);

	      sendDiscoveryRequest(socket);
	      listenForResponses(socket);
	      socket.close();
	      Log.d(TAG, "No errors when sending broadcast");
	    } catch (BindException b) {
	    	if (socket != null)
	    		socket.close();
	    } catch (IOException e) {
	    	Log.e(TAG, "Could not send discovery request", e);
	    }
  }
  
  /**
   * Send a broadcast UDP packet
   */
  protected void sendDiscoveryRequest(DatagramSocket socket) throws IOException {
    BroadcastMessage bm = new BroadcastMessage();
    bm.setInfo("received broadcast");
    bm.setAddress(InetAddress.getLocalHost());
    Log.d(TAG, "Sending data " + bm.getMessageType());
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutput out = null;
    out = new ObjectOutputStream(bos);   
    out.writeObject(bm);

    DatagramPacket packet = new DatagramPacket(bos.toByteArray(), bos.size(),
        getBroadcastAddress(), DISCOVERY_PORT);
    socket.send(packet);
    Log.d("Address + port: ", "sending message to address " + getBroadcastAddress().toString() + " and port " + DISCOVERY_PORT);
  }

  /**
   * Listen on socket for responses, timing out after TIMEOUT_MS
   */
  protected void listenForResponses(DatagramSocket socket) throws IOException {
    byte[] buf = new byte[2048];
    try {
      while (isRunning) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        IMessage message = MessageFactory.receiveMessage(new DataInputStream (new ByteArrayInputStream(packet.getData(), 0, packet.getLength())));
        //Log.d(TAG, "Received broadcast message " + ((BroadcastMessage)message).getInfo());
      }
    } catch (SocketTimeoutException e) {
    	Log.d(TAG, "Receive timed out");
    } catch (Exception e) {
    	Log.d(TAG, "Socket closed");
    }
  }
  
  /**
   * Calculate the broadcast IP we need to send the packet along..
   */
  public static InetAddress getBroadcastAddress() throws IOException {
    DhcpInfo dhcp = mWifi.getDhcpInfo();
    if (dhcp == null) {
      Log.d(TAG, "Could not get dhcp info");
      return null;
    }

    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++)
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    
    return InetAddress.getByAddress(quads);
  }
  
  public void setIsRunning(boolean isRunning) {
	  this.isRunning = isRunning;
  }
  
  public void closeSocket() {
	  socket.close();
	  isRunning = false;
  }
  
  public DatagramSocket getSocket() {
	  return socket;
  }
  
}


class BroadcastListener extends UDPListener {
	
	public BroadcastListener(WifiManager wifi) {
		super(wifi);
	}
	
	public void run() {
	    try {
	      socket = new DatagramSocket(DISCOVERY_PORT);
	      socket.setBroadcast(true);
	      listenForResponses(socket);
	    } catch (BindException b) {
	    	if (socket != null)
	    		socket.close();
	    } catch (IOException e) {
	    	Log.e(TAG, "Could not send discovery request", e);
	    }
	}

}

class TCPListener extends Thread {
	
	private static final int LISTENING_PORT = 9081;
	private ServerSocket serverSocket;
	private ArrayList<Socket> sockets;
	private Activity context = null;
	
	public TCPListener(Activity context) {
		sockets = new ArrayList<Socket>();
		this.context = context;
	}
	
	public void run() {
		 try {
			serverSocket = new ServerSocket(LISTENING_PORT);
			while (true) {
				Log.d("Connectivity", "Listening for broadcasts on port " + LISTENING_PORT);
				Socket incomingConnection = serverSocket.accept();
				Log.d("Message", "A message has arrived");
				sockets.add(incomingConnection);
				TCPConnection connection = new TCPConnection(incomingConnection, context);
				connection.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


class TCPConnection extends Thread {
	
	protected final Object sendLock = new Object();
	protected final Object receiveLock = new Object();
	protected DataInputStream dataInput = null;
	protected DataOutputStream dataOutput = null;
	protected Socket socket = null;
	protected boolean isRunning = true;
	protected Activity context= null;
	
	public TCPConnection(Socket socket, Activity context) {
		this.socket = socket;
		this.context = context;
	}
	
	public void init() {
	    try {
	      dataInput = new DataInputStream(socket.getInputStream());
	      dataOutput = new DataOutputStream(socket.getOutputStream());
	    } catch (IOException e) {
	    }
	  }
	
	public void run() {
		while (isRunning) {
			IMessage message = receiveMessage();
			((MainActivity)context).processMessage(message);
		}
	}
	
	public IMessage receiveMessage() {
	      synchronized (receiveLock) {
	           return MessageFactory.receiveMessage(dataInput);
	      }
	  }
	
	public void sendMessage(IMessage message) {
	       synchronized (sendLock) {
	            if (message != null) {
	                if (dataOutput != null) {
	                    MessageFactory.sendMessage(dataOutput, message);
	                }
	            }
	       }
	}
	
	public void setIsRunning (boolean isRunning) {
		this.isRunning = isRunning;
	}
	
	public void closeSocket() {
		isRunning = false;
	    try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

