package com.pdsd.pixchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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

import com.pdsd.pixchange.PhotoService.Photo;

import android.app.Service;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;


public class UDPListener extends Thread {
	
  protected static final int DISCOVERY_PORT = 9080;
  protected static final int TIMEOUT_MS = 2000;
  protected static final String TAG = "tag";
  protected static final String MESSAGE = "message";
  protected boolean isRunning = true;
  protected DatagramSocket socket = null;
  protected static WifiManager mWifi;
  protected HashMap<String, InetAddress> discoveredDevices = new HashMap<String, InetAddress>();
  protected Service context;

  public UDPListener(Service context, WifiManager wifi) {
	  mWifi = wifi;
	  this.context = context;
  }
  
  
  public void run() {
  }
  
  /**
   * Send a broadcast UDP packet
   */
  protected void sendDiscoveryRequest(DatagramSocket socket, String photoName) throws IOException {
    BroadcastMessage bm = new BroadcastMessage();
    bm.setInfo("New Photo");
    int ip = mWifi.getConnectionInfo().getIpAddress();
    String ipAddress = Formatter.formatIpAddress(ip);
    bm.setIPAddress(InetAddress.getByName(ipAddress));
    bm.setMACAddress(mWifi.getConnectionInfo().getMacAddress());
    bm.setPhotoName(photoName);
    
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
	  broadcastReplyMessage.setPhotoName(message.getPhotoName());
	  try {
		  int ip = mWifi.getConnectionInfo().getIpAddress();
		  String ipAddress = Formatter.formatIpAddress(ip);
		  broadcastReplyMessage.setIPAddress(InetAddress.getByName(ipAddress));
	    
		  //send reply through TCP -> more reliable than UDP
		  ((PhotoService) context).getTCPListener().createTCPConnection(message.getIPAddress(), broadcastReplyMessage);
	    
	} catch (UnknownHostException e) {
		e.printStackTrace();
	}	  	
  }
  
  public void processBroadcastReplyMessage(BroadcastReplyMessage message) {
	  discoveredDevices.put(message.getMACAddress(), message.getIPAddress());
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
	
	public BroadcastListener(Service context, WifiManager wifi) {
		super(context, wifi);
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
	protected HashMap<String, Photo> photos;
	protected String storageFolder;
	public static final Object lock = new Object();
	private static WifiManager mWifi;
	
	public TCPListener(Service context, WifiManager wifi) {
		photos = new HashMap<String, Photo>();
		sockets = new ArrayList<Socket>();
		setMWifi(wifi);
	}
	
	public void run() {
		 try {
			serverSocket = new ServerSocket(LISTENING_PORT);
			Log.d("Connectivity", "Listening for tcp connections on port " + LISTENING_PORT);
			while (true) {
				Socket incomingConnection = serverSocket.accept();
				Log.d("Message", "A device is trying to connect");
				sockets.add(incomingConnection);
				TCPConnection connection = new TCPConnection(incomingConnection, this);
				connection.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createTCPConnection(InetAddress ipAddress, BroadcastReplyMessage broadcastReplyMessage) {
		Socket tcpSocket = null;
		try {
			tcpSocket = new Socket(ipAddress, TCPListener.LISTENING_PORT);
			TCPConnection tcpConnection = new TCPConnection(tcpSocket, this);
			sockets.add(tcpSocket);
			tcpConnection.start();
			Log.d("Address + port: ", "sending message to address " + ipAddress.toString() + " and port " + TCPListener.LISTENING_PORT);
			tcpConnection.sendMessage(broadcastReplyMessage);
		} catch (IOException e) {
		}
	}
	
	public void closeSocket() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HashMap<String, Photo> getPhotos() {
		return this.photos;
	}
	
	public ArrayList<Socket> getSockets() {
		return sockets;
	}
	
	public void addPhoto(Photo photo) {
		synchronized (lock) {
			photos.put(photo.file.getName(), photo);
		}
	}
	
	public void setStorageFolder(String storageFolder) {
		this.storageFolder = storageFolder;
	}
	
	public String getStorageFolder() {
		return this.storageFolder;
	}

	public WifiManager getMWifi() {
		return mWifi;
	}

	public void setMWifi(WifiManager mWifi) {
		TCPListener.mWifi = mWifi;
	}
}


class TCPConnection extends Thread {
	
	protected final Object sendLock = new Object();
	protected final Object receiveLock = new Object();
	protected DataInputStream dataInput = null;
	protected DataOutputStream dataOutput = null;
	protected Socket socket = null;
	protected boolean isRunning = true;
	protected TCPListener context= null;
	
	public TCPConnection(Socket socket, TCPListener context) {
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
			if (message == null) {
				isRunning = false;
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				processMessage(message);
			}
		}
	}
	
	public void processMessage(IMessage message) {
		if (message.getMessageType() == IMessageTypes.BROADCAST_MESSAGE) {
			BroadcastMessage bm = (BroadcastMessage)message;
			Log.d("Message", "Broadcast from " + bm.getIPAddress());
		}
		if (message.getMessageType() == IMessageTypes.BROADCAST_REPLY_MESSAGE) {
			BroadcastReplyMessage bm = (BroadcastReplyMessage)message;
			Log.d("Message", "Receied message from " + bm.getIPAddress());
			
			//send image
		    FileInputStream fis = null;
		    try {
		        fis = new FileInputStream(context.getPhotos().get(bm.getPhotoName()).file);
		    } catch (FileNotFoundException e) {
		    	Log.d("Image","Could not find image.");
		        e.printStackTrace();
		    }
		    Bitmap bitmap = BitmapFactory.decodeStream(fis);
		    byte[] imgbyte = getBytesFromBitmap(bitmap);
		    ImageMessage image = new ImageMessage();
		    image.setPhoto(imgbyte);
		    image.setPhotoName(bm.getPhotoName());
		    int ip = context.getMWifi().getConnectionInfo().getIpAddress();
		    String ipAddress = Formatter.formatIpAddress(ip);
		    try {
				image.setIPAddress(InetAddress.getByName(ipAddress));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		    Log.d("Image", "Sending image to " + bm.getIPAddress());
		    sendMessage(image); 
		}
		if (message.getMessageType() == IMessageTypes.IMAGE_MESSAGE) {
			ImageMessage image = (ImageMessage)message;
			Log.d("Image", "An image has been received from " + image.getIPAddress());
			try {
				File newFile = new File(context.getStorageFolder() + "/" + image.getPhotoName());
				FileOutputStream fos = new FileOutputStream(newFile);
				Log.d("Image", "Writing image to sdCard");
				Bitmap bmp=BitmapFactory.decodeByteArray(image.getPhoto(),0,image.getPhoto().length);
				bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				isRunning = false;
				socket.close();
				for (int i = 0 ; i < context.getSockets().size(); i++) {
					if (context.getSockets().get(i) == socket) {
						context.getSockets().remove(i);
						break;
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				if (context == null) {
					Log.d("Context", "context is null");
				}
			} catch (IOException e) {
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

