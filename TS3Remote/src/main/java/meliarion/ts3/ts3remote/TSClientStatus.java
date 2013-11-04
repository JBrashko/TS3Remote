package meliarion.ts3.ts3remote;

/**
 * Created by Meliarion on 01/08/13.
 * Enum detailing the various states a teamspeak client can have
 */
public enum TSClientStatus {
    NotTalking,
    Talking,
    CommanderNotTalking,
    CommanderTalking,
    Whispering,
    LocalMuted,
    InputDisabled,
    InputMuted,
    OutputDisabled,
    OutputMuted,
    Away
}
