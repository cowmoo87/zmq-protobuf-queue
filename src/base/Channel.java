package base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.zeromq.ZMQ;

import utils.Utils;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Descriptors.MethodDescriptor;

/**
 * The base abstract class Channel that implements the Google Protobuf service scaffolding contract
 * uses zeromq as the message transport to transmit the service method name and arguments from client 
 * to server
 * 
 * @author paulcao
 */
public abstract class Channel implements com.google.protobuf.RpcChannel {
	
	protected ZMQ.Context zmqContext;
	protected ZMQ.Socket zmqSocket;
	
	/**
	 * hook for children class to initialize the zeromq socket to a specific type (e.g., RPC, pub-sub, push-pull)
	 * 
	 * @param address a valid ip address
	 */
	protected abstract void initializeSocket(String address);
	
	/**
	 * hook for children class to handle how to respond after the message has been send (e.g., whether wait synchronously
	 * for response or do nothing)
	 * 
	 * @param response return value from the service call
	 * @param done callback handler
	 */
	protected abstract void onSend(Message response, RpcCallback<Message> done);
	
	/**
	 * Constructor for channel
	 * 
	 * @param host valid ip address or host name
	 * @param port valid port
	 * @param io_threads how many threads to allocate for zeromq to handle message transport 
	 */
	public Channel(final String host, final int port, final int io_threads) {
		zmqContext = ZMQ.context(io_threads);
		String address = "tcp://" + host + ":" + port;
		initializeSocket(address);
	}
	
	/**
	 * Hook for writing to binary stream for message transport; protected for children
	 * class to customize binary stream writing  
	 * 
	 * @param outputStream the binary stream object
	 * @param opBytes the method operation code
	 * @param response protobuf service response object
	 * @param request protobuf service request object 
	 * @param done protobuf service callback
	 * @throws IOException
	 */
	protected void writeStream(ByteArrayOutputStream outputStream, byte[] opBytes, 
			Message response, Message request, RpcCallback<Message> done) throws IOException {
		outputStream.write(opBytes);
		outputStream.write(request.toByteArray());
	}
	
	/**
	 * Implementation of transporting protobuf message by hashing the service method descriptor and serializing
	 * the arguments for the call
	 * 
	 * @param methodDescriptor service and method name description
	 * @param controller protobuf controller object
	 * @param request protobuf request arguments
	 * @param response protobuf response object
	 * @param done protobuf async callback handler 
	 */
	public synchronized void callMethod(MethodDescriptor methodDescriptor, RpcController controller,
			Message request, Message response, RpcCallback<Message> done) {
		
		// convert the method descriptor from string to a unique byte-code
		Long hashedCode = Utils.hashString(methodDescriptor.getFullName());
		byte[] opBytes = ByteBuffer.allocate(Long.BYTES).putLong(hashedCode).array();
			
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			// serialize to binary both the method descriptor byte-code and the method's arguments
			writeStream(outputStream, opBytes, response, request, done);
			zmqSocket.send(outputStream.toByteArray(), 0);
			
			// call the children class' specific implementation of what-to-do after sending the request
			onSend(response, done);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}