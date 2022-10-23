package Client;



import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Client {

    private static int maxDataSize = 2048;
    private final int serverPort;
    private final InetAddress serverIP;
    private SocketChannel sc;
    private InitializedFile file;

    public Client(int serverPort, InetAddress serverIP) throws IOException {
        this.serverPort = serverPort;
        this.serverIP = serverIP;
        this.sc = connect();
    }

    public SocketChannel connect() throws IOException {
        SocketChannel sc = SocketChannel.open();
        //blocking call
        sc.connect(new InetSocketAddress(serverIP,serverPort));
        return sc;
    }


    public String service(String[] command) throws IOException {

        char c = 's';
        byte[] b = new byte[1];
        b[0] = (byte) c;
        ByteBuffer buffer = ByteBuffer.wrap(b);

        switch (command[0]){
            case "i":
                buffer = initialize(command);
                save();
                return "File set, ready to upload";
            case "u":
                upload(command);
                return "Upload sent";
            case "d":
                download(command);
                return "Download comment sent";
            default:
                System.out.println("no valid command");
        }


        sc.write(buffer);
        sc.read(buffer);
        buffer.flip();
        System.out.println("Message from the server: " + (char)buffer.get());
        buffer.rewind();
        sc.shutdownOutput();
        sc.close();
        return "Default Return";
    }

    private void download(String[] command) throws IOException {
        String instruction = "d" + command[1];
        byte[] b = new byte[2048];
        b = instruction.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(b);
        sc.write(buffer);
        sc.read(buffer);
        buffer.flip();
        System.out.println("Message from the server: " + (char)buffer.get());
        if ((char)buffer.get() == 'y'){
            buffer.rewind();
            sc.read(buffer);
        }

        sc.shutdownOutput();
        sc.close();
    }

    private ByteBuffer initialize(String[] command){
        File file = new File(command[1]);
        this.file = new InitializedFile(file.getName(), file);
        char c = 'i';
        byte[] b = new byte[1];
        b[0] = (byte) c;
        return ByteBuffer.wrap(b);
    }

    private void initialize(String fileName){
        this.file = new InitializedFile(fileName);
    }

    private void upload(String[] command) throws IOException {
        String instruction = "u" + file.name + " " + file.content;
        byte[] b = new byte[2048];
        b = instruction.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(b);
        sc.write(buffer);
        sc.read(buffer);
        buffer.flip();
        System.out.println("Message from the server: " + (char)buffer.get());
        if ((char)buffer.get() == 'y'){
            buffer.flip();
            b = file.content.getBytes();
            buffer = ByteBuffer.wrap(b);
            sc.write(buffer);
        }

        sc.shutdownOutput();
        sc.close();
    }

    private void save() throws IOException {
        final String content = this.file.content;
        final Path path = Paths.get(String.format("%s", this.file.name));

        try (
                final BufferedWriter writer = Files.newBufferedWriter(path,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        ) {
            writer.write(content);
            writer.flush();
        }
    }
}
