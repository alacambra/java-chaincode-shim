package tech.lacambra.fabric.javachaincode;

import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;

import java.util.concurrent.CompletableFuture;

public class QueueMessage {

    private final ChaincodeMessage chaincodeMessage;
    private final CompletableFuture<ChaincodeMessage> onResponse;

    public QueueMessage(ChaincodeMessage chaincodeMessage) {
        this.chaincodeMessage = chaincodeMessage;
        this.onResponse = new CompletableFuture<>();
    }

    public ChaincodeMessage getChaincodeMessage() {
        return chaincodeMessage;
    }


    public CompletableFuture<ChaincodeMessage> getOnResponse() {
        return onResponse;
    }

    public String getMsgTxContextId() {
        return chaincodeMessage.getChannelId() + chaincodeMessage.getTxid();
    }

    @Override
    public String toString() {
        return "QueueMessage{" +
                "chaincodeMessage=" + chaincodeMessage +
                ", onResponse=" + onResponse +
                '}';
    }
}
