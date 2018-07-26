package tech.lacambra.fabric.javachaincode;

import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import tech.lacambra.fabric.javachaincode.ledger.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AsyncChaincodeStub {

    /**
     * Returns the arguments corresponding to the call to
     * {@link Chaincode#init(AsyncChaincodeStub)} or
     * {@link Chaincode#invoke(AsyncChaincodeStub)}.
     *
     * @return a list of arguments
     */
    List<byte[]> getArgs();

    /**
     * Returns the arguments corresponding to the call to
     * {@link Chaincode#init(AsyncChaincodeStub)} or
     * {@link Chaincode#invoke(AsyncChaincodeStub)}.
     *
     * @return a list of arguments cast to UTF-8 strings
     */
    List<String> getStringArgs();

    /**
     * A convenience method that returns the first argument of the chaincode
     * invocation for use as a function name.
     * <p>
     * The bytes of the first argument are decoded as a UTF-8 string.
     *
     * @return the function name
     */
    String getFunction();

    /**
     * A convenience method that returns all except the first argument of the
     * chaincode invocation for use as the parameters to the function returned
     * by #{@link AsyncChaincodeStub#getFunction()}.
     * <p>
     * The bytes of the arguments are decoded as a UTF-8 strings and returned as
     * a list of string parameters..
     *
     * @return a list of parameters
     */
    List<String> getParameters();

    /**
     * Returns the transaction id
     *
     * @return the transaction id
     */
    String getTxId();

    /**
     * Returns the channel id
     *
     * @return the channel id
     */
    String getChannelId();

    /**
     * Invoke another chaincode using the same transaction context.
     *
     * @param chaincodeName Name of chaincode to be invoked.
     * @param args          Arguments to pass on to the called chaincode.
     * @param channel       If not specified, the caller's channel is assumed.
     * @return
     */
    CompletableFuture<Response> invokeChaincode(String chaincodeName, List<byte[]> args, String channel);

    /**
     * Returns the byte array value specified by the key, from the ledger.
     *
     * @param key name of the value
     * @return value the value read from the ledger
     */
    CompletableFuture<byte[]> getState(String key);

    /**
     * Writes the specified value and key into the ledger
     *
     * @param key   name of the value
     * @param value the value to write to the ledger
     */
    CompletableFuture<Response> putState(String key, byte[] value);

    /**
     * Removes the specified key from the ledger
     *
     * @param key name of the value to be deleted
     */
    CompletableFuture<Response> delState(String key);

    /**
     * Returns all existing keys, and their values, that are lexicographically
     * between <code>startkey</code> (inclusive) and the <code>endKey</code>
     * (exclusive).
     *
     * @param startKey
     * @param endKey
     * @return an {@link Iterable} of {@link KeyValue}
     */
    CompletableFuture<QueryResultsIterator<KeyValue>> getStateByRange(String startKey, String endKey);

    /**
     * Returns all existing keys, and their values, that are prefixed by the
     * specified partial {@link CompositeKey}.
     * <p>
     * If a full composite key is specified, it will not match itself, resulting
     * in no keys being returned.
     *
     * @param compositeKey partial composite key
     * @return an {@link Iterable} of {@link KeyValue}
     */
    CompletableFuture<QueryResultsIterator<KeyValue>> getStateByPartialCompositeKey(String compositeKey);

    /**
     * Given a set of attributes, this method combines these attributes to
     * return a composite key.
     *
     * @param objectType
     * @param attributes
     * @return a composite key
     * @throws CompositeKeyFormatException if any parameter contains either a U+000000 or U+10FFFF code
     *                                     point.
     */
    CompositeKey createCompositeKey(String objectType, String... attributes);

    /**
     * Parses a composite key from a string.
     *
     * @param compositeKey a composite key string
     * @return a composite key
     */
    CompositeKey splitCompositeKey(String compositeKey);

    /**
     * Perform a rich query against the state database.
     *
     * @param query query string in a syntax supported by the underlying state
     *              database
     * @return
     * @throws UnsupportedOperationException if the underlying state database does not support rich
     *                                       queries.
     */
    CompletableFuture<QueryResultsIterator<KeyValue>> getQueryResult(String query);

    /**
     * Returns the history of the specified key's values across time.
     *
     * @param key
     * @return an {@link Iterable} of {@link KeyModification}
     */
    CompletableFuture<QueryResultsIterator<KeyModification>> getHistoryForKey(String key);

    /**
     * Defines the CHAINCODE type event that will be posted to interested
     * clients when the chaincode's result is committed to the ledger.
     *
     * @param name    Name of event. Cannot be null or empty string.
     * @param payload Optional event payload.
     */
    void setEvent(String name, byte[] payload);

    /**
     * Invoke another chaincode using the same transaction context.
     *
     * @param chaincodeName Name of chaincode to be invoked.
     * @param args          Arguments to pass on to the called chaincode.
     * @return
     */
    CompletableFuture<Response> invokeChaincode(String chaincodeName, List<byte[]> args);

    /**
     * Invoke another chaincode using the same transaction context.
     * <p>
     * This is a convenience version of
     * {@link #invokeChaincode(String, List, String)}. The string args will be
     * encoded into as UTF-8 bytes.
     *
     * @param chaincodeName Name of chaincode to be invoked.
     * @param args          Arguments to pass on to the called chaincode.
     * @param channel       If not specified, the caller's channel is assumed.
     * @return
     */
    CompletableFuture<Response> invokeChaincodeWithStringArgs(String chaincodeName, List<String> args, String channel);

    /**
     * Invoke another chaincode using the same transaction context.
     * <p>
     * This is a convenience version of {@link #invokeChaincode(String, List)}.
     * The string args will be encoded into as UTF-8 bytes.
     *
     * @param chaincodeName Name of chaincode to be invoked.
     * @param args          Arguments to pass on to the called chaincode.
     * @return
     */
    CompletableFuture<Response> invokeChaincodeWithStringArgs(String chaincodeName, List<String> args);

    /**
     * Invoke another chaincode using the same transaction context.
     * <p>
     * This is a convenience version of {@link #invokeChaincode(String, List)}.
     * The string args will be encoded into as UTF-8 bytes.
     *
     * @param chaincodeName Name of chaincode to be invoked.
     * @param args          Arguments to pass on to the called chaincode.
     * @return
     */
    CompletableFuture<Response> invokeChaincodeWithStringArgs(final String chaincodeName, final String... args);

    /**
     * Returns the byte array value specified by the key and decoded as a UTF-8
     * encoded string, from the ledger.
     *
     * @param key name of the value
     * @return value the value read from the ledger
     */
    CompletableFuture<Response> getStringState(String key);

    /**
     * Writes the specified value and key into the ledger
     *
     * @param key   name of the value
     * @param value the value to write to the ledger
     */
    CompletableFuture<Response> putStringState(String key, String value);

    /**
     * Returns the CHAINCODE type event that will be posted to interested
     * clients when the chaincode's result is committed to the ledger.
     *
     * @return the chaincode event or null
     */
    ChaincodeEventPackage.ChaincodeEvent getEvent();

    /**
     * Returns the signed transaction proposal currently being executed.
     *
     * @return null if the current transaction is an internal call to a system
     * chaincode.
     */
    ProposalPackage.SignedProposal getSignedProposal();

    /**
     * Returns the timestamp when the transaction was created.
     *
     * @return timestamp as specified in the transaction's channel header.
     */
    Instant getTxTimestamp();

    /**
     * Returns the identity of the agent (or user) submitting the transaction.
     *
     * @return the bytes of the creator field of the proposal's signature
     * header.
     */
    byte[] getCreator();

    /**
     * Returns the transient map associated with the current transaction.
     *
     * @return
     */
    Map<String, byte[]> getTransient();

    /**
     * Returns the transaction binding.
     */
    byte[] getBinding();

}
