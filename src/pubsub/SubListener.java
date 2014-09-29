package pubsub;

import org.zeromq.ZMQ;

import base.Listener;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * SubListener implements the subscriber's listening to its corresponding publisher 
 * for streaming messages
 * 
 * @author paulcao
 *
 */
public class SubListener extends Listener {
	
	/**
	 * Constructor
	 * 
	 * @param host subscriber hostname
	 * @param port subscriber port
	 * @param io_threads number of threads dedicated to zmq sockets
	 */
	public SubListener(String host, int port, int io_threads) {
		super(host, port, io_threads);
	}

	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.SUB);	
		zmqSocket.connect(address);
		
		// don't do any filtering on messages received from the publisher
		String filter = "";
		zmqSocket.subscribe(filter.getBytes());
	}

	@Override
	protected RpcCallback<Message> callback() {
		// no callbacks, as there is no need for publisher to subscriber is a one way communication stream
		return null;
	}
}
