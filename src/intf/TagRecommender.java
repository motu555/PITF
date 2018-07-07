package intf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.DenseVector;
import data.Post;
import org.apache.commons.logging.LogFactory;
import util.Lists;
import util.Randoms;

import java.io.IOException;
import java.util.*;

/**
 * Created by wangkeqiang on 2016/5/14.
 * 2018.02.09
 * 发现了我的PITF等baseline的结果与phw的代码不同，原因是因为在eveluate的时候我把不在train中的item过滤掉了
 * 实际上按照librec的思想是没有必要过滤掉的
 * 因为这些item在train中虽然没有训练到，但可以用初始化的参数来参与预测
 * <p>
 * 评价指标AIP  和 AILD的论文：
 * Exploiting Novelty and Diversity in Tag Recommendation
 * AIP是新颖性指标、AILD是多样性指标，这里我们只关注新颖性
 */
public class TagRecommender implements Runnable {
    protected List<Post> trainPosts;
    protected List<Post> testPosts;

    protected int numRates;
    protected int randomSeed;
    protected int numUser, numItem, numTag;
    protected Table<Integer, Integer, Set<Integer>> testTagSet;


    protected int topN = 10;
    protected long trainStartTime, trainEndTime, evaStartTime, evaEndTime;

    protected Map<Integer, Map<Integer, Integer>> itemTagOccurrenceMap = new HashMap<>(); //<resource, tag, frequency>
    protected double norDisc = 0.0d;
    protected double norDist = 0.0d;
    protected Set<Integer> trainItemSet;
    protected Map<Integer, Set<Integer>> tagItemSet;
    protected final static double log2 = Math.log(2.0);

    protected String params;
    protected String[] tempList;

    protected String outputPath;



//    private static org.apache.commons.logging.Log allLog = MeasureLogs.getInstance("alllog");
    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    /**
     * 代码中统计的numUser,numItem,numTag不是真实数目，是train集合中这三个维度上id的最大值
     *
     * @param trainPostsParam
     * @param testPostsParam
     * @param randomSeedParam
     */
    public TagRecommender(List<Post> trainPostsParam, List<Post> testPostsParam,
                          int randomSeedParam) throws IOException {
        /**
         * 配置Log
         */

//        System.setProperty("alllog.dir", "./demo/Log/");
//        System.setProperty("alllog.info.file", "allResult.log");

        this.trainPosts = trainPostsParam;
        this.testPosts = testPostsParam;

        this.numRates = this.trainPosts.size();
        this.randomSeed = randomSeedParam;

        Randoms.seed(this.randomSeed);
        this.numUser = 0;
        this.numItem = 0;
        this.numTag = 0;
        this.testTagSet = HashBasedTable.create();
        this.trainItemSet = new HashSet<>();
        tagItemSet = new HashMap<>();
        for (Post trainPost : this.trainPosts) {
            int user = trainPost.getUser();
            int item = trainPost.getItem();
            int tag = trainPost.getTag();
            trainItemSet.add(item);

            this.numUser = this.numUser > user ? this.numUser : user;
            this.numItem = this.numItem > item ? this.numItem : item;
            this.numTag = this.numTag > tag ? this.numTag : tag;//numTag只是表示当前数据集中最大的tagid

            if (!itemTagOccurrenceMap.containsKey(item)) {
                itemTagOccurrenceMap.put(item, new HashMap<>());
            }
            if (!itemTagOccurrenceMap.get(item).containsKey(tag)) {
                itemTagOccurrenceMap.get(item).put(tag, 0);
            }
            itemTagOccurrenceMap.get(item).put(tag, itemTagOccurrenceMap.get(item).get(tag) + 1);

            if (!tagItemSet.containsKey(tag)) {
                this.tagItemSet.put(tag, new HashSet<>());
            }
            this.tagItemSet.get(tag).add(item);
        }

        for (Post testPost : testPostsParam) {
            int user = testPost.getUser();
            int item = testPost.getItem();
            int tag = testPost.getTag();

            this.numUser = this.numUser > user ? this.numUser : user;
            this.numItem = this.numItem > item ? this.numItem : item;
            this.numTag = this.numTag > tag ? this.numTag : tag;

            if (!this.testTagSet.contains(user, item)) {
                this.testTagSet.put(user, item, new HashSet<>());
            }
            this.testTagSet.get(user, item).add(tag);
        }

        this.numUser++;
        this.numItem++;
        this.numTag++;

        for (int i = 1; i <= this.topN; i++) {
            this.norDisc += log2 / Math.log(1.0 + i);
        }
        this.norDist = (this.topN * this.topN - this.topN) / 2.0;
    }

