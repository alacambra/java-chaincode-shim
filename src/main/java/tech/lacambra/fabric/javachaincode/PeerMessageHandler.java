package tech.lacambra.fabric.javachaincode;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.concurrent.CompletableFuture;

import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_STATE;

public class PeerMessageHandler {

    private ChatStream chatStream;

    public CompletableFuture<Response> getState(String channelId, String txId, String key) {
        return chatStream.sendMessage(newGetStateEventMessage("", channelId, txId, key));
    }

    public PeerMessageHandler(ChatStream chatStream) {
        this.chatStream = chatStream;
    }

    private CompletableFuture<Response> askPeerAndListen(ChaincodeShim.ChaincodeMessage message, String method) {
        return this.chatStream.sendMessage(message);
    }

    private static ChaincodeShim.ChaincodeMessage newGetStateEventMessage(String collection, String channelId, String txId, String key) {

        ChaincodeShim.GetState getStateMessage = ChaincodeShim.GetState
                .newBuilder()
                .setKey(key)
                .setCollection(collection)
                .build();

        return newEventMessage(GET_STATE, channelId, txId, getStateMessage.toByteString());
    }

    private static ChaincodeShim.ChaincodeMessage newEventMessage(final ChaincodeShim.ChaincodeMessage.Type type, final String channelId, final String txId, final ByteString payload) {
        return newEventMessage(type, channelId, txId, payload, null);
    }

    private static ChaincodeShim.ChaincodeMessage newEventMessage(final ChaincodeShim.ChaincodeMessage.Type type, final String channelId, final String txId, final ByteString payload, final ChaincodeEventPackage.ChaincodeEvent event) {
        if (event == null) {
            return ChaincodeShim.ChaincodeMessage.newBuilder()
                    .setType(type)
                    .setChannelId(channelId)
                    .setTxid(txId)
                    .setPayload(payload)
                    .build();
        } else {
            return ChaincodeShim.ChaincodeMessage.newBuilder()
                    .setType(type)
                    .setChannelId(channelId)
                    .setTxid(txId)
                    .setPayload(payload)
                    .setChaincodeEvent(event)
                    .build();
        }
    }
}
