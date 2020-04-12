import client.*;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sun.jdi.ByteValue;

/**
* This is just some example code to show you how to interact 
* with the server using the provided client and two queues.
* Feel free to modify this code in any way you like!
*/

public class MyProtocol{

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 13300; //TODO: Set this to your group frequency!

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;
    private int sourceAddress = (int) (Math.random()*99 + 1);
    public Map<Integer,ArrayList<Integer>> routingtable = new LinkedHashMap<Integer,ArrayList<Integer>>();
	/* private byte sourceAddressGetByteVal = */
    public MyProtocol(String server_ip, int server_port, int frequency){
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use

        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!

        // handle sending from stdin from this thread.
        try{
            ByteBuffer temp = ByteBuffer.allocate(1024);
            int read = 0;
            while(true){
                read = System.in.read(temp.array()); // Get data from stdin, hit enter to send!
                if(read > 0){					//+number of additional packet header bytes
                    ByteBuffer toSend = ByteBuffer.allocate(read-2+4); // java includes newlines in System.in.read, so -2 to ignore this
                    toSend.put((byte) (read-2));  //Length of the packet
                    toSend.put((byte) 1); //Fragment ID
                    toSend.put((byte) 0); //More Fragments
                    toSend.put((byte) sourceAddress); //Source Address
                    //toSend.put((byte)); //Destination Address
                    toSend.put( temp.array(), 0, read-2 ); // jave includes newlines in System.in.read, so -2 to ignore this
                    Message msg;
                    if( (read-2) > 2 ){
                        msg = new Message(MessageType.DATA, toSend);
                    } else {
                        msg = new Message(MessageType.DATA_SHORT, toSend);
                    }
                    sendingQueue.put(msg);
                }
            }
        } catch (InterruptedException e){
            System.exit(2);
        } catch (IOException e){
            System.exit(2);
        }        
    }

    public static void main(String args[]) {
        if(args.length > 0){
            frequency = Integer.parseInt(args[0]);
        }
        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);        
    }

    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;
        
        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }
        

        
        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=4; i<bytes.get(0)+4; i++){
                System.out.print( (char)(bytes.get(i) )+"" );
            }
            if(!routingtable.containsKey(sourceAddress)) {
            	routingtable.put(sourceAddress, new ArrayList<Integer>());
            }
            List<Integer> temp = routingtable.get(sourceAddress);
            if(!routingtable.get(sourceAddress).contains((int) bytes.get(3)) && !routingtable.get(sourceAddress).contains(sourceAddress)) {
            	temp.add((int) bytes.get(3));
            	routingtable.put((Integer) sourceAddress, (ArrayList<Integer>) temp);
            }
            System.out.println();
            System.out.println("Packet Source Address: "+ (bytes.get(3)));
            System.out.println("Source Address: "+ sourceAddress);
            System.out.println("Neighbours: "+ routingtable.get(sourceAddress));
            System.out.println();
        }
        
        public void run(){
            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){
                        System.out.println("BUSY");
                    } else if (m.getType() == MessageType.FREE){
                        System.out.println("FREE");
                    } else if (m.getType() == MessageType.DATA){
                        System.out.print("DATA: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DATA_SHORT){
                        System.out.print("DATA_SHORT: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DONE_SENDING){
                        System.out.println("DONE_SENDING");
                    } else if (m.getType() == MessageType.HELLO){
                        System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING){
                        System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END){
                        System.out.println("END");
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }                
            }
        }
    }
}

