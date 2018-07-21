package test;

import core.CAPITF;
import core.PITF;
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
 * Created by motu on 2018/7/18.
 */
public class CAPITFTest {
    private static Log allLog;

    private static Log measureLog;
    public static void main(String[] args) throws IOException {

//        String dataName = "Dianping";
//        String fileName = "checkin_pitf";
//        List<Post> trainPosts = DataReader.readPost("./demo/data/" + dataName +  "/"+fileName+"_train");
//        List<Post> testPosts = DataReader.readPost("./demo/data/" + dataName + "/"+fileName+"_test");

        String dataName ="DianpingCheckintrue0.3_false1510";//字段顺序正确
        String fileName = "UTP";
        String rootpath="./demo/data/"  + fileName + "/";
        String trainPath=rootpath + dataName  + "_train.txt";
        String testPath=rootpath + dataName  + "_test.txt";
//        String[] trainPaths = new String[]{trainPath};
//        String[] testPaths = new String[]{testPath};
        /**
         * loginfo
         */
        String className = "CAPITF";
        System.setProperty("log.dir", "./demo/Log");
        System.setProperty("alllog.info.file", className + "_" + dataName + "_All(3).log");
        System.setProperty("measurelog.info.file", className + "_" + dataName + "_Measure(3).log");
        allLog = LogFactory.getLog("alllog");
        measureLog = LogFactory.getLog("measurelog");
        allLog.info(className);
        measureLog.info(className);
        allLog.info("trainPath\t" + trainPath);
        allLog.info("testPath\t" + testPath);
        measureLog.info("trainPath\t" + trainPath);
        measureLog.info("testPath\t" + testPath);

//        int dim = 64;
//        double learnRate = 0.01;
//        double regU = 0.01;
//        double regI = regU, regT = regU;
        double initStdev = 0.01;
        int iter = 1;
        int numSample = 100;
        int randomSeed = 1;
//        int noiseRate =5;//噪音条数？？若knn生成是否还需要？？？for测试

        double[]regUStep = new double[]{0.01,0.015};//细粒度 0.005,0.0005
//        double[] regUStep = new double[]{0.001, 0.1};//粗粒度 0.5
        int[] dimStep = new int[]{64,128};//32
        double[] lrateStep = new double[]{0.01};//0.02,
        int[] noiseRate =new int[]{1,5,10};


/*        List<Post> trainPosts = DataReader.readPostUTP(trainPath);
        List<Post> testPosts = DataReader.readPostUTP(testPath);
        CAPITF capitf = new CAPITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample,noiseRate);
        capitf.run();//这个函数会调用model的初始化及创建函数
        System.out.println("dataName:\t" + dataName);*/

        double maxF1 = 0.0;
        List<DenseVector> bestMeasureList = new ArrayList<>();
        List<DenseVector> eachMeasureList = new ArrayList<>();
        double[] bestParas = new double[5];//包括reg, dimension,lrate,trainTime
        double[] eachParas = new double[5];


        measureLog.info(dataName + " ");
        String header = "startTime\tendTime\taverageTrainTime\tlearnRate\tregU\tdimension\titer";
        for (int i = 1; i <= 10; i++) {
            header += "\tPre@" + i + "\tRec@" + i + "\tF1@" + i;
        }
        header += "\tAIP@10\tAILD@10";
        measureLog.info(header);
        Date allStartTime = new Date();
//        for (int k = 0; k < trainPaths.length; k++) {
            for (int i = 0; i < regUStep.length; i++)
                for (int j = 0; j < dimStep.length; j++) {
                    for (int l = 0; l < lrateStep.length; l++) {
                        for(int r=0;r<noiseRate.length;r++) {
                            Date startTime = new Date();
                            double regI = regUStep[i], regT = regUStep[i], regU = regUStep[i];
                            int dim = dimStep[j];
                            double learnRate = lrateStep[l];
                            int noise = noiseRate[r];
                            System.out.println("dataset " + dataName + " classname " + className);
                            System.out.println(" dim " + dim + " initStdev " + initStdev + " iter " + iter + " learnRate "
                                    + learnRate + " regU regI regT " + regU + " " + regI + " " + regT + " randomseed " + randomSeed + " numSample " + numSample);
                            /**
                             * 将数据集和参数配置写入allLog
                             */
                            allLog.info(dataName + " ");
                            allLog.info(trainPath + " " + testPath + " dataset " + dataName + " initStdev " + initStdev + "\n"
                                    + " learn Rate " + learnRate + " regU " + regU + " dim " + dim + " iter " + iter + "\n" + " randomseed " + randomSeed + " numSample " + numSample + "\n");
//                        List<Post> trainPosts = DataReader.readPostUTP(trainPaths[k]);
//                        List<Post> testPosts = DataReader.readPostUTP(testPaths[k]);
                            List<Post> trainPosts = DataReader.readPostUTP(trainPath);
                            List<Post> testPosts = DataReader.readPostUTP(testPath);

                            CAPITF capitf = new CAPITF(trainPosts, testPosts, dim, initStdev, iter, learnRate, regU, regI, regT, randomSeed, numSample, noise);
                            capitf.run();//这个函数会调用model的初始化及创建函数
                            eachMeasureList = capitf.getMeasureList();
                            eachParas = new double[]{capitf.getAverageTrainTime(), learnRate, regU, dim, iter};

                            //eachMeasureList是每套参数下的最终结果，参数设置和相应的结果需要写入Log文件
                            printMeasureList(eachMeasureList, eachParas);

                            if (eachMeasureList.get(2).get(4) > maxF1) {//2表示是F1这个denseVector, 4表示是top5
                                maxF1 = eachMeasureList.get(2).get(4);
                                bestMeasureList = eachMeasureList;
                                bestParas[0] = capitf.getAverageTrainTime();
                                bestParas[1] = learnRate;
                                bestParas[2] = regU;
                                bestParas[3] = dim;
                                bestParas[4] = iter;
                            }

                            measureLog.info("end: " + new Date());
//                        String timeLog = startTime.toString() + "\t" + endTime.toString();
//                        Utils.printMeasureList(measureLog, eachMeasureList, eachParas, timeLog);
                        }
                    }
                }
//        }
        measureLog.info(dataName + " best performance on F1@5");
        PITFUTPTuning.printMeasureList(bestMeasureList, bestParas);

    }
    public static void printMeasureList(List<DenseVector> measureList, double[] paras) {
        //PITF.getAverageTrainTime(),learnRate, regU, dim, iter
        measureLog.info("参数:"+" learnRate " + paras[1] + " regU " + paras[2] + " dim " + paras[3]+"iter"+paras[4]);
        measureLog.info("average Train Time: " + paras[0] + "s");

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

