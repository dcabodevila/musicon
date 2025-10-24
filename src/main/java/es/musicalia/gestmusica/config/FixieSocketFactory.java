package es.musicalia.gestmusica.config;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.*;

public class FixieSocketFactory extends SocketFactory {
    
    private static String staticProxyHost;
    private static int staticProxyPort;
    private static String staticProxyUser;
    private static String staticProxyPassword;
    private static boolean initialized = false;
    
    // Constructor sin parámetros requerido por MariaDB
    public FixieSocketFactory() {
        if (!initialized) {
            // Obtener valores de propiedades del sistema si no están inicializados
            initializeFromSystemProperties();
        }
    }
    
    public static void initialize(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        staticProxyHost = proxyHost;
        staticProxyPort = proxyPort;
        staticProxyUser = proxyUser;
        staticProxyPassword = proxyPassword;
        initialized = true;
        
        System.out.println("🔧 FixieSocketFactory inicializado: " + proxyHost + ":" + proxyPort);
        
        // Configurar autenticación
        if (proxyUser != null && proxyPassword != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        System.out.println("🔐 Autenticando proxy para: " + getRequestingHost());
                        return new PasswordAuthentication(staticProxyUser, staticProxyPassword.toCharArray());
                    }
                    return null;
                }
            });
        }
    }
    
    private void initializeFromSystemProperties() {
        staticProxyHost = System.getProperty("socksProxyHost");
        String portStr = System.getProperty("socksProxyPort", "1080");
        try {
            staticProxyPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            staticProxyPort = 1080;
        }
        staticProxyUser = System.getProperty("java.net.socks.username");
        staticProxyPassword = System.getProperty("java.net.socks.password");
        initialized = true;
    }
    
    @Override
    public Socket createSocket() throws IOException {
        if (!initialized || staticProxyHost == null) {
            System.err.println("❌ FixieSocketFactory no inicializado correctamente");
            return new Socket();
        }
        
        System.out.println("🌐 Creando socket via proxy: " + staticProxyHost + ":" + staticProxyPort);
        Proxy socksProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(staticProxyHost, staticProxyPort));
        return new Socket(socksProxy);
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        System.out.println("🌐 Conectando a: " + host + ":" + port + " via proxy");
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port), 120000); // 2 minutos timeout
        return socket;
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(new InetSocketAddress(host, port), 120000);
        return socket;
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port), 120000);
        return socket;
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port), 120000);
        return socket;
    }
}
