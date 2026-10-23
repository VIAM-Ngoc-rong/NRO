package nro.models.network;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.IOException;
import nro.models.interfaces.IMessageHandler;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public final class Collector
        implements Runnable {

    private ISession session;
    private Socket rawSocket;
    private java.io.BufferedInputStream bis;
    private DataInputStream dis;
    private IMessageSendCollect collect;
    private IMessageHandler messageHandler;

    public Collector(ISession session, Socket socket) {
        this.session = session;
        this.setSocket(socket);
    }

    public Collector setSocket(Socket socket) {
        this.rawSocket = socket;
        try {
            this.bis = new java.io.BufferedInputStream(socket.getInputStream(), 8192);
            this.dis = new DataInputStream(this.bis);
        } catch (IOException iOException) {
        }
        return this;
    }

    @Override
    public void run() {
        try {
            if (this.bis != null && this.rawSocket != null) {
                this.bis.mark(8);
                int b0 = this.bis.read();
                if (b0 == 'G' || b0 == 'P' || b0 == 'O' || b0 == 'H') {
                    int b1 = this.bis.read();
                    int b2 = this.bis.read();
                    int b3 = this.bis.read();
                    boolean isHttp = (b0 == 'G' && b1 == 'E' && b2 == 'T' && b3 == ' ')
                            || (b0 == 'P' && b1 == 'O' && b2 == 'S' && b3 == 'T')
                            || (b0 == 'O' && b1 == 'P' && b2 == 'T' && b3 == 'I')
                            || (b0 == 'H' && b1 == 'E' && b2 == 'A' && b3 == 'D');
                    this.bis.reset();
                    if (isHttp) {
                        nro.models.server.AdminWebServer.relaySocketToLocalHttp(this.rawSocket, this.bis);
                        try {
                            Network.gI().getAcceptHandler().sessionDisconnect(this.session);
                        } catch (Exception ignored) {
                        }
                        if (this.session != null) {
                            this.session.disconnect();
                        }
                        return;
                    }
                } else if (b0 != -1) {
                    this.bis.reset();
                }
            }
            while (this.session != null && this.session.isConnected()) {
                Message msg = this.collect.readMessage(this.session, this.dis);
                if (msg.command == -27) {
                    this.session.sendKey();
                } else {
                    this.messageHandler.onMessage(this.session, msg);
                }
                msg.cleanup();
            }
        } catch (Exception exception) {
        }
        try {
            Network.gI().getAcceptHandler().sessionDisconnect(this.session);
        } catch (Exception exception) {
        }
        if (this.session != null) {
            this.session.disconnect();
        }
    }

    public void setCollect(IMessageSendCollect collect) {
        this.collect = collect;
    }

    public void setMessageHandler(IMessageHandler handler) {
        this.messageHandler = handler;
    }

    public void close() {
        if (this.dis != null) {
            try {
                this.dis.close();
            } catch (IOException iOException) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.rawSocket = null;
        this.bis = null;
        this.dis = null;
        this.collect = null;
    }
}
