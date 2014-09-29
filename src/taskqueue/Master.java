package taskqueue;

import org.zeromq.ZMQ;

import utils.Utils;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Descriptors.MethodDescriptor;

import base.Channel;
import base.ResponseMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Master implementation of the task queue, responsible for submitting tasks to its
 *  associated worker nodes and for maintaining a task sink that gathers the results
 *  of the work nodes as they complete the work
 *  
 * @author paulcao
 *
 */
public class Master extends Channel {
	
	/**
	 * Task sink that the worker nodes forward the results of the submitted tasks
	 */
	MasterTaskResultCollector taskSink;
	
	/**
	 * Constructor 
	 * 
	 * @param pusherHost master hostname
	 * @param pusherPort master port
	 * @param pullerHost task result collection channel hostname
	 * @param pullerPort task result collection channel port
	 * @param ioThreads number of threads dedicated to zmq sockets
	 */
	public Master(final String pusherHost, final int pusherPort,
			final String pullerHost, final int pullerPort, 
			final int ioThreads) {
		super(pusherHost, pusherPort, ioThreads);
		
		// initialize the task sink and kick off its listening thread
		taskSink = new MasterTaskResultCollector(pullerHost, pullerPort, ioThreads);
		taskSink.startThread();
	}

	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.PUSH);
		zmqSocket.bind(address);
	}

	@Override
	protected void onSend(Message response, RpcCallback<Message> done) {
		// do nothing as task sink is responsible for listening for responses
	}
	
	/**
	 * Overrides the default message serialization by including not only method operation 
	 * code and method , but also a unique requestId for the task; used so that the task
	 * sink can correlate the proper callbacks for the task results when the task is completed
	 * 
	 * @param outputStream binary stream for message serialization
	 * @param opBytes op-code of the service method
	 * @param response protobuf response object
	 * @param request protobuf request object
	 * @param done callback method
	 */
	@Override
	protected void writeStream(ByteArrayOutputStream outputStream, byte[] opBytes, 
			Message response, Message request, RpcCallback<Message> done) throws IOException {
		ResponseMessage responseMessage = new ResponseMessage(response, done);
		Long requestId = taskSink.registerCallback(responseMessage);
		byte[] requestIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(requestId).array();
		
		outputStream.write(opBytes);
		outputStream.write(requestIdBytes);
		outputStream.write(request.toByteArray());
	}
}
