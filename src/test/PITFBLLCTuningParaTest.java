package test;

import core.PITFBLLC;
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
 * Created by wangkeqiang on 2016/5/15.
 */
public class PITFBLLCTuningParaTest {
    private static Log allLog;
    private static Log measureLog;

    public static void main(String[] args) throws IOException {
        String className = "PITFBLLC";
        String dataName = "lastFM";
        int coreNum = 1;

//        allLog = MeasureLogs.newInstance("alllog", className, dataName, coreNum);
//        System.setProperty("log.dir", "./demo/Log");
//        System.setProperty("alllog.info.file", className + "_" + dataName + coreNum + "_all.log");

        allLog = LogFactory.getLog("alllog");

//        System.setProperty("measurelog.info.file", className + "_" + dataName + coreNum + "_measure.log");
        measureLog = LogFactory.getLog("measurelog");

        allLog.info(className);
        measureLog.info(className);

//        measureLog = MeasureLogs.newInstance("measurelog", className, dataName, coreNum);

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
        double d = 0.5;
        double alphaUser = 1.2;
        double alpahItem = 1.2;

        double maxF1 = 0.0;
        List<DenseVector> bestMeasureList = new ArrayList<>();
        List<DenseVector> eachMeasureList = new ArrayList<>();
        double[] bestParas = new double[3];//包括d, alpha, trainTime
        double[] eachParas = new double[3];


        measureLog.info(dataName + " " + coreNum);
        allLog.info(dataName + " " + coreNum);


//        double[]alphaStep = new double[]{0.0, 0.2, 0.4, 0.6, 0.8};
        double[]alphaStep = new double[]{1.2};
//        double[]alphaStep = new double[]{1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
        for(double alpha : alphaStep) {
            measureLog.info("start " + new Date());
            alphaUser = alpahItem = alpha;
            measureLog.info("alpha\t" + alphaUser + "\td\t" + d);
            allLog.info("alpha\t" + alphaUser + "\td\t" + d);
            PITFBLLC pitfbllc = new PITFBLLC(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample, d, alphaUser, alpahItem);
            pitfbllc.run();
            measureLog.info("end " + new Date());

            eachMeasureList = pitfbllc.getMeasureList();
            eachParas = new double[]{d, alphaUser,pitfbllc.getAverageTrainTime()};

            //eachMeasureList是每套参数下的最终结果，参数设置和相应的结果需要写入Log文件
            printMeasureList(eachMeasureList, eachParas);

            if (eachMeasureList.get(2).get(4) > maxF1) {//2表示是F1这个denseVector, 4表示是top5
                maxF1 = eachMeasureList.get(2).get(4);
                bestMeasureList = eachMeasureList;
                bestParas[0] = d;
                bestParas[1] = alphaUser;
                bestParas[2] = pitfbllc.getAverageTrainTime();
            }

            measureLog.info("end: " + new Date());
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

        measureLog.info("参数：d " + paras[0] + " alphaUser " + paras[1]);
        measureLog.info("average Train Time: " + paras[2] + "s");

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
