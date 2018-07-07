package dataProcess;

import com.sun.org.apache.xerces.internal.xs.StringList;
import data.FileOperation;
import data.Post;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by motu on 2018/6/17.
 */
public class process {
/*
读取原始数据checkinWithTimestamp.txt 格式"userid+\t+poiid+\t+poicategory+time"
并以<user,time,category,time>存储，time转换为unix时间戳
*/
    public static void timestampTrans(String filename) {
    String readname = filename+"/checkinWithTimestamp.txt";
    String writename = filename+ "/checkinWithUnixTime.txt";
//    List<Post> dataPosts = new ArrayList<>();
    List<String> dataList = FileOperation.readLineArrayList(readname);
    List<String> output= new ArrayList<>() ;
    for (String data : dataList) {
        String[] contents = data.trim().split("[ \t,]+");
       /* int user = Integer.parseInt(contents[0]);
        int item = Integer.parseInt(contents[1]);
        String date = Date2TimeStamp(contents[3], "yy-MM-dd");
        long time = Long.parseLong(date);*/
//        dataPosts.add(new Post(user, item, tag,time));
        String date = Date2TimeStamp(contents[3], "yy-MM-dd");
        String line = contents[0]+"\t"+contents[1]+"\t"+contents[2]+"\t"+ date;
        System.out.println(line);
        output.add(line);
        FileOperation.writeAppdend(writename,line+"\t");

    }

    //加上对train与test数据集中的user，item,tag数据统计
//    return dataPosts;

}
    /*将str指定格式时间转换为unix时间戳*/
    public static String Date2TimeStamp(String dateStr, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return String.valueOf(sdf.parse(dateStr).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    /*将unix时间戳转换为指定格式*/
    public static String TimeStamp2Date(String timestampString, String formats) {
//        formats = "yyyy-MM-dd HH:mm:ss";
        Long timestamp = Long.parseLong(timestampString);//* 1000
        String date = new SimpleDateFormat(formats, Locale.CHINA).format(new Date(timestamp));
        return date;
    }

    /**
     * 将文件mapping编号，并分为训练集和测试集
     *随机
     * TODO
     * 按照每个用户的最后几个切分？
     */
    public static void DivideData(String filepath){
        String filename = "DianpingCheckinfalse2525.txt";
        String readpath = filepath+filename;

        FileInputStream file;
        BufferedReader bufferedReader;
        StringBuilder trainStr = new StringBuilder();
        StringBuilder testStr = new StringBuilder();
        String read;
        int trainnum=0;
        int testnum=0;
        try {
            file = new FileInputStream(readpath);
            bufferedReader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            while ((read = bufferedReader.readLine()) != null) {
                if(Math.random()<0.8){

                }
                else {

                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static void main(String args[]){
//        timestampTrans("./rawdata");
            DivideData("./rawdata/");
//        String date = Date2TimeStamp("15-09-24" , "yy-MM-dd");// HH:mm:ss
//        String date =TimeStamp2Date( "1288291694000","yy-MM-dd");
//        System.out.print(date);
    }

}
