package rpc;

import org.zeromq.ZMQ;

import base.Listener;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * RpcServer is the server-side implementation of RPC
 * 
 * @author paulcao
 *
 */
public class RpcServer extends Listener {
	/**
	 * Constructor
	 * 
	 * @param host rpc hostname 
	 * @param port rpc port 
	 * @param io_threads number of threads dedicated to zmq sockets
	 */
	public RpcServer(String host, int port, int io_threads) {
		super(host, port, io_threads);
	}
	
	@Override
	protected void initializeSocket(String address) {
		zmqSocket = zmqContext.socket(ZMQ.REP);
		zmqSocket.bind(address);
	}
	
	/**
	 * On callback after receiving and executing a RPC request from the server,
	 * send the result right back to the originating RPC client
	 */
	@Override
	public RpcCallback<Message> callback() {
		return new RpcCallback<Message>() {
			@Override
			public void run(Message returnVal) {
				synchronized (socketBlock) {
					// Send reply back to client and ensure that zmqSocket.send() is synchronized
					zmqSocket.send(returnVal.toByteArray(), 0);
				}
			}
		};
	}
}
