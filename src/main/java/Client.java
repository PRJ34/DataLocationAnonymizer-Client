import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    private SocketChannel socketChannel;

    public Client(int serverPort){
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress("localhost", serverPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        ByteBuffer buffer = ByteBuffer.allocate(256);
        while (true){
            try {
                socketChannel.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
