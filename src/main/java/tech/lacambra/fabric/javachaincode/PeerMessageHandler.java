package tech.lacambra.fabric.javachaincode;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.*;
import static tech.lacambra.fabric.javachaincode.MessageFactory.newErrorChaincodeResponse;

public class PeerMessageHandler {

    private ChatStream chatStream;

    public CompletableFuture<ByteString> getState(String channelId, String txId, String key) {
        return chatStream.sendMessage(newGetStateEventMessage("", channelId, txId, key));
    }

    public PeerMessageHandler(ChatStream chatStream) {
        this.chatStream = chatStream;
    }

    private CompletableFuture<ByteString> askPeerAndListen(ChaincodeShim.ChaincodeMessage message, String method) {
        return this.chatStream.sendMessage(message);
    }

    private static ChaincodeShim.ChaincodeMessage newGetStateEventMessage(String collection, String channelId, String txId, String key) {

        ChaincodeShim.GetState getStateMessage = ChaincodeShim.GetState
                .newBuilder()
                .setKey(key)
                .setCollection(collection)
                .build();

        return MessageFactory.newEventMessage(GET_STATE, channelId, txId, getStateMessage.toByteString());
    }

    CompletableFuture<Response> putState(String channelId, String txId, String key, ByteString value) {

        if (!isTransaction(channelId, txId)) {
            throw new IllegalStateException("Cannot put state in query context");
        }

        ChaincodeShim.ChaincodeMessage message = MessageFactory.newPutStateEventMessage(channelId, txId, key, value);
        return chatStream.sendMessage(message).thenApply(this::toResponse);
    }

    CompletableFuture<Response> deleteState(String channelId, String txId, String key) {

        if (!isTransaction(channelId, txId)) {
            throw new IllegalStateException("Cannot put state in query context");
        }

        ChaincodeShim.ChaincodeMessage message = MessageFactory.newDeleteStateEventMessage(channelId, txId, key);
        return chatStream.sendMessage(message).thenApply(this::toResponse);
    }

    CompletableFuture<ChaincodeShim.QueryResponse> getStateByRange(String channelId, String txId, String startKey, String endKey) {
        ChaincodeShim.GetStateByRange stateByRange = ChaincodeShim.GetStateByRange.newBuilder()
                .setStartKey(startKey)
                .setEndKey(endKey)
                .build();

        return invokeQueryResponseMessage(channelId, txId, GET_STATE_BY_RANGE, stateByRange.toByteString());
    }

    CompletableFuture<ChaincodeShim.QueryResponse> queryStateNext(String channelId, String txId, String queryId) {
        return invokeQueryResponseMessage(channelId, txId, QUERY_STATE_NEXT, ChaincodeShim.QueryStateNext.newBuilder()
                .setId(queryId)
                .build().toByteString());
    }

    void queryStateClose(String channelId, String txId, String queryId) {
        invokeQueryResponseMessage(channelId, txId, QUERY_STATE_CLOSE, ChaincodeShim.QueryStateClose.newBuilder()
                .setId(queryId)
                .build().toByteString());
    }

    CompletableFuture<ChaincodeShim.QueryResponse> getQueryResult(String channelId, String txId, String query) {
        return invokeQueryResponseMessage(channelId, txId, GET_QUERY_RESULT, ChaincodeShim.GetQueryResult.newBuilder()
                .setQuery(query)
                .build().toByteString());
    }

    CompletableFuture<ChaincodeShim.QueryResponse> getHistoryForKey(String channelId, String txId, String key) {
        return invokeQueryResponseMessage(channelId, txId, ChaincodeShim.ChaincodeMessage.Type.GET_HISTORY_FOR_KEY, ChaincodeShim.GetQueryResult.newBuilder()
                .setQuery(key)
                .build().toByteString());
    }

    private CompletableFuture<ChaincodeShim.QueryResponse> invokeQueryResponseMessage(String channelId, String txId, ChaincodeShim.ChaincodeMessage.Type type, ByteString payload) {

        ChaincodeShim.ChaincodeMessage message = MessageFactory.newEventMessage(type, channelId, txId, payload);

        return chatStream.sendMessage(message).thenApply(bytes -> {
            try {
                return ChaincodeShim.QueryResponse.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        });
    }

    CompletableFuture<Response> invokeChaincode(String channelId, String txId, String chaincodeName, List<byte[]> args) {
        final Chaincode.ChaincodeSpec invocationSpec = org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec.newBuilder()
                .setChaincodeId(org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID.newBuilder()
                        .setName(chaincodeName)
                        .build())
                .setInput(org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput.newBuilder()
                        .addAllArgs(args.stream().map(ByteString::copyFrom).collect(Collectors.toList()))
                        .build())
                .build();

        ChaincodeShim.ChaincodeMessage message = MessageFactory.newInvokeChaincodeMessage(channelId, txId, invocationSpec.toByteString());
        return chatStream.sendMessage(message).thenApply(this::toResponse);
    }

    private Response toResponse(ByteString bytes) {
        return toResponse(parseResponseFrom(bytes));
    }

    private boolean isTransaction(String channelId, String uuid) {
        return true;
    }

    private ChaincodeShim.ChaincodeMessage parseResponseFrom(ByteString payload) {
        try {
            return ChaincodeShim.ChaincodeMessage.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private Response toResponse(ChaincodeShim.ChaincodeMessage m) {
        try {
            if (m.getType() == COMPLETED) {
                // success
                return MessageFactory.toChaincodeResponse(ProposalResponsePackage.Response.parseFrom(m.getPayload()));
            } else {
                // error
                return newErrorChaincodeResponse(m.getPayload().toStringUtf8());
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
