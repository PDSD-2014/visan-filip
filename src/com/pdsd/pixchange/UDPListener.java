package com.pdsd.pixchange;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;


public class UDPListener extends Thread {
	
  protected static final int DISCOVERY_PORT = 9080;
  protected static final int TIMEOUT_MS = 2000;
  protected static final String TAG = "tag";
  protected static final String MESSAGE = "message";
  protected boolean isRunning = true;
  protected DatagramSocket socket = null;
  protected static WifiManager mWifi;
  protected HashMap<String, InetAddress> discoveredDevices = new HashMap<String, InetAddress>();

  public UDPListener(WifiManager wifi) {
	  mWifi = wifi;
  }
  
  
  public void run() {
	  while (true) {
		  try {
			socket = new DatagramSocket(DISCOVERY_PORT);
		    socket.setBroadcast(true);
		    socket.setSoTimeout(TIMEOUT_MS);
		    
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
	      //listenForResponses(socket);
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
    bm.setInfo("Request to send picture");
    
    int ip = mWifi.getConnectionInfo().getIpAddress();
    String ipAddress = Formatter.formatIpAddress(ip);
    bm.setIPAddress(InetAddress.getByName(ipAddress));
    bm.setMACAddress(mWifi.getConnectionInfo().getMacAddress());
    //bm.setPictureName(InetAddress.getLocalHost().hashCode() + "Pic001");
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutput out = null;
    out = new ObjectOutputStream(bos);   
    out.writeObject(bm);

    DatagramPacket packet = new DatagramPacket(bos.toByteArray(), bos.size(), getBroadcastAddress(), DISCOVERY_PORT);
    socket.send(packet);
    Log.d("Address + port: ", "sending message to address " + getBroadcastAddress().toString() + " and port " + DISCOVERY_PORT);
  }

  /**
   * Listen on socket for responses, timing out after TIMEOUT_MS
   */
  protected void listenForBroadcast(DatagramSocket socket) throws IOException {
    byte[] buf = new byte[2048];
    Log.d("Connectivity", "Listening for broadcasts");
    try {
      while (isRunning) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        IMessage message = MessageFactory.receiveMessage(new DataInputStream (new ByteArrayInputStream(packet.getData(), 0, packet.getLength())));
        int messageType = message.getMessageType();
        switch (messageType) {
        	case (IMessageTypes.BROADCAST_MESSAGE):
        		processBroadcastMessage((BroadcastMessage)message);
        		break;
        	case (IMessageTypes.BROADCAST_REPLY_MESSAGE):
        		processBroadcastReplyMessage((BroadcastReplyMessage)message);
        		break;
        	default:
        		Log.d(MESSAGE, "Message of type " + messageType + " has been dumped");
        		break;
        }
      }
    } catch (SocketTimeoutException e) {
    	Log.d(TAG, "Receive timed out. Close socket and start listening for Broadcasts.");
    	if (socket != null) {
    		socket.close();
    	}
    	new UDPListener(mWifi).start();
    	sendPicture();
    } catch (IOException e) {
    	Log.d("Exception", e.getMessage());
    }
    
  }
  
  public void processBroadcastMessage(BroadcastMessage message) {
	  WifiInfo info = mWifi.getConnectionInfo();
	  String address = info.getMacAddress();
	  if (address.equals(message.getMACAddress())) {
		  Log.d("Message", "Duplicate message. Discarded.");
		  return;
	  }
	  BroadcastReplyMessage broadcastReplyMessage = new BroadcastReplyMessage();
	  broadcastReplyMessage.setMACAddress(address);
	  try {
		  int ip = mWifi.getConnectionInfo().getIpAddress();
		  String ipAddress = Formatter.formatIpAddress(ip);
		  broadcastReplyMessage.setIPAddress(InetAddress.getByName(ipAddress));
	    
	    //send reply through TCP -> more reliable than UDP
	    Socket tcpSocket = new Socket(message.getIPAddress(), TCPListener.LISTENING_PORT);
	    TCPConnection tcpConnection = new TCPConnection(tcpSocket, null);
	    tcpConnection.start();
	    Log.d("Address + port: ", "sending message to address " + message.getIPAddress().toString() + " and port " + TCPListener.LISTENING_PORT);
	    tcpConnection.sendMessage(broadcastReplyMessage);
	    
	    
	} catch (UnknownHostException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
	  	
  }
  
  public void processBroadcastReplyMessage(BroadcastReplyMessage message) {
	  discoveredDevices.put(message.getMACAddress(), message.getIPAddress());
  }
  
  public void sendPicture() {
	  // TODO algorithm for sending between multiple devices
	  
	  //for now send picture to every device.
	  for (String macAddress : discoveredDevices.keySet()) {
		  try {
			Socket tcpSocket = new Socket(discoveredDevices.get(macAddress), TCPListener.LISTENING_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	  }  
  }
  
  /**
   * Calculate the broadcast IP we need to send the packet along
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
	      listenForBroadcast(socket);
	    } catch (BindException b) {
	    	if (socket != null)
	    		socket.close();
	    } catch (IOException e) {
	    	Log.e(TAG, "Could not send discovery request", e);
	    }
	}

}

class TCPListener extends Thread {
	
	protected static final int LISTENING_PORT = 9081;
	protected ServerSocket serverSocket;
	protected ArrayList<Socket> sockets;
	protected Activity context = null;
	
	public TCPListener(Activity context) {
		sockets = new ArrayList<Socket>();
		this.context = context;
	}
	
	public void run() {
		 try {
			serverSocket = new ServerSocket(LISTENING_PORT);
			Log.d("Connectivity", "Listening for tcp connections on port " + LISTENING_PORT);
			while (true) {
				Socket incomingConnection = serverSocket.accept();
				Log.d("Message", "A device is trying to connect");
				sockets.add(incomingConnection);
				TCPConnection connection = new TCPConnection(incomingConnection, context);
				connection.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeSocket() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Socket> getSockets() {
		return sockets;
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
		try {
			dataInput = new DataInputStream(socket.getInputStream());
			dataOutput = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
	
	public void run() {
		while (isRunning) {
			IMessage message = receiveMessage();
			Log.d("Message", "Received message ");
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (message != null)
				processMessage(message);
		}
		
	}
	
	public void processMessage(IMessage message) {
		if (message.getMessageType() == IMessageTypes.BROADCAST_MESSAGE) {
			BroadcastMessage bm = (BroadcastMessage)message;
			Log.d("Message", android.os.Build.MODEL + " " + bm.getInfo());
		}
		if (message.getMessageType() == IMessageTypes.BROADCAST_REPLY_MESSAGE) {
			BroadcastReplyMessage bm = (BroadcastReplyMessage)message;
			Log.d("Message", "Receied message from " + bm.getIPAddress());
			
			//send image
		    
		    String filepath = "/sdcard/DCIM/Camera/IMG053.jpg";
		    File imagefile = new File(filepath);
		    FileInputStream fis = null;
		    try {
		        fis = new FileInputStream(imagefile);
		    } catch (FileNotFoundException e) {
		    	Log.d("Image","Could not find image.");
		        e.printStackTrace();
		    }
		    Bitmap bitmap = BitmapFactory.decodeStream(fis);
		    byte[] imgbyte = getBytesFromBitmap(bitmap);
		    ImageMessage image = new ImageMessage();
		    image.setImage(imgbyte);
		    image.setImageName("IMG053_REMOTE.jpg");
		    Log.d("Image", "Sending first image");
		    sendMessage(image); 
		}
		if (message.getMessageType() == IMessageTypes.IMAGE_MESSAGE) {
			Log.d("Image", "An image has arrived.");
			ImageMessage image = (ImageMessage)message;
			Log.d("Image", "ImageMessage was successfully read.");
			try {
				File newFile = new File("/storage/extSdCard/DCIM/Camera/" + image.getImageName());
				FileOutputStream fos = new FileOutputStream("/storage/extSdCard/DCIM/Camera/" + image.getImageName());
				Log.d("Image", "Writing image to file");
				Bitmap bmp=BitmapFactory.decodeByteArray(image.getImage(),0,image.getImage().length);
				bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		
	}
	
	public byte[] getBytesFromBitmap(Bitmap bitmap) {
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    bitmap.compress(CompressFormat.JPEG, 70, stream);
	    return stream.toByteArray();
	}
	
	public IMessage receiveMessage() {
	      synchronized (receiveLock) {
	    	  //Log.d("Message", "Receiving message");
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
		Log.d("Socket", "Closing sockets");
		isRunning = false;
	    try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

