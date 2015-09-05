package meliarion.ts3.ts3remote;

/**
 * Created by Meliarion on 30/07/13.
 * Exception created to handle client query errors
 */
public class ClientQueryMessageException extends Exception {
    private final int errorCode;
    private final String errorDescription;
    public ClientQueryMessageException(int code, String message){
        super(message);
        this.errorCode = code;
        this.errorDescription = message;
    }
    public ClientQueryMessageException(int code, String message, Throwable tr){
        super(message,tr);
        this.errorCode = code;
        this.errorDescription = message;
    }
    public int getCode(){
        return errorCode;
    }
    public String getDescription(){
        return errorDescription;
    }

}
