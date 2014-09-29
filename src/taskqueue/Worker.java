package taskqueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.zeromq.ZMQ;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;

import base.Listener;
import base.RequestMessage;

/**
 * Worker node implementation of the master-worker task queue, responsible for listening for
 * assigned work from the master node, calling the protobuf service implementation that do the work
 * encode the work result and forward it to the master task result sink/collector
 * 
 * @author paulcao
 *
 */
public class Worker extends Listener {

	private ZMQ.Socket senderSocket;
	
	private Boolean senderSocketBlock = new Boolean(false);
	
	/**
	 * Constructor
	 * 
	 * @param host master hostname
	 * @param port master host port 
	 * @param senderHost task sink hostname
	 * @param senderPort task sink port 
	 * @param io_threads number of threads dedicated to zmq sockets
	 */
	public Worker(String host, int port, 
			String senderHost, int senderPort, int io_threads) {
		// initialize the listener thread to listen to tasks
		super(host, port, io_threads);
		
		// initialize the task sink thread to forward the task results
		String senderAddress = "tcp://" + senderHost + ":" + senderPort;
		initializeSenderSocket(senderAddress);
	}
	
	protected void initializeSenderSocket(String senderAddress) {
		senderSocket = zmqContext.socket(ZMQ.PUSH);
		senderSocket.connect(senderAddress);
	}
	
	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.PULL);	
		zmqSocket.connect(address);
	}
	
	/**
	 * Overrides the callback of the listener socket after work has been done to forward the result of word to master
	 * task sink
	 * 
	 * @param requestId the request id of the submitted task
	 * @return the callback handler that encodes the request id and result of the submitted task after the task has been done
	 */
	protected RpcCallback<Message> callback(Long requestId) {
		RpcCallback<Message> sendTaskResult = new RpcCallback<Message>() {
			@Override
			public void run(Message msg) {
				byte[] requestIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(requestId).array();				
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				
				// serialize the message with the task id and task results
				try {
					outputStream.write(requestIdBytes);
					outputStream.write(msg.toByteArray());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// forward the result to task sink in synchronized fashion
				synchronized(senderSocketBlock) {
					senderSocket.send(outputStream.toByteArray(), 0);
				}
			}
		};
		
		return sendTaskResult;
	}
	
	
	/**
	 * The overriden listener thread for worker node to listen and decode task requests
	 * as they come in from the master node; perform the work via protobuf service proxy and 
	 * forward the result back to master node's task sink/collector
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
			// a task message contains the method op code, task id and the task arguments in that order
			ByteBuffer byteBuffer = ByteBuffer.wrap(requestArr);
			Long opCode = byteBuffer.getLong(0);
			Long requestId = byteBuffer.getLong(Long.BYTES);
			RequestMessage methodMessage = rpcMethodMap.get(opCode);
			
			if (methodMessage == null)	// invalid message as the service descriptor cannot be found, skip to next message
				continue;
			
			MethodDescriptor method = methodMessage.method;
			Message request = methodMessage.request;
			Service service = methodMessage.service;
						
			try {
				// parse the request parameters and call the registered service implementation with the method and parameters
				request = request.getParserForType().parseFrom(requestArr, Long.BYTES * 2, requestArr.length - Long.BYTES * 2);
				service.callMethod(method, null, request, callback(requestId));	// pass the specific callback hook
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected RpcCallback<Message> callback() {
		// do nothing as we override the default listener implementation, this method is redudant for this case
		return null;
	}
}
