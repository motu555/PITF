package core;

import core.PITF;
import data.DataReader;
import data.Post;

import java.io.IOException;
import java.util.List;

/**
 * Created by motu on 2018/7/6.
 * user-time-poi三维张量
 */
public class PIFTUTP {
    public static void main(String[] args) throws IOException {
//        String dataName = "lastFM";
        String dataName = "DianpingCheckinfalse2525";
        String fileName = "UTP";

        List<Post> trainPosts = DataReader.readPostUTP("./demo/data/"  + fileName + "/"+ dataName  + "_train.txt");
        List<Post> testPosts = DataReader.readPostUTP("./demo/data/"  + fileName + "/"+ dataName  + "_test.txt");
//        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/"  + "_train");
//        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/"  + "_test");
        System.out.println("PITFTest");
        System.out.println(dataName);
        int dim = 64;//纬度
        double initStdev = 0.01;//初始化的赋值方差
        int iter = 20;//100
        double learnRate = 0.05;//
        double regU = 0.00005;
        double regI = regU, regT = regU;//lameta
        int numSample = 100;
        int randomSeed = 1;

        PITF pitf = new PITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample);
        pitf.run();//这个函数会调用model的初始化及创建函数
        System.out.println("dataName:\t" + dataName);
//        System.out.println("coreNum:\t" + coreNum);

    }
}
