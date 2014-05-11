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
			  ObjectInputStream input = new ObjectInputStream(dataInput);
			  IMessage message = (IMessage)input.readObject();
			  return message;
		  } catch (StreamCorruptedException e) {
			  e.printStackTrace();
		  } catch (IOException e) {
			  e.printStackTrace();
		  } catch (ClassNotFoundException e) {
			  e.printStackTrace();
		  }
		  return null;  
	  }
	  
	  public static void sendMessage(DataOutputStream dataOutput, IMessage message) {
		  try {
			ObjectOutput output = new ObjectOutputStream(dataOutput);
			output.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}  
	  }

}
