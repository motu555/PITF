package util;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by jinyuanyuan on 2018/3/20.
 */
public class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        Date date = new Date();
        String sDate = date.toString();
        return  record.getMessage() + "\n";
    }
}
