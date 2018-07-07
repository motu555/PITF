package test;

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
        String dataName = "Dianping_Time";
//        String fileName = "checkin_pitf";
        int coreNum = 1;
//        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/" + fileName + "_train");
//        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/" + fileName + "_test");
        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/"  + "_train");
        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/"  + "_test");
        System.out.println("PITFTest");
        System.out.println(dataName + " " + coreNum);
        int dim = 64;
        double initStdev = 0.01;
        int iter = 20;//100
        double learnRate = 0.05;
        double regU = 0.00005;
        double regI = regU, regT = regU;
        int numSample = 100;
        int randomSeed = 1;

        PITF pitf = new PITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample);
        pitf.run();//这个函数会调用model的初始化及创建函数
        System.out.println("dataName:\t" + dataName);
        System.out.println("coreNum:\t" + coreNum);

    }
}
