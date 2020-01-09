import javax.crypto.KeyAgreement;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class Main {
    public static void main (String[] args){
        Client alice = new Client(1234, 1);
        Client bob = new Client(1234, 1);

        try {
            KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");;
            KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
            KeyPair aliceKpair = alice.generateKpair();
            aliceKeyAgree.init(aliceKpair.getPrivate());
            byte[] alicePK = aliceKpair.getPublic().getEncoded();
            KeyPair bobKpair = bob.generateKpair_fromSpec(alicePK);
            bobKeyAgree.init(bobKpair.getPrivate());
            byte[] bobPK = bobKpair.getPublic().getEncoded();
            aliceKeyAgree = alice.handleReceivedPK(bobPK, aliceKeyAgree);
            bobKeyAgree = bob.handleReceivedPK(alicePK, bobKeyAgree);
            byte[] aliceSecret = alice.createSecret(aliceKeyAgree);
            byte[] bobSecret = bob.createSecret(bobKeyAgree);
            System.out.println("Alice secret: " +
                    toHexString(aliceSecret));
            System.out.println("Bob secret: " +
                    toHexString(bobSecret));
            if (!java.util.Arrays.equals(aliceSecret, bobSecret))
                throw new Exception("Shared secrets differ");
            System.out.println("Shared secrets are the same");
            byte[] randomBytes = new byte[128];
            SecureRandom secureRandom1 = SecureRandom.getInstance("SHA1PRNG");
            secureRandom1.setSeed(aliceSecret);
            secureRandom1.nextBytes(randomBytes);
            System.out.println(toHexString(randomBytes));
            SecureRandom secureRandom2 = SecureRandom.getInstance("SHA1PRNG");
            secureRandom2.setSeed(bobSecret);
            secureRandom2.nextBytes(randomBytes);
            System.out.println(toHexString(randomBytes));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

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