package meliarion.ts3.ts3remote;

/**
 * Created by James on 24/08/2015.
 * Class designed to allow the storage of Chat messages and associated information
 */
public class ChatMessage {
    private final int sender;
    private final String message;
    private final long timestap;

    public ChatMessage(String _message, int _sender, long _timestamp) {
        this.sender = _sender;
        this.message = _message;
        this.timestap = _timestamp;

    }

    public int getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestap() {
        return timestap;
    }

    public String toString() {
        return sender + ":" + message;
    }
}
