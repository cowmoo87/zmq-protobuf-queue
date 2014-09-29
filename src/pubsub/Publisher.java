package pubsub;

import rpc.RpcServer;
import base.Channel;

import com.google.protobuf.Service;

/**
 * Publisher is the Publisher of pub-sub queue model; and contains both the publisher channel and 
 * the publisher command channel (RPC) that accepts subscriber's requests for new subscriptions
 * 
 * @author paulcao
 *
 */
public abstract class Publisher {
	
	protected Channel pubChannel;
	
	protected RpcServer reqServer;
	
	protected Service pubService;
	
	/**
	 * Constructor
	 * 
	 * @param reqHost Command channel host 
	 * @param reqPort Command channel port
	 * @param pubHost Publisher channel host
	 * @param pubPort Publisher channel port
	 * @param ioThreads number of threads dedicated to the sockets
	 */
	public Publisher(final String reqHost, final int reqPort,
							final String pubHost, final int pubPort, 
							final int ioThreads) {

		pubChannel = new PubChannel(pubHost, pubPort, ioThreads);
		reqServer = new RpcServer(reqHost, reqPort, ioThreads);
		pubService = pubService(pubChannel);
		
		reqServer.registerService(reqService(pubService));
	}
	
	/**
	 * Kick off the publisher command channel listener thread, non-blocking
	 * 
	 */
	public void start() {
		reqServer.startThread();
	}
	
	/**
	 * Hook for publisher to implement the publisher command channel service (e.g., client requesting to subscribe
	 * to a new topic).
	 * 
	 * @param pubService protobuf service contract
	 * @return concrete protobuf service implementation
	 */
	public abstract Service reqService(Service pubService);
	
	/**
	 * Hook for publisher to provide the publisher service contract (e.g., onQuoteTick for stock quotes)
	 * 
	 * @param pubChannel a valid publisher channel with valid zmq publisher socket
	 * @return an associated protobuf service that calls the publisher channel by proxy
	 */
	public abstract Service pubService(Channel pubChannel);
}
