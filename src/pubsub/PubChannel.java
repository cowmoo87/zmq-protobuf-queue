package pubsub;

import org.zeromq.ZMQ;

import base.Channel;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * Publisher's streaming channel that handles a Publisher node's ability to broadcast to subscribers
 * the messages
 * 
 * @author paulcao
 *
 */
public class PubChannel extends Channel {
	
	/**
	 * Constructor 
	 * 
	 * @param host	valid ip host
	 * @param port	valid ip host
	 * @param io_threads	number of threads dedicated to associated zmq socket 	
	 */
	public PubChannel(String host, int port, int io_threads) {
		super(host, port, io_threads);
	}

	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.PUB);
		zmqSocket.bind(address);
	}

	@Override
	protected void onSend(Message response, RpcCallback<Message> done) {
		// do nothing, pub channel only pushes messages downstream, and need not receive any acknowledgements from clients
	}

}
