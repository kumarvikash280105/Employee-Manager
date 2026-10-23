import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Server {

    // Dedicated port for this project (avoiding 8080 and 8085 used by previous projects)
    private static final int DEFAULT_PORT = 8090;
    private static final String WEB_ROOT = ".";
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
    }
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        port = findAvailablePort(port);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new StaticFileHandler(WEB_ROOT));
            server.setExecutor(null); // Default single-thread executor
            server.start();

            System.out.println("=================================================");
            System.out.println("  EmpManage - Java HTTP Local Server Running");
            System.out.println("=================================================");
            System.out.println("  URL:        http://localhost:" + port);
            System.out.println("  Directory:  " + new File(WEB_ROOT).getAbsolutePath());
            System.out.println("  Java:       " + System.getProperty("java.version"));
            System.out.println("  Note:       Ports 8080 & 8085 are reserved for");
            System.out.println("              your other projects.");
            System.out.println("=================================================");
            System.out.println("  Press Ctrl+C to stop the server.\n");

        } catch (IOException e) {
            System.err.println("Failed to start Java server on port " + port + ": " + e.getMessage());
        }
    }
    private static int findAvailablePort(int startingPort) {
        int port = startingPort;
        while (port < startingPort + 100) {
            // Never use 8080 or 8085
            if (port == 8080 || port == 8085) {
                port++;
                continue;
            }
            try (ServerSocket socket = new ServerSocket(port)) {
                return port; // Available
            } catch (IOException e) {
                System.out.println("Notice: Port " + port + " is busy. Trying port " + (port + 1) + "...");
                port++;
            }
        }
        return startingPort;
    }
    static class StaticFileHandler implements HttpHandler {
        private final Path rootPath;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        public StaticFileHandler(String root) {
            this.rootPath = Paths.get(root).toAbsolutePath().normalize();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/") || requestPath.isEmpty()) {
                requestPath = "/index.html";
            }
            Path requestedFile = rootPath.resolve("." + requestPath).normalize();

            if (!requestedFile.startsWith(rootPath)) {
                sendResponse(exchange, 403, "403 Forbidden: Access Denied", "text/plain");
                logRequest(403, exchange.getRequestMethod(), requestPath);
                return;
            }

            File file = requestedFile.toFile();

            if (!file.exists() || file.isDirectory()) {
                String notFoundMsg = "404 Not Found: " + requestPath;
                sendResponse(exchange, 404, notFoundMsg, "text/plain");
                logRequest(404, exchange.getRequestMethod(), requestPath);
                return;
            }
            String mimeType = getMimeType(file.getName());
            boolean isHead = exchange.getRequestMethod().equalsIgnoreCase("HEAD");

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, isHead ? -1 : file.length());

            if (!isHead) {
                try (FileInputStream fis = new FileInputStream(file);
                    OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                exchange.getResponseBody().close();
            }

            logRequest(200, exchange.getRequestMethod(), requestPath);
        }

        private String getMimeType(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
            }
            return "application/octet-stream";
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
            byte[] bytes = responseText.getBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void logRequest(int status, String method, String path) {
            String time = dateFormat.format(new Date());
            System.out.printf("[%s] %s %-6s %s%n", time, status == 200 ? "200 OK" : status + "   ", method, path);
        }
    }
}
