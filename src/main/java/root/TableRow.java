package root;

public class TableRow {
    private int id;
    private int port;
    private byte[] PK;

    public TableRow(int id, int port, byte[] PK){
        this.id = id;
        this.port = port;
        this.PK = PK;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public byte[] getPK() {
        return PK;
    }
}
