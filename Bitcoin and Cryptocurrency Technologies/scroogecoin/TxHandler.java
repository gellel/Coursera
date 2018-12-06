import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    public UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        UTXOPool UTXOSet = new UTXOPool();

        double previous = 0;
        
        double current = 0;
        
        for (int i = 0; i < tx.numInputs(); i++) {
            
            // collect the transaction input instance, 
            // using the current iteration as reference index.
            Transaction.Input input = tx.getInput(i);

            // construct a new UTXO instance using the collected
            // inputs previous transaction hash and output index.
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            
            // confirm whether class instance utxoPool does not
            // contain a current reference to the new UTXO.
            // perform the same operation for the unique set, but
            // in this instance confirm a match.
            // if either booleans return true exit early.
            if (!this.utxoPool.contains(utxo) || UTXOSet.contains(utxo)) {
                return false;
            }

            // collect the transaction output instance using the
            // new UTXO instance from this classes utxoPool instance.
            Transaction.Output output = this.utxoPool.getTxOutput(utxo);
            
            // attempt to verify that the hash signatures match for the tranasction public address
            // using the transaction argument raw data and the public signature. 
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }
            
            // prevent double claim for the current UTXO but storing this
            // new copy to the unique set of UTXO's.
            UTXOSet.addUTXO(utxo, output);

            // update the sum of all previous transactions
            // using the output value double.
            previous = previous + output.value;
        }
        // interate across the final store of transactions
        // to confirm that the sum of output values did
        // not reach a negative double.
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }
            current = (current + output.value);
        }

        // confirm that after this was verified that the check sum value.
        // of the previous transactions is still valid.
        return previous >= current;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        // create a set of (to be) unique transactions
        // subsetted from the range of possible transactions.
        Set<Transaction> verified = new HashSet<>();

        for (Transaction tx : possibleTxs) {
            // call our own validate transaction method
            // that was set up.check the signatures and
            // add our new transaction to the Set.
            if (this.isValidTx(tx)) {

                verified.add(tx);

                // iterate across the collected inputs that were
                // generated and stored.
                for (Transaction.Input input : tx.getInputs()) {
                    // remove the previous hash that was cloned during the
                    // instantiation of the transaction handler. we assume that the
                    // items we are removing were processed successfully and need new hashes.
                    this.utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }
                // iterate over the collect outputs
                for (int i = 0; i < tx.numOutputs(); i++) {
                    // use the store updated hashset using the UTXO generator.
                    this.utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                }
            }
        }
        // convert the Set to an Array of Transactions
        return verified.toArray(new Transaction[verified.size()]);   
    }
}
