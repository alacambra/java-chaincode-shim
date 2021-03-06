package tech.lacambra.fabric.javachaincode;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

public class ChatStream implements StreamObserver<ChaincodeMessage> {

    private static final Logger logger = Logger.getLogger(ChatStream.class.getName());

    private static final Deque<QueueMessage> EMPTY_DEQUE;
    private final MsgQueueHandler msgQueueHandler;
    private StreamObserver<ChaincodeMessage> sender;
    private ManagedChannel channel;


    static {
        EMPTY_DEQUE = new LinkedList<>();
    }

    public ChatStream(ManagedChannel channel) {
        this.channel = channel;
        ChaincodeSupportGrpc.ChaincodeSupportStub stub = ChaincodeSupportGrpc.newStub(channel);
        sender = stub.register(this);
        msgQueueHandler = new MsgQueueHandler();
    }

    public CompletableFuture<ByteString> sendMessage(ChaincodeMessage message) {
        return msgQueueHandler.queueMsg(message);
    }

    @Override
    public void onNext(ChaincodeMessage message) {
        logger.info("[onNext] Received message=" + message);
        msgQueueHandler.handleMsgResponse(message);
    }

    @Override
    public void onError(Throwable t) {
        throw new RuntimeException(t);


    }

    @Override
    public void onCompleted() {
        channel.shutdown();
        logger.info("[onCompleted] Done!");

    }

    private class MsgQueueHandler {


        private Map<String, Deque<QueueMessage>> txQueues;

        public MsgQueueHandler() {
            txQueues = new ConcurrentHashMap<>();
        }

        public CompletableFuture<ByteString> queueMsg(ChaincodeMessage message) {
            QueueMessage queueMessage = new QueueMessage(message);
            String txContextId = queueMessage.getMsgTxContextId();
            txQueues.computeIfAbsent(txContextId, key -> new ConcurrentLinkedDeque<>()).addLast(queueMessage);

            CompletableFuture<ByteString> future = queueMessage.getOnResponse().thenApply(ChaincodeMessage::getPayload);

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
                logger.info("[sendMessage] Sending message=" + message.getChaincodeMessage());
                sender.onNext(message.getChaincodeMessage());
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
}
