package test;

import core.BLLC;
import data.DataReader;
import data.Post;

import java.io.IOException;
import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/18.
 */
public class BLLCTest {
    public static void main(String[] args) throws IOException {
        String dataName = "lastfm";
        int coreNum = 3;
        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_train");
        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_test");
        int randomSeed = 1;
        System.out.println(dataName +" "+coreNum);

        double[]dstep = new double[]{0.5};
        double[]betaStep = new double[]{0.5};
        for(double d: dstep){
            for(double beta:betaStep){
                System.out.println("d "+d+" beta "+beta);
                BLLC bllc = new BLLC(trainPosts, testPosts, randomSeed, d, beta);
                bllc.run();
            }
        }
    }
}
