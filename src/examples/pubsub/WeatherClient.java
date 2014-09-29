package examples.pubsub;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import pubsub.Subscriber;
import rpc.RpcChannel;
import messaging.ExampleProto.WeatherService;

/**
 * WeatherClient that extends the Subscriber class that explicitly specifies and registers the WeatherService
 * as the Subscriber's service contract
 * 
 * @author paulcao
 *
 */
public abstract class WeatherClient extends Subscriber {
	
	private WeatherService weatherService;
	
	/**
	 * Constructor
	 * 
	 * @param pubHost publisher streaming hostname
	 * @param pubPort publisher streaming channel
	 * @param rpcHost publisher command channel hostname
	 * @param rpcPort publisher command channel port
	 * @param ioThreads number of threads dedicated to socket
	 */
	public WeatherClient(String pubHost, int pubPort, String rpcHost,
			int rpcPort, int ioThreads) {
		super(pubHost, pubPort, rpcHost, rpcPort, ioThreads);
	}

	public WeatherService getWeatherService() {
		return weatherService;
	}
	
	/**
	 * Bind the publisher command channel with the weather service contract
	 */
	@Override
	public Service reqService(RpcChannel reqChannel) {
		weatherService = WeatherService.newStub(this.reqChannel);
		return weatherService;
	}

}
