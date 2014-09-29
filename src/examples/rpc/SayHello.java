package examples.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import rpc.RpcChannel;
import rpc.RpcServer;
import messaging.ExampleProto.HelloService;
import messaging.ExampleProto.MessageString;

/**
 * SayHello example to demonstrate RPC, a HelloClient that makes a request to sayHello(name)
 * to a HelloServer that returns "Hello (name)"
 * 
 * @author paulcao
 *
 */
public class SayHello {

	public static void main(String[] args) {
		// initializes helloServer and implements the HelloService contract, start the server
		RpcServer helloServer = new RpcServer("127.0.0.1", 5555, 1);
		helloServer.registerService(new HelloService() {
			@Override
			public void sayHello(RpcController controller,
					MessageString request, RpcCallback<MessageString> done) {
				System.out.println("[HelloServer]: Received from client: " + request);
				
				// return value of the RPC method = "Hello {name}"
				String returnStr = "Hello " + request.getMessage();
				MessageString returnVal = MessageString.newBuilder().setMessage(returnStr).build();
				
				if (done != null) {
					done.run(returnVal);
				}
			}
		});
		helloServer.startThread();
		
		// initialize the helloClient
		HelloService helloClient =  HelloService.newStub(new RpcChannel("127.0.0.1", 5555, 1));
		
		// make a helloClient's sayHello() request
		MessageString name = MessageString.newBuilder().setMessage("Michael").build();
		helloClient.sayHello(null, name, new RpcCallback<MessageString>() {
			@Override
			public void run(MessageString msg) {
				System.out.println("[HelloClient]: Received from server: " + msg.getMessage());
			}			
		});
	}

}
