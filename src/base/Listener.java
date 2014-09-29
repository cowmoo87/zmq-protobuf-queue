package base;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZMQ;

import rpc.RpcServer;
import utils.Utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

/**
 * The base abstract class Listener that implements the server-side of Google Protobuf service scaffolding contract
 * uses zeromq as the message transport to listen to clients' requests, calls the service implementation with 
 * deserialized arguments and transmit back to client the returned value
 * 
 * @author paulcao
 */
public abstract class Listener {
	/**
	 * Map of the service method signatures and their corresponding compact binary op-code
	 */
	protected Map<Long, RequestMessage> rpcMethodMap = new HashMap<Long, RequestMessage>();
	
	protected ZMQ.Context zmqContext;
	protected ZMQ.Socket zmqSocket;
	
	/**
	 * Synchronization object used to ensure that zeromq socket object's recv() and send() methods are synchronized
	 */
	protected Boolean socketBlock = new Boolean(false);
	
	/**
	 * hook for children class to initialize the zeromq socket to a specific type (e.g., RPC, pub-sub, push-pull)
	 * 
	 * @param address a valid ip address
	 */
	protected abstract void initializeSocket(String address);
	
	/**
	 * Constructor for channel
	 * 
	 * @param host valid ip address or host name
	 * @param port valid port
	 * @param io_threads how many threads to allocate for zeromq to handle message transport 
	 */
	public Listener(final String host, final int port, final int io_threads) {
		zmqContext = ZMQ.context(io_threads);
		String address = "tcp://" + host + ":" + port;
		initializeSocket(address);
	}
	
	/**
	 * Registers a Google protobuf service into the listener; iterates through every service methods
	 * and registers the method signature and its binary operation code in the listener map 
	 * 
	 * @param service protobuf service implementation
	 */
	public void registerService(Service service) {
		
		for (MethodDescriptor methodDescriptor : service.getDescriptorForType().getMethods()) {
			Message request = service.getRequestPrototype(methodDescriptor);
			Message response = service.getResponsePrototype(methodDescriptor);
			RequestMessage methodMessage = new RequestMessage(service, request, response, methodDescriptor);
			
			// put the the method's binary operation code in map
			Long opCode = Utils.hashString(methodDescriptor.getFullName());
			rpcMethodMap.put(opCode, methodMessage);
		}
	}
	
	/**
	 * Hook for children class to implement callback after the server finishes processing request
	 * 
	 * @return a callback handler
	 */
	protected abstract RpcCallback<Message> callback();
	
	/**
	 * Starts the listener thread, which queues off incoming requests via zeromq socket, calls the method
	 * from registered service and calls the appropriate callback method with the returned value
	 * 
	 */
	public void start() {
		while (!Thread.currentThread().isInterrupted()) {
			
			// get the top request from the zeromq listening socket
			byte[] requestArr;
			synchronized (socketBlock) {
				requestArr = zmqSocket.recv(0);
			}
			
			// parse corresponding method/service descriptor from its hashed value
			Long opCode = ByteBuffer.wrap(requestArr).getLong();
			RequestMessage methodMessage = rpcMethodMap.get(opCode);
			
			if (methodMessage == null)	// invalid message as the service descriptor cannot be found, skip to next message
				continue;
			
			MethodDescriptor method = methodMessage.method;
			Message request = methodMessage.request;
			Service service = methodMessage.service;
						
			try {
				// parse the request parameters and call the registered service implementation with the method and parameters
				request = request.getParserForType().parseFrom(requestArr, Long.BYTES, requestArr.length - Long.BYTES);
				service.callMethod(method, null, request, callback());	// pass the specific callback hook
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Non-blocking start method that kicks off the listener thread
	 */
	public void startThread() {
		Listener self = this;
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				self.start();
			}
		};
			
		Thread processMsg = new Thread(runnable);
		processMsg.start();
	}
	
}
