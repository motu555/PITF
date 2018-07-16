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
    public static List<Post> readPostUTP(String filename){
        List<Post> dataPosts = new ArrayList<>();
        List<String> dataList = FileOperation.readLineArrayList(filename);
        for (String data : dataList) {
            String[] contents = data.trim().split("[ \t,]+");
            /**
             * 注意文件中的顺序与读取顺序对应
             */
            int user = Integer.parseInt(contents[0]);
            int item = Integer.parseInt(contents[1]);
            int timestamp = Integer.parseInt(contents[2]);
//            String time = Long.parseLong(contents[3]);
            dataPosts.add(new Post(user, item, timestamp));
        }

        return dataPosts;
    }

}
