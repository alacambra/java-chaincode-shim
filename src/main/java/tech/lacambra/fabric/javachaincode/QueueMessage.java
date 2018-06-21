package tech.lacambra.fabric.javachaincode;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.concurrent.CompletableFuture;

public class QueueMessage {

    private final ChaincodeShim.ChaincodeMessage chaincodeMessage;
    private final CompletableFuture<ChaincodeShim.ChaincodeMessage> onResponse;

    public QueueMessage(ChaincodeShim.ChaincodeMessage chaincodeMessage) {
        this.chaincodeMessage = chaincodeMessage;
        this.onResponse = new CompletableFuture<>();
    }

    public ChaincodeShim.ChaincodeMessage getChaincodeMessage() {
        return chaincodeMessage;
    }


    public CompletableFuture<ChaincodeShim.ChaincodeMessage> getOnResponse() {
        return onResponse;
    }

    public String getMsgTxContextId() {
        return chaincodeMessage.getChannelId() + chaincodeMessage.getTxid();
    }
}
