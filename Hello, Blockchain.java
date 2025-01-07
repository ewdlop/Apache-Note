import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import java.security.Security;
import java.security.SecureRandom;

public class BlockchainCryptoExamples {
    
    static {
        // Register Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate a SHA-256 hash of input data
     */
    public static String sha256Hash(String input) {
        SHA256Digest digest = new SHA256Digest();
        byte[] inputBytes = input.getBytes();
        digest.update(inputBytes, 0, inputBytes.length);
        
        byte[] hashBytes = new byte[digest.getDigestSize()];
        digest.doFinal(hashBytes, 0);
        
        return Hex.toHexString(hashBytes);
    }

    /**
     * Generate a secp256k1 key pair (commonly used in blockchain)
     */
    public static AsymmetricCipherKeyPair generateKeyPair() {
        ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
        SecureRandom random = new SecureRandom();
        
        // Use secp256k1 curve parameters
        ECDomainParameters domainParams = SECNamedCurves.getByName("secp256k1");
        ECKeyGenerationParameters params = 
            new ECKeyGenerationParameters(domainParams, random);
        
        keyGen.init(params);
        return keyGen.generateKeyPair();
    }

    /**
     * Sign a message using ECDSA with the secp256k1 curve
     */
    public static byte[] signMessage(AsymmetricCipherKeyPair keyPair, byte[] message) {
        SHA256Digest digest = new SHA256Digest();
        ECDSASigner signer = new ECDSASigner();
        
        signer.init(true, keyPair.getPrivate());
        
        digest.update(message, 0, message.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        
        BigInteger[] signature = signer.generateSignature(hash);
        
        // Encode signature components (r,s) into DER format
        return encodeDERSignature(signature[0], signature[1]);
    }

    /**
     * Verify an ECDSA signature
     */
    public static boolean verifySignature(
        ECPublicKeyParameters publicKey, 
        byte[] message, 
        byte[] signature
    ) {
        SHA256Digest digest = new SHA256Digest();
        ECDSASigner signer = new ECDSASigner();
        
        signer.init(false, publicKey);
        
        digest.update(message, 0, message.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        
        // Decode DER signature into (r,s) components
        BigInteger[] sigComponents = decodeDERSignature(signature);
        
        return signer.verifySignature(hash, sigComponents[0], sigComponents[1]);
    }

    /**
     * https://brilliant.org/wiki/merkle-tree/
     * Generate a Merkle tree root from a list of transaction hashes
     */
    public static String calculateMerkleRoot(List<String> txHashes) {
        if (txHashes == null || txHashes.isEmpty()) {
            return "";
        }
        
        List<String> tempHashes = new ArrayList<>(txHashes);
        
        while (tempHashes.size() > 1) {
            List<String> newHashes = new ArrayList<>();
            
            for (int i = 0; i < tempHashes.size() - 1; i += 2) {
                String combinedHash = sha256Hash(
                    tempHashes.get(i) + tempHashes.get(i + 1)
                );
                newHashes.add(combinedHash);
            }
            
            // Handle odd number of elements
            if (tempHashes.size() % 2 == 1) {
                String lastHash = tempHashes.get(tempHashes.size() - 1);
                newHashes.add(sha256Hash(lastHash + lastHash));
            }
            
            tempHashes = newHashes;
        }
        
        return tempHashes.get(0);
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        // Generate key pair
        AsymmetricCipherKeyPair keyPair = generateKeyPair();
        
        // Sign a message
        String message = "HHello, Blockchain!";
        byte[] signature = signMessage(keyPair, message.getBytes());
        
        // Verify signature
        boolean isValid = verifySignature(
            (ECPublicKeyParameters)keyPair.getPublic(),
            message.getBytes(),
            signature
        );
        
        System.out.println("Signature valid: " + isValid);
        
        // Calculate Merkle root
        List<String> txHashes = Arrays.asList(
            sha256Hash("tx1"),
            sha256Hash("tx2"),
            sha256Hash("tx3"),
            sha256Hash("tx4")
        );
        
        String merkleRoot = calculateMerkleRoot(txHashes);
        System.out.println("Merkle root: " + merkleRoot);
    }
}
