import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NonBlockingClient {
    public static void main(String[] args) {
        // Informasi server yang akan dihubungi
        String serverAddress = "localhost";
        int port = 8888;

        try {
            // Membuat socket channel dan menghubungkannya ke server
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(serverAddress, port));
            socketChannel.configureBlocking(false); // Konfigurasi channel agar non-blocking
            ByteBuffer buffer = ByteBuffer.allocate(1024); // Buffer untuk membaca dan menulis pesan

            // Thread untuk membaca pesan dari server
            new Thread(() -> {
                try {
                    while (true) {
                        buffer.clear();
                        int bytesRead = socketChannel.read(buffer); // Membaca pesan dari server ke dalam buffer

                        if (bytesRead > 0) {
                            buffer.flip();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            String message = new String(bytes);
                            System.out.println(message.trim()); // Menampilkan pesan dari server
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Terputus dari server"); // Pesan jika terputus dari server
                }
            }).start();

            Scanner scanner = new Scanner(System.in);
            String message;
            while (true) {
                message = scanner.nextLine();
                socketChannel.write(ByteBuffer.wrap(message.getBytes())); // Mengirim pesan ke server

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            socketChannel.close(); // Menutup koneksi saat selesai
        } catch (IOException e) {
            System.out.println("Tidak dapat terhubung ke server"); // Pesan jika tidak dapat terhubung ke server
        }
    }
}
