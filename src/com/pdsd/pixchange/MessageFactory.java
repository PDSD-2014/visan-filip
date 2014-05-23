package com.pdsd.pixchange;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.SocketException;

public class MessageFactory {
	
	public static void writeString(String string, DataOutput dataOutput) throws IOException{
	    if(string == null){
	      dataOutput.writeInt(0);
	    } else {
	      byte[] bytes = string.getBytes("UTF-8");
	      dataOutput.writeInt(bytes.length);
	      dataOutput.write(bytes);
	    }
	  }

	  public static String readString(DataInput dataInput) throws IOException{
	    int length = dataInput.readInt();
	    if(length > 0){
	      try{
	      byte[] bytes = new byte[length];
	      dataInput.readFully(bytes);
	      return new String(bytes, "UTF-8");
	      } catch(OutOfMemoryError e){
	        e.printStackTrace();
	        return null;
	      }
	    } else {
	      return null;
	    }
	  }
	  
	  public static IMessage receiveMessage(DataInputStream dataInput) {
		  try {
			  if (dataInput != null) {
				  ObjectInputStream input = new ObjectInputStream(dataInput);
				  IMessage message = (IMessage)input.readObject();
				  return message;
			  }
		  } catch (StreamCorruptedException e) {
			  e.printStackTrace();
			  return null; 
		  } catch (SocketException e) {
			  return null; 
		  } catch (IOException e) {
			  return null; 
		  } catch (ClassNotFoundException e) {
			  e.printStackTrace();
			  return null; 
		  }
		  return null;  
	  }
	  
	  public static void sendMessage(DataOutputStream dataOutput, IMessage message) {
		  try {		    
			  if(message != null) {
				  ObjectOutput output = new ObjectOutputStream(dataOutput);
				  output.writeObject(message);
				  output.flush();
			  }
		    
		} catch (IOException e) {
			e.printStackTrace();
		}  
	  }

}
