import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.*;
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
    private ArrayList<int[][]> masks = new ArrayList<>();


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
        ByteBuffer buffer = ByteBuffer.allocate(4000);
        while (true){
            try {
                if(socketChannel == null || !socketChannel.isOpen())
                    socketChannel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
                else if(socketChannel.isConnected()){
                    System.out.println("Connected");
                    while (buffer.remaining() == 4000) {
                        socketChannel.read(buffer);
                    }
                    buffer.flip();
                    CharBuffer firstRead = StandardCharsets.UTF_8.decode(buffer);
                    buffer.clear();
                    System.out.println("message reçu " + firstRead.toString());
                    if(firstRead.toString().contains("start")){
                        String timeFrame = firstRead.toString().split(":")[1];
                        System.out.println(timeFrame);
                        this.generateKpair();
                        String payload_string = this.id+":"+this.port+":";
                        ByteBuffer string_buffer = StandardCharsets.UTF_8.encode(payload_string);
                        ByteBuffer byte_buffer = ByteBuffer.wrap(this.kPair.getPublic().getEncoded());
                        ByteBuffer payload_buffer = ByteBuffer.allocate(4+string_buffer.capacity()+byte_buffer.capacity()).putInt(string_buffer.limit()).put(string_buffer).put(byte_buffer);
                        payload_buffer.flip();
                        System.out.println("send info to server");
                        socketChannel.write(payload_buffer);
                        System.out.println("wait for table from server");
                        while (buffer.remaining() == 4000) {
                            socketChannel.read(buffer);
                        }
                        buffer.flip();
                        this.parseTablePK(buffer);
                        buffer.clear();
                        this.computeSharedSecret();
                        this.computeMask(181,60);
                        for(int[][]m : this.masks){
                            for (int h=0; h<10;h++){
                                for(int w=0; w<10;w++){
                                    System.out.print(m[h][w]+":");
                                }
                            }
                            System.out.println();
                        }
                        socketChannel.close();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }

    private List<ByteBuffer[]> buildHeatmapBuffer(double heatmap[][]) {
        List<ByteBuffer[]> buffers = new ArrayList<>();

        for (int i = 0; i < Heatmap.HEATMAP_ROW; i++) {
            ByteBuffer buffer[] = new ByteBuffer[Heatmap.HEATMAP_COL];
            for (int j = 0; j < Heatmap.HEATMAP_COL; j++) {
                buffer[j] = ByteBuffer.allocate(2048);
                buffer[j].putDouble(heatmap[i][j]);
            }
            buffers.add(buffer);
        }

        return buffers;
    }

    public void sendHeatmap(double heatmap[][]) {
//        List<ByteBuffer[]> buffers = this.buildHeatmapBuffer(heatmap);
//
//        for (ByteBuffer[] buffer : buffers) {
//            try {
//                socketChannel.write(buffer);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        try {
            socketChannel.configureBlocking(true);
            ObjectOutputStream oos = new
                    ObjectOutputStream(socketChannel.socket().getOutputStream());
            oos.writeObject(heatmap);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runPythonScript(String param) throws IOException {
        int number1 = 10;
        int number2 = 32;

        ProcessBuilder pb = new ProcessBuilder("python","create_map.py",""+param);
        Process p = pb.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        int ret = Integer.parseInt(in.readLine());
    }

    public int[][] addMasks(int[][] hm){
        for(int[][] mask : this.masks){
            for(int i = 0; i<hm.length;i++){
                for (int j = 0; j<hm[0].length; j++){
                    hm[i][j] += mask[i][j];
                }
            }
        }
        return hm;
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

    public void computeMask(int height, int width) throws NoSuchAlgorithmException {
        int bound = 5000;
        for(int id : sharedSecrets.keySet()){
            int factor;
            if(id < this.id)
                factor = -1;
            else if(id == this.id)
                factor = 0;
            else
                factor = 1;
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(this.sharedSecrets.get(id));
            int[][] mask = new int[height][width];
            for(int i = 0; i<height;i++){
                for (int j = 0; j<width; j++){
                    mask[i][j] = factor*sr.nextInt(bound);
                }
            }
            this.masks.add(mask);
        }
    }

    public void parseTablePK(ByteBuffer receivedTable){
        ArrayList<ByteBuffer> rows = new ArrayList<>();
        int length;
        byte[] byte_arr;
        while (receivedTable.remaining() != 0){
            length = receivedTable.getInt();
            byte_arr = new byte[length];
            receivedTable.get(byte_arr);
            rows.add(ByteBuffer.wrap(byte_arr));
        }
        byte[] string_buffer;
        byte[] byte_buffer;
        for (ByteBuffer bb : rows){
            length = bb.getInt();
            string_buffer = new byte[length];
            bb.get(string_buffer);
            ByteBuffer string_bb = ByteBuffer.wrap(string_buffer);
            CharBuffer s = StandardCharsets.UTF_8.decode(string_bb);
            byte_buffer = new byte[bb.remaining()];
            bb.get(byte_buffer);
            String [] strSplit = s.toString().split(":");
            this.tablePK.add(new TableRow(Integer.valueOf(strSplit[0]), Integer.valueOf(strSplit[1]), byte_buffer));
        }
    }

    public void generateKpair() throws NoSuchAlgorithmException {
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
        this.kPair = aliceKpair;
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

    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
}
