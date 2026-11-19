package nro.models.network;

import java.net.Socket;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public final class Sender implements Runnable {

    @NonNull
    private ISession session;
    @NonNull
    private BlockingDeque<Message> messages;
    private DataOutputStream dos;
    private IMessageSendCollect sendCollect;

    public Sender(@NonNull ISession session, @NonNull Socket socket) {
        if (session == null) {
            throw new NullPointerException("session is marked non-null but is null");
        }
        if (socket == null) {
            throw new NullPointerException("socket is marked non-null but is null");
        }
        try {
            this.session = session;
            this.messages = new LinkedBlockingDeque<Message>(4000);
            this.setSocket(socket);
        } catch (Exception exception) {
        }
    }

    public Sender setSocket(@NonNull Socket socket) {
        if (socket == null) {
            throw new NullPointerException("socket is marked non-null but is null");
        }
        try {
            this.dos = new DataOutputStream(new java.io.BufferedOutputStream(socket.getOutputStream(), 8192));
        } catch (IOException iOException) {
        }
        return this;
    }

    @Override
    public void run() {
        while (this.session != null && this.session.isConnected()) {
            try {
                BlockingDeque<Message> queue = this.messages;
                if (queue == null) {
                    break;
                }
                Message message = queue.poll(1L, TimeUnit.SECONDS);
                if (message != null) {
                    this.doSendMessage(message);
                    message.cleanup();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException ioe) {
                break;
            } catch (Exception exception) {
            }
        }
    }

    public synchronized void doSendMessage(Message message) throws Exception {
        if (this.sendCollect != null && this.dos != null && this.session != null && this.session.isConnected()) {
            this.sendCollect.doSendMessage(this.session, this.dos, message);
        }
    }

    public void sendMessage(Message msg) {
        try {
            if (msg != null && this.session != null && this.session.isConnected() && this.messages != null) {
                // Chốt sẵn mảng byte payload trước khi đưa vào hàng đợi để tránh xung đột với msg.cleanup() ở luồng gọi
                msg.getData();
                if (!this.messages.offer(msg)) {
                    msg.cleanup();
                    if (this.messages.size() >= 4000) {
                        this.session.disconnect();
                    }
                }
            }
        } catch (Exception exception) {
        }
    }

    public void setSend(IMessageSendCollect sendCollect) {
        this.sendCollect = sendCollect;
    }

    public int getNumMessages() {
        return this.messages.size();
    }

    public void close() {
        this.messages.clear();
        if (this.dos != null) {
            try {
                this.dos.close();
            } catch (IOException iOException) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.messages = null;
        this.sendCollect = null;
        this.dos = null;
    }
}
