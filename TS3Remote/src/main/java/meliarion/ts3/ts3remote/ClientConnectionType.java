package meliarion.ts3.ts3remote;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Meliarion on 25/11/13.
 */
public enum ClientConnectionType {
    InvalidConnectionType(-1),
    DirectNetwork(0),
    ManagedNetwork(1),
    SecureNetwork(2),
    USB(3);
    private int Code;
    ClientConnectionType(int messageCode)
    {
        this.Code = messageCode;
    }
    int showCode(){
        return Code;
    }
    private static final Map lookup = new HashMap();

    // Populate the lookup table on loading time
    static {
        for (ClientConnectionType s : EnumSet.allOf(ClientConnectionType.class))
            lookup.put(s.showCode(), s);
    }

    public static ClientConnectionType get(int code){
        return (ClientConnectionType)lookup.get(code);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
