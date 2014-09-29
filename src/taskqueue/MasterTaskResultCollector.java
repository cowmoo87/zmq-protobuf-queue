package taskqueue;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.zeromq.ZMQ;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;

import base.Listener;
import base.RequestMessage;
import base.ResponseMessage;

/**
 * MasterTaskResultCollector is responsible for listening for forwarded results
 * of work from worker nodes after the work has been completed
 * 
 * @author paulcao
 *
 */
public class MasterTaskResultCollector extends Listener {
	
	/**
	 * Current request id, used to keep an unique identifier for the task
	 */
	AtomicLong requestId = new AtomicLong();
	
	/**
	 * A map of submitted tasks, their calls and their request id; used to invoke
	 * the task's callback after the result of the task is received by the master
	 * from the worker nodes
	 */
	Map<Long, ResponseMessage> resultCallBacks = new HashMap<Long, ResponseMessage>();
	
	/**
	 * Constructor
	 * 
	 * @param host task sink hostname
	 * @param port task sink port 
	 * @param io_threads number of threads dedicated to zmq sockets
	 */
	public MasterTaskResultCollector(String host, int port, int io_threads) {
		super(host, port, io_threads);
	}
	
	/**
	 * Method that registers a task just before it's sent out to the worker nodes
	 * so that we can refer to it after the work nodes forward its results back later on
	 * 
	 * @param responseMessage the wrapper of the submitted work
	 * @return the request id associated with the task
	 */
	public Long registerCallback(ResponseMessage responseMessage) {
		Long currentId = requestId.incrementAndGet();
		resultCallBacks.put(currentId, responseMessage);
		return currentId;
	}
	
	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.PULL);	
		zmqSocket.bind(address);
	}
	
	/**
	 * The overriden listener thread for task sink to listen and decode task results
	 * as they come in from the worker nodes; decode the result and pass it to the task's
	 * registered callback handler
	 * 
	 */
	@Override
	public void start() {
		while (!Thread.currentThread().isInterrupted()) {
			
			// get the top request from the zeromq listening socket
			byte[] requestArr;
			synchronized (socketBlock) {
				requestArr = zmqSocket.recv(0);
			}
			
			// parse corresponding method/service descriptor from its hashed value
			Long opCode = ByteBuffer.wrap(requestArr).getLong();
			ResponseMessage responseMessage = resultCallBacks.get(opCode);
			
			if (responseMessage == null)	// invalid message as the service descriptor cannot be found, skip to next message
				continue;
			
			Message response = responseMessage.response;
			RpcCallback<Message> callback = responseMessage.callback;
						
			try {
				// parse the request parameters and call the registered service implementation with the method and parameters
				response = response.getParserForType().parseFrom(requestArr, Long.BYTES, requestArr.length - Long.BYTES);
				callback.run(response); // pass the results to that task's original callback		
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
