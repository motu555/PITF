package dataProcess;

import com.sun.org.apache.xerces.internal.xs.StringList;
import data.FileOperation;
import data.Post;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by motu on 2018/6/17.
 * 划分test、train
 */
public class process {
    /*
    读取原始数据checkinWithTimestamp.txt 格式"userid+\t+poiid+\t+poicategory+time"
    并以<user,time,category,time>存储，time转换为unix时间戳
    */
    public static void timestampTrans(String filename) {
        String readname = filename + "/checkinWithTimestamp.txt";
        String writename = filename + "/checkinWithUnixTime.txt";
//    List<Post> dataPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(readname);
        List<String> output = new ArrayList<>();
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
       /* int user = Integer.parseInt(contents[0]);
        int item = Integer.parseInt(contents[1]);
        String date = Date2TimeStamp(contents[3], "yy-MM-dd");
        long time = Long.parseLong(date);*/
//        dataPosts.add(new Post(user, item, tag,time));
            String date = Date2TimeStamp(contents[3], "yy-MM-dd");
            String line = contents[0] + "\t" + contents[1] + "\t" + contents[2] + "\t" + date;
            System.out.println(line);
            output.add(line);
            FileOperation.writeAppdend(writename, line + "\t");

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

    public static String TimeStamp2Date(long timestamp, String formats) {
        String date = new SimpleDateFormat(formats, Locale.CHINA).format(new Date(timestamp));
        return date;
    }

    /**
     * 将文件mapping编号，并分为训练集和测试集
     * 随机
     * TODO
     * 按照每个用户的最后几个切分？
     */
    public static void DivideData(String filepath, String filename) {

        String readpath = filepath + filename + ".txt";
        String despth = "./demo/data/UTP/";

        FileInputStream file;
        BufferedReader bufferedReader;
        StringBuilder trainStr = new StringBuilder();
        StringBuilder testStr = new StringBuilder();
        String read;
        Map<String, Integer> userIds, poiIds, timeIds,catIds;//用于编号的映射关系
        userIds = new HashMap<>();
        poiIds = new HashMap<>();
        timeIds = new HashMap<>();
        catIds = new HashMap<>();
        int trainnum = 0;
        int testnum = 0;
        try {
            file = new FileInputStream(readpath);
            bufferedReader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            while ((read = bufferedReader.readLine()) != null) {
                String[] contents = read.trim().split("[ \t,]+");
                String tempUser = contents[0];
                String tempTime = contents[1];
                String tempItem = contents[2];
                String tempCat = contents[3];
                String tempLng = contents[4];
                String tempLat = contents[5];
                String month = tempTime.substring(3, 5);
                //重新映射编号
                int innerUser = userIds.containsKey(tempUser) ? userIds.get(tempUser) : userIds.size();
                userIds.put(tempUser, innerUser);
                int innerPoi = poiIds.containsKey(tempItem) ? poiIds.get(tempItem) : poiIds.size();
                poiIds.put(tempItem, innerPoi);
                int innerTime = timeIds.containsKey(tempTime) ? timeIds.get(tempTime) : timeIds.size();
                timeIds.put(tempTime, innerTime);
                int innerCat = catIds.containsKey(tempCat) ? catIds.get(tempCat) : catIds.size();
                catIds.put(tempCat,innerCat);
                String writeStr = innerUser + "\t" + innerTime + "\t" + innerPoi + "\t" +innerCat+"\t" +tempLng+"\t" +tempLat+"\n";

                if (Math.random() < 0.8) {//随机二八划分训练集
//                if(Integer.parseInt(month)<10){
//                    System.out.print(writeStr);
                    trainStr.append(writeStr);
                    trainnum++;
                } else {
//                    System.out.print(writeStr);
                    testStr.append(writeStr);
                    testnum++;
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("trainnum:" + trainnum + " testnum:   " + testnum);
        System.out.println("usernum:" + userIds.size() + " poinum:" + poiIds.size() + " timenum:" + timeIds.size());
        FileOperation.writeNotAppdend(despth + filename + "random_train.txt", trainStr.toString());
        FileOperation.writeNotAppdend(despth + filename + "random_test.txt", testStr.toString());
        //id映射文件记录
        FileOperation.writeNotAppdend(filepath + filename + "_userMapIndex", userIds.toString());
        FileOperation.writeNotAppdend(filepath + filename + "_poiMapIndex", poiIds.toString());
        FileOperation.writeNotAppdend(filepath + filename + "_timeMapIndex", timeIds.toString());
        FileOperation.writeNotAppdend(filepath + filename + "_timeMapIndex", catIds.toString());

    }

    /**
     * 对user进行map映射
     *
     * @param filepath
     */
    public static void getuserMap(String filepath) {
        String filename = "DianpingCheckinfalse1010";
        String readpath = filepath + filename + ".txt";
        String despth = "./demo/data/UTP/";
        FileInputStream file;
        BufferedReader bufferedReader;
        StringBuilder trainStr = new StringBuilder();
        StringBuilder testStr = new StringBuilder();
        String read;
        Map<String, Integer> userIds;//用于编号的映射关系
        userIds = new HashMap<>();
        try {
            file = new FileInputStream(readpath);
            bufferedReader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            while ((read = bufferedReader.readLine()) != null) {
                String[] contents = read.trim().split("[ \t,]+");
                String tempUser = contents[0];
                //重新映射编号
                int innerUser = userIds.containsKey(tempUser) ? userIds.get(tempUser) : userIds.size();
                userIds.put(tempUser, innerUser);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOperation.writeNotAppdend(filepath + filename + "_userMap", userIds.toString());

    }

    /**
     * 将shopInfo中category的中文字段映射为id
     */
    public static void shopCatId(String filepath) {
        String filename = "shopInfo_15";
        String readpath = filepath + filename + ".txt";
        String despth = "./demo/data/UTP/";
        FileInputStream file;
        BufferedReader bufferedReader;
        StringBuilder trainStr = new StringBuilder();
        StringBuilder testStr = new StringBuilder();
        String read;
        Map<String, Integer> shopIds;
        shopIds = new HashMap<>();
        Map<String, Integer> catIds;
        catIds = new HashMap<>();
        Map<String, Integer> lngIds;
        lngIds = new HashMap<>();
        Map<String, Integer> latIds;
        latIds = new HashMap<>();
        try {
            file = new FileInputStream(readpath);
            bufferedReader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            while ((read = bufferedReader.readLine()) != null) {
                String[] contents = read.trim().split("[ \t,]+");
                String tempShop = contents[0];
                String tempCat = contents[1];
                String tempLng = contents[2];
                String tempLat = contents[3];
                //重新映射编号
//                int innerShop = shopIds.containsKey(tempShop) ? shopIds.get(tempShop) : shopIds.size();
//                shopIds.put(tempShop, innerShop);
                int innerCat = catIds.containsKey(tempCat) ? catIds.get(tempCat) : catIds.size();
                catIds.put(tempCat,innerCat);
                //经纬度不需要映射id
//                int innerIng = lngIds.containsKey(tempLng) ? lngIds.get(tempLng) : lngIds.size();
//                lngIds.put(tempLng, innerIng);
//                int innerIat = latIds.containsKey(tempLat) ? latIds.get(tempLat) : latIds.size();
//                latIds.put(tempLat, innerIat);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        FileOperation.writeNotAppdend(filepath + filename + "_userMap", userIds.toString());

    }


    public static void main(String args[]) {
//        timestampTrans("./rawdata");
        String filename = "DianpingCheckin15info0.3_false1510";
            DivideData("./rawdata/modelInput/" ,filename);



//        String formats = "yyyy-MM-dd HH:mm:ss";
//        System.out.println(TimeStamp2Date("886397596000", formats));
////        1288397039000
//        System.out.println(TimeStamp2Date(System.currentTimeMillis(), formats));
//        getuserMap("./rawdata/");
//        String date = Date2TimeStamp("15-09-24" , "yy-MM-dd");// HH:mm:ss
//        String date =TimeStamp2Date( "1288291694000","yy-MM-dd");
    }

}
