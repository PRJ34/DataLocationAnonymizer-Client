import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

public class Client {
    private SocketChannel socketChannel = null;
    private int serverPort;
    private int id;
    HashMap<Integer, Double> pkTable = new HashMap<Integer, Double>();

    public Client(int serverPort, int id){
        this.serverPort = serverPort;
        this.id = id;
        this.start();
/*        try {
            socketChannel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void start(){
        ByteBuffer buffer = ByteBuffer.allocate(256);
        while (true){
            try {
                if(socketChannel == null)
                    socketChannel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
                socketChannel.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte [] generatePK() throws NoSuchAlgorithmException, InvalidKeyException {
        System.out.println("Generate DH public key ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Alice creates and initializes her DH KeyAgreement object
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();
        return alicePubKeyEnc;
    }

    public byte [] generatePK_fromSpec(X509EncodedKeySpec x509KeySpec) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");

        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

        /*
         * Bob gets the DH parameters associated with Alice's public key.
         * He must use the same parameters when he generates his own key
         * pair.
         */
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey)alicePubKey).getParams();

        // Bob creates his own DH key pair
        System.out.println("BOB: Generate DH keypair ...");
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        // Bob creates and initializes his DH KeyAgreement object
        System.out.println("BOB: Initialization ...");
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());

        // Bob encodes his public key, and sends it over to Alice.
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        return  bobPubKeyEnc;
    }


    public void initializeForTests(){

    }
}
