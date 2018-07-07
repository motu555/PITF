package test;

import core.TAPITF;
import data.DataReader;
import data.DenseVector;
import data.Post;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by jinyuanyuan on 2017/9/8.
 * 1。确定调参范围
 * 2.将调参结果写入Log文件，需要全部的详细结果，最好的一次的结果和参数，以及每个参数下100次的结果
 * 3.我这里对TAPITF调参只调alpha【0.0-2.0】
 */
public class TAPITFTuningParaTest {

    private static Log allLog ;

    private static Log measureLog ;


    public static void main(String[] args) throws IOException {
        String className = "TAPITF";
        String dataName = "delicious";
        int coreNum = 3;

        /**
         * 配置Log
         */
//        allLog = MeasureLogs.newInstance("alllog", className, dataName, coreNum);
//        measureLog = MeasureLogs.newInstance("measurelog", className, dataName, coreNum);

        allLog = LogFactory.getLog("alllog");
        measureLog = LogFactory.getLog("measurelog");

        allLog.info(className);
        measureLog.info(className);

        List<Post> trainPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_train");
        List<Post> testPosts = DataReader.readPosts("./demo/data/" + dataName + "/all_id_core" + coreNum + "_test");
        int dim = 64;
        double initStdev = 0.01;
        int iter = 100;
        double learnRate = 0.05;
        double regU = 0.00005;
        double regI = regU, regT = regU;
        int numSample = 100;
        int randomSeed = 1;

        double[] dstep = new double[]{0.5};
        double[] initialTaoStep = new double[]{0.0};

        //调参范围[0.0-2.0]
//        double[] alphaStep = new double[]{1.4, 1.6};
//        double[] alphaStep = new double[]{0.0, 0.2, 0.4, 0.6, 0.8 };
        double[] alphaStep = new double[]{1.0, 1.2, 1.4, 1.6, 1.8, 2.0};


//        double[] alphaStep = new double[]{1.0,1.2, 1.4,1.6,1.8, 2.0};
//        double[] alphaStep = new double[]{0.6};
//        double[] alphaStep = new double[]{0.6};
        double e = Math.exp(1);
        double[] baseStep = new double[]{e};

        double maxF1 = 0.0;
        List<DenseVector> bestMeasureList = new ArrayList<>();
        List<DenseVector> eachMeasureList = new ArrayList<>();
        double[] bestParas = new double[5];
        double[] eachParas = new double[5];//包括d, alpha, initialTao, base, trainTime

        measureLog.info(dataName + " " + coreNum);

        for (int j = 0; j < alphaStep.length; j++) {
            double alphaUser = alphaStep[j];
//            System.out.println(alphaUser);
            for (int k = 0; k < initialTaoStep.length; k++) {
                double initialTao = initialTaoStep[k];
                for (int m = 0; m < baseStep.length; m++) {
                    double base = baseStep[m];
                    for (int i = 0; i < dstep.length; i++) {
                        measureLog.info("start： " + new Date());
                        double d = dstep[i];
                        double alpahItem = alphaUser;
                        /**
                         * 将数据集和参数配置写入allLog
                         */
                        allLog.info(dataName + " " + coreNum);
                        allLog.info("d " + d + " alphaUser " + alphaUser + " initialTao " + initialTao + " " + base);

                        TAPITF TAPITF = new TAPITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample, d, base, initialTao, alphaUser, alpahItem);
                        TAPITF.run();

                        eachMeasureList = TAPITF.getMeasureList();
                        eachParas = new double[]{d, alphaUser, initialTao, base, TAPITF.getAverageTrainTime()};

                        //eachMeasureList是每套参数下的最终结果，参数设置和相应的结果需要写入Log文件
                        printMeasureList(eachMeasureList, eachParas);


                        if (eachMeasureList.get(2).get(4) > maxF1) {//2表示是F1这个denseVector, 4表示是top5
                            maxF1 = eachMeasureList.get(2).get(4);
                            bestMeasureList = eachMeasureList;
                            bestParas[0] = d;
                            bestParas[1] = alphaUser;
                            bestParas[2] = initialTao;
                            bestParas[3] = base;
                            bestParas[4] = TAPITF.getAverageTrainTime();
                        }

//                        measureLog.info("以上结果的参数：d " + d + " alphaUser " + alphaUser + " initialTao " + initialTao + " averageTrainTime " + TAPITF.getAverageTrainTime());
                        measureLog.info("end: " + new Date());
                    }
                }
            }
        }

        measureLog.info(dataName + " " + coreNum + " best performance on F1@5");
        printMeasureList(bestMeasureList, bestParas);

    }

    /**
     * 将每个参数下的运行结果以及最好的运行结果写入Log文件中
     *
     * @param measureList
     * @param paras
     */
    public static void printMeasureList(List<DenseVector> measureList, double[] paras) {

        measureLog.info("参数：d " + paras[0] + " alphaUser " + paras[1] + " initialTao " + paras[2] + " base " + paras[3]);
        measureLog.info("average Train Time: " + paras[4] + "s");

        StringBuilder string = new StringBuilder();

        string.append(String.format("%-15s\t", "Top N"));

        for (int index = 0; index < 10; index++) {
            string.append(String.format("%-6d\t", (index + 1)));
        }
        measureLog.info(string.toString());

        string.setLength(0);
        string.append(String.format("%-15s\t","Precisions"));
        DenseVector precisions = measureList.get(0);

        for (int index = 0; index < 10; index++) {

            string.append(String.format("%.4f\t", precisions.get(index)));
        }
        measureLog.info(string.toString());

        string.setLength(0);
        string.append(String.format("%-15s\t", "Recalls"));

        DenseVector recalls = measureList.get(1);
        for (int index = 0; index < 10; index++) {
            string.append(String.format("%.4f\t" , recalls.get(index)));
        }


        measureLog.info(string.toString());
        string.setLength(0);
        string.append(String.format("%-15s\t", "F1"));

        DenseVector f1 = measureList.get(2);
        for (int index = 0; index < 10; index++) {
            string.append(String.format("%.4f\t" , f1.get(index)));
        }
        measureLog.info(string.toString());

        measureLog.info(String.format("%-15s\t" ,"AIP@10"));

        measureLog.info(String.format("%.4f\t" , measureList.get(3).get(0)));

        measureLog.info(String.format("%-15s\t" , "AILD@10"));

        measureLog.info(String.format("%.4f\t" , measureList.get(4).get(0)));

        measureLog.info("==============================================================================================");
    }
}
