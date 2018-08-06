package data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/14.
 */
public class DataReader {
    /*
     我们读取train与test数据时默认的前提是两者中的user和item是一致的
     */
    public static List<Post> readPosts(String filename) {
        List<Post> dataPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(filename);
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
            int user = Integer.parseInt(contents[0]);
            int item = Integer.parseInt(contents[1]);
            int tag = Integer.parseInt(contents[2]);
            long time = Long.parseLong(contents[3]);
            dataPosts.add(new Post(user, item, tag, time));
        }
        //加上对train与test数据集中的user，item,tag数据统计
        return dataPosts;
    }
    //没有时间
    public static List<Post> readPost(String filename) {
        List<Post> dataPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(filename);
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
            int user = Integer.parseInt(contents[0]);
            int item = Integer.parseInt(contents[1]);
            int tag = Integer.parseInt(contents[2]);
//            long time = Long.parseLong(contents[3]);
            dataPosts.add(new Post(user, item, tag));
        }
        //加上对train与test数据集中的user，item,tag数据统计
        return dataPosts;
    }
    public static List<Post> readPostUTP(String filename){
        List<Post> dataPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(filename);
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
            /**
             * 注意文件中的顺序与读取顺序对应
             */
            int user = Integer.parseInt(contents[0]);
            int timestamp = Integer.parseInt(contents[1]);//时间===pitf中item
            int shop = Integer.parseInt(contents[2]);//poi====pitf中tag
            dataPosts.add(new Post(user, timestamp, shop));
        }

        return dataPosts;
    }

    //  ContextPost( tag, cate, lng, lat)
    //测试能否读取成功，先要把cat编号
    public static List<ContextPost> readContextPost(String filename){
        List<ContextPost> ContextPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(filename);
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
            /**
             * 合并在一个文件中
             * 注意文件中的顺序与读取顺序对应
             */
            int tag = Integer.parseInt(contents[2]);//shop
            int cate = Integer.parseInt(contents[3]);
            String lng = contents[4];
            String lat = contents[5];
            ContextPosts.add(new ContextPost( tag, cate, lng, lat));
        }

        return ContextPosts;
    }

}
