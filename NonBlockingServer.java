import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingServer {
    private Selector selector; // Selector untuk monitoring event
    private ServerSocketChannel serverSocketChannel; // Channel server untuk menerima koneksi
    private AtomicInteger clientIdCounter; // Counter untuk ID klien
    private ConcurrentHashMap<SocketChannel, Integer> clientIds; // Memetakan SocketChannel ke ID klien

    public NonBlockingServer(int port) {
        try {
            selector = Selector.open(); // Membuka Selector baru
            serverSocketChannel = ServerSocketChannel.open(); // Membuka ServerSocketChannel
            serverSocketChannel.socket().bind(new InetSocketAddress(port)); // Binding serverSocketChannel ke port
            serverSocketChannel.configureBlocking(false); // Konfigurasi channel non-blocking
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // Register serverSocketChannel ke selector untuk menerima koneksi
            clientIdCounter = new AtomicInteger(1); // Inisialisasi counter untuk ID klien
            clientIds = new ConcurrentHashMap<>(); // Inisialisasi map SocketChannel ke ID klien
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        System.out.println("Server Aktif. Menunggu koneksi dari Klien...");

        try {
            while (true) {
                selector.select(); // Memantau event yang sudah siap
                Set<SelectionKey> selectedKeys = selector.selectedKeys(); // Mengambil kunci yang sudah siap
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) { // Jika kunci siap untuk menerima koneksi
                        handleAcceptable(key); // Tangani koneksi yang diterima
                    }
                    if (key.isReadable()) { // Jika kunci siap untuk membaca
                        handleReadable(key); // Tangani data yang diterima
                    }

                    iter.remove(); // Hapus kunci yang sudah ditangani
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAcceptable(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientSocketChannel = serverSocketChannel.accept();
        clientSocketChannel.configureBlocking(false); // Konfigurasi channel agar non-blocking
        clientSocketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024)); // Register channel ke selector untuk membaca data
        int clientId = clientIdCounter.getAndIncrement(); // Dapatkan ID baru untuk klien
        clientIds.put(clientSocketChannel, clientId); // Simpan ID klien ke map
        clientSocketChannel.write(ByteBuffer.wrap(("Anda adalah Klien-" + clientId + "\n").getBytes())); // Kirim pesan selamat datang ke klien
        System.out.println("Klien-" + clientId + " Terhubung: " + clientSocketChannel);
    }

    private void handleReadable(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = clientSocketChannel.read(buffer); // Baca data yang diterima

        if (read == -1) {
            key.cancel();
            clientSocketChannel.close();
            int clientId = clientIds.get(clientSocketChannel);
            System.out.println("Klien-" + clientId + " Terputus");
            return;
        }

        if (read > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String message = new String(bytes);
            System.out.println("Pesan dari klien: " + message);

            if (message.trim().equalsIgnoreCase("exit")) {
                key.cancel();
                clientSocketChannel.close();
                int clientId = clientIds.get(clientSocketChannel);
                System.out.println("Klien-" + clientId + " Terputus");
            } else if (message.trim().equalsIgnoreCase("/server")) {
                int clientId = clientIds.get(clientSocketChannel);
                clientSocketChannel.write(ByteBuffer.wrap(("Halo Klien-" + clientId + "\n").getBytes()));
            } else {
                broadcastMessageToClients("Klien-" + clientIds.get(clientSocketChannel) + ": " + message, clientSocketChannel);
            }
        }
    }

    private void broadcastMessageToClients(String message, SocketChannel senderChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        for (SelectionKey key : selector.keys()) {
            SelectableChannel channel = key.channel();
            if (channel instanceof SocketChannel && channel != senderChannel) {
                SocketChannel clientChannel = (SocketChannel) channel;
                clientChannel.write(buffer);
                buffer.rewind();
            }
        }
    }

    public static void main(String[] args) {
        int port = 8888;
        NonBlockingServer server = new NonBlockingServer(port);
        server.startServer();
    }
}
