package es.musicalia.gestmusica.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static String getDateStr(Date date, String format){
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

}
