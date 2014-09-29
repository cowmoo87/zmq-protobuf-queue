package examples.pubsub;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import messaging.ExampleProto.MessageString;
import messaging.ExampleProto.MessageWeather;
import messaging.ExampleProto.Void;
import messaging.ExampleProto.WeatherSubscriberService;

/**
 * Weather broadcast example to demonstrate pub-sub, a WeatherPublisher that broadcasts weather
 * and a WeatherSubscriber that subscribes to the weather reports
 * 
 * @author paulcao
 *
 */
public class WeatherMain {
	public static void main(String[] args) {
		// initialize and start weather publisher
		WeatherServer weatherServer = new WeatherServer("127.0.0.1", 5555, "127.0.0.1", 5556, 1);
		weatherServer.start();
		
		// initialize the weather subscriber and start the listening
		WeatherClient weatherClient = new WeatherClient("127.0.0.1", 5556, "127.0.0.1", 5555, 1) {
			@Override
			public Service subService() {
				// actual handler to handle the publisher's streaming msg, just print it to screen
				WeatherSubscriberService subscriberImpl = new WeatherSubscriberService() {
					@Override
					public void weatherBroadcast(RpcController controller,
							MessageWeather request, RpcCallback<Void> done) {
						System.out.println("[WeatherSubscriber]:" + request);
					}
				};
				return subscriberImpl;
			}
		};
		weatherClient.start();
		
		// request from weather client a weather subscription for Boston 
		weatherClient.getWeatherService().subscribeBroadcast(null, 
				MessageString.newBuilder().setMessage("Boston").build(), 
				new RpcCallback<Void>() {
					@Override
					public void run(Void arg) {
						// do nothing
					}
				});
	}
}