    public void run() {
        try {
            execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * execution method of a recommender
     */
    public void execute() throws Exception {
        this.initModel();

        this.buildModel();

        /**
         * buildModel中每一次迭代已经输出评价指标了，这里输出的与模型最后一次的输出是一样的
         */
        this.evaluate();

        this.postModel();
    }

    /**
     * initilize recommender model
     */
    protected void initModel() throws Exception {
    }

    /**
     * Learning method: override this method to build a model, for a model-based method. Default implementation is
     * useful for memory-based methods.
     */
    protected void buildModel() throws Exception {
    }

    /**
     * After learning model: release some intermediate data to avoid memory leak
     */
    protected void postModel() throws Exception {
    }


    protected double predict(int user, int item, int tag) throws Exception {
        return 0.0;
    }

    /**
     * evaluate的结果需要写入Log文件
     *
     * @throws Exception
     */
    protected List<DenseVector> evaluate() throws Exception {
        List<DenseVector> measureList = new ArrayList<>();
        this.evaStartTime = System.currentTimeMillis();
        DenseVector precisions = new DenseVector(this.topN);
        DenseVector recalls = new DenseVector(this.topN);
        DenseVector AIPs = new DenseVector(1);
        DenseVector AILDs = new DenseVector(1);
        double AIP = 0.0d;
        double AILD = 0.0d;

        StringBuilder measureData = new StringBuilder();
        int count = 0;
        for (Table.Cell<Integer, Integer, Set<Integer>> tempCell : this.testTagSet.cellSet()) {

            int user = tempCell.getRowKey();
            int item = tempCell.getColumnKey();

            Set<Integer> tags = tempCell.getValue();
            List<Map.Entry<Integer, Double>> itemScores = this.predictTopN(user, item);

            AILD += this.diversity(itemScores);

            int number = 0;
            int tagsNum = tags.size();

            for (int index = 0; index < this.topN; index++) {

                if (index < itemScores.size()) {
                    int tag = itemScores.get(index).getKey();
                    if (tags.contains(tag)) {
                        number++;
                    }
                    precisions.set(index, precisions.get(index) + 1.0 * number / (index + 1));
                    recalls.set(index, recalls.get(index) + 1.0 * number / tagsNum);

                } else {
                    precisions.set(index, precisions.get(index) + 1.0 * number / (index + 1));
                    recalls.set(index, recalls.get(index) + 1.0 * number / tagsNum);
                }
            }

            Double tempAIP = 0.0d;
            double maxiff = Double.MIN_VALUE;
            //由AIP的计算公式所决定
            if (!this.trainItemSet.contains(item)) {
                tempAIP = 1.0;
            } else {
                for (int index = 0; index < this.topN; index++) {
                    int tag = itemScores.get(index).getKey();
                    int tagCount = 0;
                    if (this.itemTagOccurrenceMap.get(item).containsKey(tag)) {
                        tagCount = this.itemTagOccurrenceMap.get(item).get(tag);
                    }
                    double tempiff = Math.log((itemTagOccurrenceMap.get(item).size() + 1.0) / (tagCount + 1.0)) / log2;//换底公式，以2为底
                    tempAIP += log2 / Math.log(1.0 + index + 1.0) * tempiff;
                    maxiff = maxiff > tempiff ? maxiff : tempiff;
                }
                tempAIP /= (this.norDisc * maxiff);
            }

            if (tempAIP.isInfinite() || tempAIP.isNaN()) {
                tempAIP = 1.0;
            }

            AIP += tempAIP;

            count++;
        }


        AIP /= count;
        AILD /= (count * this.norDist);
        precisions.scaleSelf(1.0 / count);
        recalls.scaleSelf(1.0 / count);
        DenseVector f1 = precisions.times(recalls).scale(2.0).divide(precisions.add(recalls));

        AIPs.set(0, AIP);
        AILDs.set(0, AILD);

        this.evaEndTime = System.currentTimeMillis();
        allLog.info("Train Time\t" + (trainEndTime - trainStartTime) * 1.0 / 1000 + "s");
        measureData.append("Train Time:\t" + (trainEndTime - trainStartTime) * 1.0 / 1000 + "s");
        measureData.append("Evaluate Time\t" + (evaEndTime - evaStartTime) * 1.0 / 1000 + "s\n");
        allLog.info("Evaluate Time\t" + (evaEndTime - evaStartTime) * 1.0 / 1000 + "s");
        StringBuilder string = new StringBuilder();
        string.append(String.format("%-15s\t", "Top N"));
        measureData.append("Top N ");
        for (int index = 0; index < this.topN; index++) {
            string.append(String.format("%-6d\t", index + 1));
            measureData.append((index + 1) + "\t");
        }
        allLog.info(string.toString());
        measureData.append("\n");

        string.setLength(0);
        string.append(String.format("%-15s\t", "Precisions"));
        measureData.append("Precisions ");
        for (int index = 0; index < this.topN; index++) {
            string.append(String.format("%.4f\t", precisions.get(index)));
            measureData.append(precisions.get(index) + "\t");
        }
        allLog.info(string.toString());
        measureData.append("\n");

        string.setLength(0);
        string.append(String.format("%-15s\t", "Recalls"));

        measureData.append("Recalls ");
        for (int index = 0; index < this.topN; index++) {
            string.append(String.format("%.4f\t", recalls.get(index)));
            measureData.append(recalls.get(index) + "\t");
        }

        allLog.info(string.toString());
        measureData.append("\n");
        /***********************************/

        string.setLength(0);
        string.append(String.format("%-15s\t", "F1"));
        measureData.append("F1 ");
        for (int index = 0; index < this.topN; index++) {
            string.append(String.format("%.4f\t", f1.get(index)));
            measureData.append(f1.get(index) + "\t");
        }

        allLog.info(string.toString());
        measureData.append("\n");
        allLog.info(String.format("%-15s\t", "AIP@" + this.topN));
        measureData.append("AIP@" + this.topN);
        allLog.info(String.format("%.4f\t", AIP));
        measureData.append("\t" + AIP);

        measureData.append("\n");
        allLog.info(String.format("%-15s\t", "AILD@" + this.topN));
        measureData.append("AILD@" + this.topN);
        allLog.info(String.format("%.4f\t", AILD));
        measureData.append("\t" + AILD);

        measureData.append("\n");
        allLog.info("==============================================================================================");
        //save recomendation result

//        try {
//            FileUtil.writeString(outputPath, measureData.toString(),true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        measureList.add(precisions);
        measureList.add(recalls);
        measureList.add(f1);
        measureList.add(AIPs);
        measureList.add(AILDs);

        return measureList;
    }

    protected List<Map.Entry<Integer, Double>> predictTopN(int user, int item) throws Exception {
        List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>();
        for (int tag = 0; tag < this.numTag; tag++) {
            double rank = this.predict(user, item, tag);
            itemScores.add(new AbstractMap.SimpleImmutableEntry<>(tag, rank));
        }
        Lists.sortList(itemScores, true);
        return itemScores;
    }


    protected double diversity(List<Map.Entry<Integer, Double>> itemScores) {
        double diver = 0.0d;
        for (int index = 0; index < this.topN; index++) {
            for (int j = index + 1; j < this.topN; j++) {
                diver += 1.0 - this.jaccard(itemScores.get(index).getKey(), itemScores.get(j).getKey());
            }
        }
        return diver;
    }

    protected double jaccard(int tag1, int tag2) {
        if (this.tagItemSet.containsKey(tag1) && this.tagItemSet.containsKey(tag2)) {
            Set<Integer> set1 = this.tagItemSet.get(tag1);
            Set<Integer> set2 = this.tagItemSet.get(tag2);
            int intersectNum = 0;
            for (int elem : set1) {
                if (set2.contains(elem)) {
                    intersectNum++;
                }
            }
            return 1.0 * intersectNum / (set1.size() + set2.size() - intersectNum);
        } else {
            return 0.0d;
        }
    }
    protected static double loss(double x) {
        return 0.0;
    }
}
