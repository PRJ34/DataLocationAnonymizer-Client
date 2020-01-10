import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

public class Client {
    private SocketChannel socketChannel = null;
    private int serverPort;
    private int id;
    private int port;
    private KeyPair kPair;
    HashMap<Integer, Double> pkTable = new HashMap<Integer, Double>();


    public Client(int serverPort, int id){
        this.serverPort = serverPort;
        this.id = id;
        this.port = 1234 + id;
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
                else if(socketChannel.isConnected()){
                    socketChannel.read(buffer);
                    String s = StandardCharsets.UTF_8.decode(buffer).toString();
                    System.out.println("message reçu" + s);
                    if(s == "ok"){
                        this.generateKpair();
                        String payload = this.id+":"+this.port+":"+this.kPair.getPublic().getEncoded();
                        ByteBuffer payload_buffer = ByteBuffer.wrap(payload.getBytes());
                        socketChannel.write(payload_buffer);
                        socketChannel.read(buffer);
                    }
                }

            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public void parseTablePK(String receivedTable){

    }

    public void generateKpair() throws NoSuchAlgorithmException {
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
        this.kPair = aliceKpair;
        /*// Alice creates and initializes her DH KeyAgreement object
        aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();
        return alicePubKeyEnc;*/
    }

    public KeyPair generateKpair_fromSpec(byte[] alicePubKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

        /*
         * Bob gets the DH parameters associated with Alice's public key.
         * He must use the same parameters when he generates his own key
         * pair.
         */
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey)alicePubKey).getParams();

        // Bob creates his own DH key pair
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();
        return bobKpair;

        /*// Bob creates and initializes his DH KeyAgreement object
        System.out.println("BOB: Initialization ...");
        bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());

        // Bob encodes his public key, and sends it over to Alice.
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        return  bobPubKeyEnc;*/
    }

    public KeyAgreement handleReceivedPK(byte [] pubKeyEnc, KeyAgreement keyAgree) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        KeyFactory keyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKeyEnc);
        PublicKey pubKey = keyFac.generatePublic(x509KeySpec);
        keyAgree.doPhase(pubKey, true);
        return keyAgree;
    }


    public byte [] createSecret(KeyAgreement keyAgree){
        /*
         * At this stage, both Alice and Bob have completed the DH key
         * agreement protocol.
         * Both generate the (same) shared secret.
         */
        byte[] sharedSecret = keyAgree.generateSecret();
        return sharedSecret;
    }



    public void initializeForTests(){

    }
}
