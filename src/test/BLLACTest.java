package test;

import core.BLLAC;
import data.DataReader;
import data.Post;

import java.io.IOException;
import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/18.
 * BLL_AC + MPm
 * 把BLL_AC部分的系数设置为0， 就退化成MPm了
 */
public class BLLACTest {
    public static void main(String[] args) throws IOException {
        String dataName = "movielens";
        int coreNum = 3;
        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_train");
        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_test");
        int randomSeed = 1;
        double[]dstep = new double[]{0.5};
        double[]betaStep =new double[]{0.5};
        System.out.println(dataName +" "+coreNum);
        for(double d: dstep){
            for(double beta:betaStep){
                System.out.println("d "+d+" beta "+beta);
                BLLAC bllac = new BLLAC(trainPosts, testPosts, randomSeed, d, beta);
                bllac.run();
            }
        }
    }
}
