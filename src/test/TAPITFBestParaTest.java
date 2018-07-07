package test;

import core.TAPITF;
import data.DataReader;
import data.DenseVector;
import data.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jinyuanyuan on 2018/3/20.
 * 使用6个数据集上的最优参数运行
 * //TODO 最优参数还需要根据修正后的代码调整
 */
public class TAPITFBestParaTest {
    public static void main(String[] args) throws IOException {
        String[] dataNames = new String[]{"movielens", "lastfm", "delicious"};
        //1表示没有经过过滤的数据集，3表示数据集按照p-3的标准过滤过
        int[] coreNums = new int[]{1, 3};

        /**
         *  这里指定dataName和coreNum,以及相应的alpha的取值
         */
        String dataName = dataNames[0];
        int coreNum = coreNums[1];
        /**
         * movielens [3] 1.4   movielens [1] 1.6
         * lastfm [3] 0.2     lastfm [1] 0.4
         * delicious [3] 0.8   delicious [1] 0.6
         */
        double[] alphaSteps = new double[]{1.4, 0.2, 0.8, 1.6, 0.4, 0.6};
        double alphaUser = alphaSteps[3];
        System.out.println(dataName + " " + coreNum + " " + alphaUser);
        System.out.println("PITFBLLCHawkesTest");

        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_train");
        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_test");
        /**
         * 固定参数，以下参数没有调参
         */
        //隐向量维度
        int dim = 64;
        //参数初始化时高斯分布的标准差
        double initStdev = 0.01;
        //迭代次数
        int iter = 100;
        //步长
        double learnRate = 0.05;
        //正则化项系数
        double regU = 0.00005;
        double regI = regU, regT = regU;
        //控制迭代样本总数的变量，可以调节
        int numSample = 100;
        int randomSeed = 1;

        /**
         * 以下取值为调参后固定下来的较优的值
         * d=0.5
         * initialTao = 0
         */
        //hawkes过程函数 指数中的参数
        double[] dstep = new double[]{0.5};
        //hawkes过程的初始强度值
        double[] initialTaoStep = new double[]{0.0};
        //模型中的两个系数 w_ut和w_mt中的参数 alpha_P和alpha_Q的取值，是我们重点要调的参数

        //hawkes过程中的指数函数的底数e的调节，当时尝试了2,3这种数值，发现1还是比较好
        double e = Math.exp(1);
        double[] baseStep = new double[]{e};

        //用于记录最终结果和运行时间的数据结构
        List<DenseVector> maxMeasureList = new ArrayList<>();
        List<DenseVector> eachMeasureList = new ArrayList<>();
        double[] paras = new double[4];
        double trainTime = 0.0;


        System.out.println(alphaUser);
        for (int k = 0; k < initialTaoStep.length; k++) {
            double initialTao = initialTaoStep[k];
            for (int m = 0; m < baseStep.length; m++) {
                double base = baseStep[m];
                for (int i = 0; i < dstep.length; i++) {
                    double d = dstep[i];
                    double alpahItem = alphaUser;
                    TAPITF TAPITF = new TAPITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample, d, base, initialTao, alphaUser, alpahItem);
                    TAPITF.run();
                    eachMeasureList = TAPITF.getMeasureList();
                    maxMeasureList = eachMeasureList;

                    System.out.println("d " + d + " alphaUser " + alphaUser + " initialTao " + initialTao + " averageTrainTime " + TAPITF.getAverageTrainTime());
                }
            }
        }


        System.out.println(dataName + " " + coreNum + " best performance on F1@5");
        System.out.printf("%-15s\t", "Train Time:");
        System.out.println(trainTime + "s");
        System.out.printf("%-15s\t", "Top N");

        for (int index = 0; index < 10; index++) {
            System.out.printf("%-6d\t", index + 1);
        }
        System.out.println();

        System.out.printf("%-15s\t", "Precisions");

        DenseVector precisions = maxMeasureList.get(0);
        for (int index = 0; index < 10; index++) {
            System.out.printf("%.4f\t", precisions.get(index));
        }
        System.out.println();

        System.out.printf("%-15s\t", "Recalls");
        DenseVector recalls = maxMeasureList.get(1);
        for (int index = 0; index < 10; index++) {
            System.out.printf("%.4f\t", recalls.get(index));
        }
        System.out.println();

        System.out.printf("%-15s\t", "F1");
        DenseVector f1 = maxMeasureList.get(2);
        for (int index = 0; index < 10; index++) {
            System.out.printf("%.4f\t", f1.get(index));
        }
        System.out.println();

        System.out.printf("%-15s\t", "AIP@10");

        System.out.printf("%.4f\t", maxMeasureList.get(3).get(0));

        System.out.println();

        System.out.printf("%-15s\t", "AILD@10");

        System.out.printf("%.4f\t", maxMeasureList.get(4).get(0));
        System.out.println();

        System.out.println("==============================================================================================");
    }
}


