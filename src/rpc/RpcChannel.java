package rpc;

import java.io.ByteArrayInputStream;

import org.zeromq.ZMQ;

import base.Channel;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * RpcChannel is the client-side interface to make a request to the RpcServer
 * 
 * @author paulcao
 *
 */
public class RpcChannel extends Channel {
	
	/**
	 * Constructor
	 * 
	 * @param host rpc server host
	 * @param port rpc server port
	 * @param io_threads number of threads dedicated to zmq sockets
	 */
	public RpcChannel(String host, int port, int io_threads) {
		super(host, port, io_threads);
	}

	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.REQ);
		zmqSocket.connect(address);
	}
	
	/**
	 * RpcChannel's specific implementation of after making a RPC request, waiting for a response back
	 * and decoding the response and passing it to the RPC callback
	 * 
	 * @param response protobuf response
	 * @param done callback handler
	 */
	@Override
	protected void onSend(Message response, RpcCallback<Message> done) {
		// wait for zmq socket to get a response back
		byte[] responseArr = zmqSocket.recv(0);	
		try {
			// decode the binary response to a protobuf object
			ByteArrayInputStream input = new ByteArrayInputStream(responseArr);
			response = response.getParserForType().parseFrom(input);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} finally {
			// if callback handler exists, pass the parsed response to it
			if (done != null) {
				done.run(response);
			}
		}
	}

}
