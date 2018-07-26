package tech.lacambra.fabric.javachaincode;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.*;

public class MessageFactory {
    public static Response newErrorChaincodeResponse(String message) {
        return new Response(Response.Status.INTERNAL_SERVER_ERROR, message, null);
    }

    public static ChaincodeShim.ChaincodeMessage newGetStateEventMessage(final String channelId, final String txId, final String key) {
        return newEventMessage(GET_STATE, channelId, txId, ChaincodeShim.GetState.newBuilder()
                .setKey(key)
                .setCollection("")
                .build().toByteString());
    }

    public static ChaincodeShim.ChaincodeMessage newPutStateEventMessage(final String channelId, final String txId, final String key, final ByteString value) {
        return newEventMessage(PUT_STATE, channelId, txId, ChaincodeShim.PutState.newBuilder()
                .setKey(key)
                .setValue(value)
                .build().toByteString());
    }

    public static ChaincodeShim.ChaincodeMessage newDeleteStateEventMessage(final String channelId, final String txId, final String key) {
        return newEventMessage(
                DEL_STATE,
                channelId,
                txId,
                ChaincodeShim.DelState.newBuilder()
                        .setCollection("")
                        .setKey(key)
                        .build()
                        .toByteString()
        );
    }

    public static ChaincodeShim.ChaincodeMessage newErrorEventMessage(final String channelId, final String txId, final Throwable throwable) {
        return newErrorEventMessage(channelId, txId, printStackTrace(throwable));
    }

    public static ChaincodeShim.ChaincodeMessage newErrorEventMessage(final String channelId, final String txId, final String message) {
        return newErrorEventMessage(channelId, txId, message, null);
    }

    public static ChaincodeShim.ChaincodeMessage newCompletedEventMessage(final String channelId, final String txId, final Response response,
                                                                          final ChaincodeEventPackage.ChaincodeEvent event) {
        return newEventMessage(COMPLETED, channelId, txId, toProtoResponse(response).toByteString(), event);
    }

    public static ChaincodeShim.ChaincodeMessage newInvokeChaincodeMessage(final String channelId, final String txId, final ByteString payload) {
        return newEventMessage(INVOKE_CHAINCODE, channelId, txId, payload, null);
    }

    public static ChaincodeShim.ChaincodeMessage newErrorEventMessage(final String channelId, final String txId, final String message,
                                                                      final ChaincodeEventPackage.ChaincodeEvent event) {
        return newEventMessage(ERROR, channelId, txId, ByteString.copyFromUtf8(message), event);
    }

    public static ChaincodeShim.ChaincodeMessage newEventMessage(final ChaincodeShim.ChaincodeMessage.Type type, final String channelId, final String txId,
                                                                 final ByteString payload) {
        return newEventMessage(type, channelId, txId, payload, null);
    }

    public static ChaincodeShim.ChaincodeMessage newEventMessage(final ChaincodeShim.ChaincodeMessage.Type type, final String channelId, final String txId,
                                                                 final ByteString payload, final ChaincodeEventPackage.ChaincodeEvent event) {
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

    private static ProposalResponsePackage.Response toProtoResponse(Response response) {
        final ProposalResponsePackage.Response.Builder builder = ProposalResponsePackage.Response.newBuilder();
        builder.setStatus(response.getStatus().getCode());
        if (response.getMessage() != null) builder.setMessage(response.getMessage());
        if (response.getPayload() != null) builder.setPayload(ByteString.copyFrom(response.getPayload()));
        return builder.build();
    }

    public static Response toChaincodeResponse(ProposalResponsePackage.Response response) {
        return new Response(
                Response.Status.forCode(response.getStatus()),
                response.getMessage(),
                response.getPayload() == null ? null : response.getPayload().toByteArray()
        );
    }

    private static String printStackTrace(Throwable throwable) {
        if (throwable == null) return null;
        final StringWriter buffer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }
}
