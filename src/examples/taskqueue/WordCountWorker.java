package examples.taskqueue;

import messaging.ExampleProto.MessageCount;
import messaging.ExampleProto.WordCountService;
import messaging.ExampleProto.MessageString;
import taskqueue.Worker;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * WordCountWorker is a implementation of a worker node that receives Strings
 * and tally up the number of words contained and return back the count
 * 
 * @author paulcao
 *
 */
public class WordCountWorker {
	/**
	 * Constructor
	 * 
	 * @param host master hostname
	 * @param port master port
	 * @param senderHost master task sink hostname
	 * @param senderPort master task sink port
	 * @param io_threads threads dedicated to the zmq socket
	 * @param nodeId id of the worker node
	 */
	public WordCountWorker(String host, int port, 
			String senderHost, int senderPort, int io_threads, int nodeId) {
		
		// initialize the worker node and implement the WordCountService scaffold
		Worker worker = new Worker("127.0.0.1", 5555, "127.0.0.1", 5556, 1);
		worker.registerService(new WordCountService() {
			@Override
			public void wordCount(RpcController controller,
					MessageString request, RpcCallback<MessageCount> done) {
				
				// count the words from the request String
				String[] wordArray = request.getMessage().split("\\s+");
				MessageCount count = MessageCount.newBuilder().setCount(wordArray.length).build();
				System.out.println("Work performed by node " + nodeId);
				
				if (done != null) {
					done.run(count);	// forward the count to the call-back handler and master task result collector
				}
			}
		});
		
		// start the worker node to listen for assigned tasks from the master node
		worker.startThread();
	}
	
}
