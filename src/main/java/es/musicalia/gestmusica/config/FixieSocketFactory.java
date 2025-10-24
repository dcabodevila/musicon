package es.musicalia.gestmusica.config;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.*;

public class FixieSocketFactory extends SocketFactory {
    
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUser;
    private final String proxyPassword;
    private final Proxy socksProxy;
    
    public FixieSocketFactory(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.socksProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
        
        // Configurar autenticación si es necesaria
        if (proxyUser != null && proxyPassword != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                    return null;
                }
            });
        }
    }
    
    @Override
    public Socket createSocket() throws IOException {
        return new Socket(socksProxy);
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket(socksProxy);
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket socket = new Socket(socksProxy);
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = new Socket(socksProxy);
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = new Socket(socksProxy);
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port));
        return socket;
    }
}
