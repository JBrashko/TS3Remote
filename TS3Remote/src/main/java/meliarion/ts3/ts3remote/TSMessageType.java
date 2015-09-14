package meliarion.ts3.ts3remote;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Meliarion on 26/07/13.
 * Enum which contains the various messages that the client connection send to the activity
 */

public enum TSMessageType {
    DebugMessage (0),
    GeneralMessage (1),
    SetupMessage (2),
    ChatMessage (3),
    ClientAltered(4),
    SetupDone(5),
    DisplayedServerConnectionChange(6),
    ServerConnectionChange(7),
    ChannelAltered(8),
    ClientMoved(9),
    ClientLeft(10),
    ClientJoined(11),
    ChannelCreated(12),
    ChannelDeleted(13),
    ChannelMoved(14),
    ClientConnectionAltered(15),
    ServerAltered(16),
    ClientTalkStatusChanged(17);

    private final int Code;
    TSMessageType(int messageCode)
    {
        this.Code = messageCode;
    }
    int showCode(){
        return Code;
    }

   private static final Map lookup = new HashMap();

	// Populate the lookup table on loading time
	static {
		for (TSMessageType s : EnumSet.allOf(TSMessageType.class))
			lookup.put(s.showCode(), s);
	}

    public static TSMessageType get(int code){
        return (TSMessageType)lookup.get(code);
    }


}
