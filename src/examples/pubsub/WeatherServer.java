package examples.pubsub;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import messaging.ExampleProto.MessageString;
import messaging.ExampleProto.MessageWeather;
import messaging.ExampleProto.Void;
import base.Channel;
import base.RequestMessage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;

import pubsub.Publisher;
import messaging.ExampleProto.WeatherSubscriberService;
import messaging.ExampleProto.WeatherService;

/**
 * WeatherServer that extends the Publisher class that explicitly specifies and registers the WeatherService
 * as the pub-sub service contract; broadcast the temperature for all subscribers' requested locations every 1 sec
 * with randomly generated integer between 0-80. 
 * 
 * @author paulcao
 *
 */
public class WeatherServer extends Publisher {
	
	/**
	 * All of the city/locations that subscribers requested weather for
	 */
	Set<String> locations = new CopyOnWriteArraySet<String>();
	
	WeatherSubscriberService weatherSubcriberService;
	
	/**
	 * Constructor 
	 * 
	 * @param reqHost Publisher command channel host
	 * @param reqPort Publisher command channel port
	 * @param pubHost Publisher stream channel host
	 * @param pubPort Publisher stream channel port
	 * @param ioThreads threads dedicated to zmq sockets
	 */
	public WeatherServer(String reqHost, int reqPort, String pubHost,
			int pubPort, int ioThreads) {
		super(reqHost, reqPort, pubHost, pubPort, ioThreads);
	}
	
	/**
	 * Actual WeatherService implementation that handles subscriber's subscription requests
	 */
	@Override
	public Service reqService(Service pubService) {
		WeatherService weatherServiceImpl = new WeatherService() {

			@Override
			public void subscribeBroadcast(RpcController controller,
					MessageString request, RpcCallback<Void> done) {
				// just add the requested city/location to the Set of locations 
				System.out.println("[WeatherPublisher]: adding subscription request for " + request);
				locations.add(request.getMessage());
				
				if (done != null) {
					done.run(Void.newBuilder().build());
				}
			}
		};
		
		return weatherServiceImpl;
	}
	
	/**
	 * Weather Publisher streaming channel
	 */
	@Override
	public Service pubService(Channel pubChannel) {
		if (weatherSubcriberService == null) {
			weatherSubcriberService = WeatherSubscriberService.newStub(pubChannel);
		} 
		
		return weatherSubcriberService;
	}
	
	/**
	 * The repeating timer task that broadcasts the weather to subscribers every second
	 */
	private void broadcastWeather() {
		Random random = new Random();
		
		while (true) {
			for (String location : locations) {	//iterate all requested locations
				// make a random weather number for the current location
				int temperature = random.nextInt(80);
				MessageWeather weatherMessage = MessageWeather.newBuilder().setLocation(location)
													.setTemperature(temperature).build();
				
				// broadcast the weather
				weatherSubcriberService.weatherBroadcast(null, weatherMessage, null);
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Non-blocking start method that kicks off the chronological weather broadcast 
	 */
	@Override
	public void start() {
		super.start();
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				broadcastWeather();
			}
		};
			
		Thread broadcastThread = new Thread(runnable);
		broadcastThread.start();
	}
}
