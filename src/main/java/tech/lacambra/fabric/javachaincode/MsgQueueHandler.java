package tech.lacambra.fabric.javachaincode;

import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MsgQueueHandler {

    private static final Deque<QueueMessage> EMPTY_DEQUE;
    private Map<String, Deque<QueueMessage>> txQueues;
    private StreamObserver<ChaincodeMessage> streamObserver;
    private StreamObserver<ChaincodeMessage> responseObserver;

    static {
        EMPTY_DEQUE = new LinkedList<>();
    }

    public MsgQueueHandler(StreamObserver<ChaincodeMessage> streamObserver) {

        this.streamObserver = streamObserver;
        txQueues = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Response> queueMsg(ChaincodeMessage message) {
        QueueMessage queueMessage = new QueueMessage(message);
        String txContextId = queueMessage.getMsgTxContextId();
        txQueues.computeIfAbsent(txContextId, key -> new ConcurrentLinkedDeque<>()).addLast(queueMessage);

        CompletableFuture<Response> future = queueMessage.getOnResponse().thenApply(m -> new Response());

        if (txQueues.get(txContextId).size() == 1) {
            sendMessage(txContextId);
        }

        return future;
    }

    private QueueMessage getCurrentMessage(String txContextId) {
        QueueMessage message = txQueues.getOrDefault(txContextId, EMPTY_DEQUE).peekFirst();

        if (message == null) {
            //can that happens?
        }

        return message;
    }

    private void sendMessage(String txContextId) {
        QueueMessage message = getCurrentMessage(txContextId);
        if (message != null) {
            streamObserver.onNext(message.getChaincodeMessage());
        }
    }

    /*
     * Handle a response to a message. this looks at the top of
     * the queue for the specific txn id to get the message this
     * response is associated with so it can drive the promise waiting
     * on this message response. it then removes that message from the
     * queue and sends the next message on the queue if there is one.
     *
     * @param {any} response the received response
     */
    public void handleMsgResponse(ChaincodeMessage response) {
        String txId = response.getTxid();
        String channelId = response.getChannelId();
        String txContextId = channelId + txId;
        QueueMessage qMsg = getCurrentMessage(txContextId);
        if (qMsg != null) {
            qMsg.getOnResponse().complete(response);
            removeCurrentAndSendNextMsg(txContextId);
        }
    }

    private void removeCurrentAndSendNextMsg(String txContextId) {
        txQueues.computeIfPresent(txContextId, (txId, mQueue) -> {
            mQueue.pollFirst();
            if (mQueue.isEmpty()) {
                return null;
            }
            return mQueue;
        });

        sendMessage(txContextId);
    }

    private Response parseResponse(PeerMessageHandler handler, ChaincodeMessage response, String method) {
        return null;
    }
}
