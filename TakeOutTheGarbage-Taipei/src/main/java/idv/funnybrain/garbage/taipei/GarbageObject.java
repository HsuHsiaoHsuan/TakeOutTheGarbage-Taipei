package idv.funnybrain.garbage.taipei;

/**
 * Created by Freeman on 2014/3/20.
 */
public class GarbageObject {

    private String ADDRESS = "";
    private String DISTRICT = "";
    //private String CAR_NUM = "";
    //private String CAR_SERIAL = "";
    private String START_TIME = "";
    private String END_TIME = "";
    private String LAT = "";
    private String LNG = "";
    private String NOTICE = "";

    //GarbageObject(String add, String dist, String num, String ser, String start, String end, String lat, String lng, String not) {
    GarbageObject(String add, String dist, String start, String end, String lat, String lng, String not) {
        ADDRESS = add;
        DISTRICT = dist;
        //CAR_NUM = num;
        //CAR_SERIAL = ser;
        START_TIME = start;
        END_TIME = end;
        LAT = lat;
        LNG = lng;
        NOTICE = not;
    }

    public String getADDRESS() { return ADDRESS; }

    public String getDISTRICT() { return DISTRICT; }

    //public String getCAR_NUM() { return CAR_NUM; }

    //public String getCAR_SERIAL() { return CAR_SERIAL; }

    public String getSTART_TIME() { return START_TIME; }

    public String getEND_TIME() { return END_TIME; }

    public String getLAT() { return LAT; }

    public String getLNG() { return LNG; }

    public String getNOTICE() { return NOTICE; }
}
