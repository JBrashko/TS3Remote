package meliarion.ts3.ts3remote;

/**
 * Created by Meliarion on 30/07/13.
 * Exception to handle issues in the Client connection
 */
public class TSRemoteException extends Exception {
    String message;

    TSRemoteException(String _message){
        super(_message);
        this.message=_message;
    }
    TSRemoteException(String _message,Throwable tr){
        super(_message,tr);
        this.message=_message;
    }
}
