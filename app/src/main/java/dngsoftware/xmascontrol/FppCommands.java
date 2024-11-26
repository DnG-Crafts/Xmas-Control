package dngsoftware.xmascontrol;


public class FppCommands {

    //fpp rest api
    public static String  fppInfo = "/api/system/info";
    public static String  fppStatus = "/api/system/status";
    public static String  fppReboot = "/api/system/reboot";
    public static String  fppShutDown = "/api/system/shutdown";
    public static String  fppDRestart = "/api/system/fppd/restart";
    public static String  fppDStart = "/api/system/fppd/start";
    public static String  fppDStop = "/api/system/fppd/stop";
    public static String  fppCommand = "/api/command";
    public static String  fppEffects = "/api/effects";
    public static String  fppStartEffect = fppCommand + "/FSEQ Effect Start/%s";
    public static String  fppStopEffect = fppCommand + "/Effects Stop";
    public static String  fppPlaylist = "/api/playlist/%s";
    public static String  fppPlaylists = "/api/playlists";
    public static String  fppStartPlaylist = "/api/playlist/%s/start";
    public static String  fppStartPlaylistAtBody = "{\"command\":\"Start Playlist At Item\",\"args\":[\"%s\",%s,%s,false]}";
    public static String  fppStartPlaylistBody = "{\"command\":\"Start Playlist\",\"args\":[\"%s\",1,%s,%s]}";
    public static String fppStartSequenceAsPlaylistBody = "{\"command\":\"Start Playlist\",\"args\":[\"%s.fseq\",%s,false]}";
    public static String  fppStopPlaylist = fppPlaylists + "/stop";
    public static String  fppNextPlaylistItem = fppCommand + "/Next Playlist Item";
    public static String  fppPrevPlaylistItem = fppCommand + "/Prev Playlist Item";
    public static String  fppSequence = "/api/sequence";
    public static String  fppSequenceMeta = "/api/sequence/%s/meta";
    public static String  fppStartSequence = fppSequence + "/%s/start/0";
    public static String  fppStopSequence = fppSequence + "/current/stop";
    public static String  fppStartTestBody = "{\"command\":\"Test Start\",\"multisyncCommand\":false,\"multisyncHosts\":\"\",\"args\":[\"1000\",\"%s\",\"%s\",\"%s\"]}";
    public static String  fppStopTest = fppCommand + "/Test Stop";
    public static String  fppVolume = "/api/system/volume";
    public static String  fppVolumeBody = "{\"volume\": %s}";

}