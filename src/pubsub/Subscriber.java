package pubsub;

import rpc.RpcChannel;

import com.google.protobuf.Service;

/**
 * Subscriber is the subscriber node object of the pub-sub queue model, contains both the subscriber listener
 * that gets the messages from the publisher; and also the subscriber command channel that makes new subscription
 * request to the publisher
 * 
 * @author paulcao
 *
 */
public abstract class Subscriber {
	
	protected SubListener subListener;
	protected RpcChannel reqChannel;
	
	protected Service reqService;
	
	/**
	 * Constructor
	 * 
	 * @param pubHost publisher stream host
	 * @param pubPort publisher port
	 * @param rpcHost publisher command channel host 
	 * @param rpcPort publisher command channel port
	 * @param ioThreads number of threads dedicated to zmq socket
	 */
	public Subscriber(String pubHost, int pubPort,
					  String rpcHost, int rpcPort,
						int ioThreads) {
		
		// initialize the listener module and registers the specified subscriber service implementation
		subListener = new SubListener(pubHost, pubPort, ioThreads);
		subListener.registerService(subService());
		
		// initialize the publisher command channel module and register the specified publisher command channel service
		if (rpcHost != null) {
			reqChannel = new RpcChannel(rpcHost, rpcPort, ioThreads);
			reqService = reqService(reqChannel);
		}
	}
	
	/**
	 * Kick off the listener thread, non-blocking
	 * 
	 */
	public void start() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				subListener.start();
			}
		};
		
		Thread processMsg = new Thread(runnable);
		processMsg.start();
	}
	
	/**
	 * Hook for concrete subscriber to implement the subscriber service (e.g., client requesting to subscribe
	 * to a new topic).
	 * 
	 * @return concrete protobuf service implementation
	 */
	public abstract Service subService();

	/**
	 * Hook for concrete subscriber to implement the publisher command channel service (e.g., client requesting to subscribe
	 * to a new topic).
	 * 
	 * @param reqChannel a valid publisher command channel with valid zmq publisher socket
	 * @return concrete protobuf service implementation
	 */
	public abstract Service reqService(RpcChannel reqChannel);
}
