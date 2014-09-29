package base;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * ResponseMessage is a wrapper object that contains the method response type and the callback handler
 * of the method
 * 
 * @author paulcao
 *
 */
public class ResponseMessage {
	public final Message response;
	public final RpcCallback<Message> callback;

	public ResponseMessage(Message response, RpcCallback<Message> callback) {
		this.response = response;
		this.callback = callback;
	}
}
