package base;

import com.google.protobuf.Message;
import com.google.protobuf.Service;
import com.google.protobuf.Descriptors.MethodDescriptor;

/**
 * MethodMessage is a wrapper object that contains the method name and also the arguments and return
 * value
 * 
 * @author paulcao
 */
public class RequestMessage {
	public final Service service;
	public final Message request;
	public final Message response;
	public final MethodDescriptor method;

	public RequestMessage(Service service, Message request, Message response, 
		MethodDescriptor method) {
		this.service = service;
		this.request = request;
		this.response = response;
		this.method = method;
	}
}
