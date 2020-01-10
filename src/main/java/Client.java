import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Client {
    private SocketChannel socketChannel = null;
    private int serverPort;
    private int id;
    private int port;
    private KeyPair kPair;
    private ArrayList<TableRow> tablePK = new ArrayList<>();
    private HashMap<Integer, byte[]> sharedSecrets = new HashMap<>();


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
                    System.out.println("connected");
                    while (buffer.remaining() == 256) {
                        socketChannel.read(buffer);
                    }
                    buffer.flip();
                    //String s = new String(buffer.array(), "UTF-8");
                    CharBuffer s = StandardCharsets.UTF_8.decode(buffer);
                    buffer.clear();
                    System.out.println("message reçu " + s.toString());
                    if(s.toString().contains("ok")){
                        this.generateKpair();
                        String payload = this.id+":"+this.port+":"+this.kPair.getPublic().getEncoded();
                        ByteBuffer payload_buffer = StandardCharsets.UTF_8.encode(payload);
                        System.out.println("send");
                        socketChannel.write(payload_buffer);
                        while (buffer.remaining() == 256) {
                            socketChannel.read(buffer);
                        }
                        buffer.flip();
                        s = StandardCharsets.UTF_8.decode(buffer);
                        this.parseTablePK(s.toString());
                        buffer.clear();
                        System.out.println(s.toString());
                        socketChannel.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public void computeSharedSecret() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        for(TableRow row : this.tablePK){
            KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");;
            aliceKeyAgree.init(this.kPair.getPrivate());
            byte[] bobPK = row.getPK();
            aliceKeyAgree = this.handleReceivedPK(bobPK, aliceKeyAgree);
            byte[] aliceSecret = this.createSecret(aliceKeyAgree);
            this.sharedSecrets.put(row.getId(), aliceSecret);
        }
    }

    public void computeMask(int height, int width){
        //SecureRandom secureRandom1 = SecureRandom.getInstance("SHA1PRNG");
        //secureRandom1.setSeed(aliceSecret);
        //secureRandom1.nextBytes(randomBytes);
    }

    public void parseTablePK(String receivedTable){
        String [] rows = receivedTable.split(";");
        for (int i = 0; i<rows.length; i++){
            String [] row = rows[i].split(":");
            this.tablePK.add(new TableRow(Integer.valueOf(row[0]), Integer.valueOf(row[1]), row[2].getBytes()));
        }
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

    public KeyPair getkPair() {
        return kPair;
    }
}
