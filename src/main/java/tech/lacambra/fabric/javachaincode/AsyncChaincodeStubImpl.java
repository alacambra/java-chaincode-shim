package tech.lacambra.fabric.javachaincode;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import tech.lacambra.fabric.javachaincode.ledger.CompositeKey;
import tech.lacambra.fabric.javachaincode.ledger.KeyModification;
import tech.lacambra.fabric.javachaincode.ledger.KeyValue;
import tech.lacambra.fabric.javachaincode.ledger.QueryResultsIterator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class AsyncChaincodeStubImpl implements AsyncChaincodeStub {

    private static final String UNSPECIFIED_KEY = new String(Character.toChars(0x000001));
    private final String channelId;
    private final String txId;
    private final PeerMessageHandler handler;
    private final List<ByteString> args;
    private final ProposalPackage.SignedProposal signedProposal;
    private final Instant txTimestamp;
    private final ByteString creator;
    private final Map<String, ByteString> transientMap;
    private final byte[] binding;
    private ChaincodeEventPackage.ChaincodeEvent event;

    AsyncChaincodeStubImpl(String channelId, String txId, PeerMessageHandler handler, List<ByteString> args, ProposalPackage.SignedProposal signedProposal) {
        this.channelId = channelId;
        this.txId = txId;
        this.handler = handler;
        this.args = Collections.unmodifiableList(args);
        this.signedProposal = signedProposal;

        if (this.signedProposal == null) {
            this.creator = null;
            this.txTimestamp = null;
            this.transientMap = Collections.emptyMap();
            this.binding = null;
        } else {
            try {
                final ProposalPackage.Proposal proposal = ProposalPackage.Proposal.parseFrom(signedProposal.getProposalBytes());
                final Common.Header header = Common.Header.parseFrom(proposal.getHeader());
                final Common.ChannelHeader channelHeader = Common.ChannelHeader.parseFrom(header.getChannelHeader());
                validateProposalType(channelHeader);
                final Common.SignatureHeader signatureHeader = Common.SignatureHeader.parseFrom(header.getSignatureHeader());
                final ProposalPackage.ChaincodeProposalPayload chaincodeProposalPayload = ProposalPackage.ChaincodeProposalPayload.parseFrom(proposal.getPayload());
                final Timestamp timestamp = channelHeader.getTimestamp();

                this.txTimestamp = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
                this.creator = signatureHeader.getCreator();
                this.transientMap = chaincodeProposalPayload.getTransientMapMap();
                this.binding = computeBinding(channelHeader, signatureHeader);
            } catch (InvalidProtocolBufferException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<byte[]> getArgs() {
        return args.stream().map(ByteString::toByteArray).collect(Collectors.toList());
    }

    @Override
    public List<String> getStringArgs() {
        return args.stream().map(ByteString::toStringUtf8).collect(Collectors.toList());
    }

    @Override
    public String getFunction() {
        if (getStringArgs().isEmpty()) {
            return null;
        } else {
            return getStringArgs().get(0);
        }
    }

    @Override
    public List<String> getParameters() {
        return getStringArgs().stream().skip(1).collect(toList());
    }

    @Override
    public String getTxId() {
        return txId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public CompletableFuture<Response> invokeChaincode(String chaincodeName, List<byte[]> args, String channel) {

        final String compositeName;

        if (channel != null && channel.trim().length() > 0) {
            compositeName = chaincodeName + "/" + channel;
        } else {
            compositeName = chaincodeName;
        }
        return handler.invokeChaincode(this.channelId, this.txId, compositeName, args);
    }

    @Override
    public CompletableFuture<byte[]> getState(String key) {
        return handler.getState(channelId, txId, key).thenApply(ByteString::toByteArray);
    }

    @Override
    public CompletableFuture<Response> putState(String key, byte[] value) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key cannot not be an empty string");
        }
        return handler.putState(channelId, txId, key, ByteString.copyFrom(value));
    }

    @Override
    public CompletableFuture<Response> delState(String key) {
        return handler.deleteState(channelId, txId, key);
    }

    @Override
    public CompletableFuture<QueryResultsIterator<KeyValue>> getStateByRange(String startKey, String endKey) {
        if (startKey == null || startKey.isEmpty()) startKey = UNSPECIFIED_KEY;
        if (endKey == null || endKey.isEmpty()) endKey = UNSPECIFIED_KEY;

        return new QueryResultsIteratorImpl<KeyValue>(this.handler, getChannelId(), getTxId(),
                handler.getStateByRange(getChannelId(), getTxId(), startKey, endKey),
                queryResultBytesToKv.andThen(KeyValueImpl::new)
        );    }

    @Override
    public CompletableFuture<QueryResultsIterator<KeyValue>> getStateByPartialCompositeKey(String compositeKey) {
        return null;
    }

    @Override
    public CompositeKey createCompositeKey(String objectType, String... attributes) {
        return null;
    }

    @Override
    public CompositeKey splitCompositeKey(String compositeKey) {
        return null;
    }

    @Override
    public CompletableFuture<QueryResultsIterator<KeyValue>> getQueryResult(String query) {
        return null;
    }

    @Override
    public CompletableFuture<QueryResultsIterator<KeyModification>> getHistoryForKey(String key) {
        return null;
    }

    @Override
    public void setEvent(String name, byte[] payload) {

    }

    @Override
    public CompletableFuture<Response> invokeChaincode(String chaincodeName, List<byte[]> args) {
        return null;
    }

    @Override
    public CompletableFuture<Response> invokeChaincodeWithStringArgs(String chaincodeName, List<String> args, String channel) {
        return null;
    }

    @Override
    public CompletableFuture<Response> invokeChaincodeWithStringArgs(String chaincodeName, List<String> args) {
        return null;
    }

    @Override
    public CompletableFuture<Response> invokeChaincodeWithStringArgs(String chaincodeName, String... args) {
        return null;
    }

    @Override
    public CompletableFuture<Response> getStringState(String key) {
        return null;
    }

    @Override
    public CompletableFuture<Response> putStringState(String key, String value) {
        return null;
    }

    @Override
    public ChaincodeEventPackage.ChaincodeEvent getEvent() {
        return null;
    }

    @Override
    public ProposalPackage.SignedProposal getSignedProposal() {
        return null;
    }

    @Override
    public Instant getTxTimestamp() {
        return null;
    }

    @Override
    public byte[] getCreator() {
        return new byte[0];
    }

    @Override
    public Map<String, byte[]> getTransient() {
        return null;
    }

    @Override
    public byte[] getBinding() {
        return new byte[0];
    }

    private void validateProposalType(Common.ChannelHeader channelHeader) {
        switch (Common.HeaderType.forNumber(channelHeader.getType())) {
            case ENDORSER_TRANSACTION:
            case CONFIG:
                return;
            default:
                throw new RuntimeException(String.format("Unexpected transaction type: %s", Common.HeaderType.forNumber(channelHeader.getType())));
        }
    }

    private byte[] computeBinding(final Common.ChannelHeader channelHeader, final Common.SignatureHeader signatureHeader) throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(signatureHeader.getNonce().asReadOnlyByteBuffer());
        messageDigest.update(this.creator.asReadOnlyByteBuffer());
        final ByteBuffer epochBytes = ByteBuffer.allocate(Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(channelHeader.getEpoch());
        epochBytes.flip();
        messageDigest.update(epochBytes);
        return messageDigest.digest();
    }
}
