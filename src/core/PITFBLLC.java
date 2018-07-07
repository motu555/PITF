package core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.DenseMatrix;
import data.KeyValue;
import data.Post;
import intf.TagMFRecommender;
import org.apache.commons.logging.LogFactory;
import util.Lists;
import util.Randoms;

import java.io.IOException;
import java.util.*;

/**
 * Created by wangkeqiang on 2016/5/14.
 * 之前NDBC的版本代码PITFBLLC有错误：
 * 在做发现有两处用了log的地方，
 * 已经改为log10后与原来结果不用，但结果相差不是不大
 * ##PITFBLLC和TAPITF的区别在于PITFBLLC中建模时间使用的是幂函数，只能累加，不能递归简化计算
 * ##PITFBLLC中使用的时间间隔的单位是ms, 而TAPITF中使用的是天
 *
 */
public class PITFBLLC extends TagMFRecommender {
    protected int numSample;
    protected Table<Integer, Integer, Set<Integer>> trainTagSet;
    protected List<Set<Integer>> userTagSet, itemTagSet;

    double d = 0.1;
    int userTagWeightNum = 0;
    int userTagWeightIterNum = 0;

    double averageTrainTime = 0.0;


    List<Map<Integer, Double>> userTagPrefList;
    List<Map<Integer, Double>> itemTagPrefList;

    double alphaUser = 3.0;
    double alphaItem = 3.0;

    List<List<KeyValue<Integer, Long>>> userTagRankTimeList;
    List<Double> userTagTrainWeightList;
    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    public PITFBLLC(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                    double learnRateParam, double regUParam, double regIParam, double regTParam,
                    int randomSeedParam, int numSampleParam, double dParam, double alphaUserParam, double alphaItemParam) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);
        this.numSample = numSampleParam;
        this.d = dParam;
        this.alphaUser = alphaUserParam;
        this.alphaItem = alphaItemParam;
    }

    public PITFBLLC(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                    double learnRateParam, double regUParam, double regIParam, double regTParam,
                    int randomSeedParam, int numSampleParam, double dParam, double alphaUserParam, double alphaItemParam, String params, String[] tempList) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);
        this.params = params;
        this.tempList = tempList;
        this.numSample = numSampleParam;
        this.d = dParam;
        this.alphaUser = alphaUserParam;
        this.alphaItem = alphaItemParam;
    }

    /**
     * initialize recommender model
     */
    protected void initModel() throws Exception {
        this.trainTagSet = HashBasedTable.create();

        this.userTagSet = new ArrayList<>();
        this.itemTagSet = new ArrayList<>();

        List<Map<Integer, List>> userTagTimeList = new ArrayList<>();
        List<Map<Integer, Integer>> itemTagCountList = new ArrayList<>();
        List<Long> userTimeList = new ArrayList<>();

        this.userTagPrefList = new ArrayList<>();
        this.itemTagPrefList = new ArrayList<>();

        this.userTagRankTimeList = new ArrayList<>();
        this.userTagTrainWeightList = new ArrayList<>();

        for (int userIndex = 0; userIndex < this.numUser; userIndex++) {
            this.userTagSet.add(new HashSet<>());

            userTagTimeList.add(new HashMap<>());
            userTimeList.add(0L);
            this.userTagPrefList.add(new HashMap<>());
            this.userTagRankTimeList.add(new ArrayList<>());
        }

        for (int itemIndex = 0; itemIndex < this.numItem; itemIndex++) {
            this.itemTagSet.add(new HashSet<>());

            itemTagCountList.add(new HashMap<>());
            this.itemTagPrefList.add(new HashMap<>());
        }

        for (Post post : this.trainPosts) {
            int user = post.getUser();
            int item = post.getItem();
            int tag = post.getTag();
            long time = post.getTime();

            if (!this.trainTagSet.contains(user, item)) {
                this.trainTagSet.put(user, item, new HashSet<>());
            }
            this.trainTagSet.get(user, item).add(tag);

            this.userTagSet.get(user).add(tag);
            this.itemTagSet.get(item).add(tag);

            if (time > userTimeList.get(user)) {
                userTimeList.set(user, time);
            }
            if (!userTagTimeList.get(user).containsKey(tag)) {
                userTagTimeList.get(user).put(tag, new ArrayList<Long>());
            }
            userTagTimeList.get(user).get(tag).add(time);

            if (!itemTagCountList.get(item).containsKey(tag)) {
                itemTagCountList.get(item).put(tag, 1);
            } else {
                itemTagCountList.get(item).put(tag, itemTagCountList.get(item).get(tag) + 1);
            }

            this.userTagRankTimeList.get(user).add(new KeyValue(tag, time));
        }

        for (Post post : this.testPosts) {
            int user = post.getUser();
            long time = post.getTime();

            if (time > userTimeList.get(user)) {
                userTimeList.set(user, time);
            }
        }

        for (Post post : this.trainPosts) {
            int user = post.getUser();
            int tag = post.getTag();
            long time = post.getTime();
            this.userTagTrainWeightList.add(this.calUserTagWeight(user, tag, time));
        }
        for (int user = 0; user < this.numUser; user++) {
            double nor = 0.0d;
            long preTime = userTimeList.get(user);
            Map<Integer, List> tempTagsTimeMap = userTagTimeList.get(user);

            for (Map.Entry<Integer, List> tempTagsTime : tempTagsTimeMap.entrySet()) {
                int tag = tempTagsTime.getKey();
                List<Long> tempTimeList = tempTagsTime.getValue();
                double tempValue = 0.0;
                /**
                 * 计算预测时的权重时，这里如果preTime == 最后一个tempTime时，依然会加上一个常数
                 * 1000^-d这个值
                 */
                for (long tempTime : tempTimeList) {
                    tempValue += Math.pow(1.0 * (preTime - tempTime) + 1000.0, -this.d);
                }
                nor += tempValue;
                this.userTagPrefList.get(user).put(tag, tempValue);
            }

            for (int tag : tempTagsTimeMap.keySet()) {
                this.userTagPrefList.get(user).put(tag, 1.0 + Math.log10(1.0 + Math.pow(10, alphaItem) * this.userTagPrefList.get(user).get(tag) / nor));
            }
        }

        for (int item = 0; item < this.numItem; item++) {
            double nor = 0.0;
            Map<Integer, Integer> tempTagsCountMap = itemTagCountList.get(item);
            for (Map.Entry<Integer, Integer> tempTagsCount : tempTagsCountMap.entrySet()) {
                int tag = tempTagsCount.getKey();
                int count = tempTagsCount.getValue();
                double tempValue = count;
                nor += tempValue;
                this.itemTagPrefList.get(item).put(tag, tempValue);
            }

            for (int tag : tempTagsCountMap.keySet()) {
                this.itemTagPrefList.get(item).put(tag, 1.0 + Math.log10(1.0 + Math.pow(10, alphaItem) * this.itemTagPrefList.get(item).get(tag) / nor));
            }
        }


        this.userVectors = new DenseMatrix(this.numUser, this.dim);
        this.itemVectors = new DenseMatrix(this.numItem, this.dim);
        this.tagUserVectors = new DenseMatrix(this.numTag, this.dim);
        this.tagItemVectors = new DenseMatrix(this.numTag, this.dim);

        this.userVectors.init(0.0, this.initStdev);
        this.itemVectors.init(0.0, this.initStdev);
        this.tagUserVectors.init(0.0, this.initStdev);
        this.tagItemVectors.init(0.0, this.initStdev);

        int avgTag = 0;
        for (Set<Integer> tagSet : this.userTagSet) {
            avgTag += tagSet.size();
        }
    }

    @Override
    public void buildModel() throws Exception {
        long num_draws_per_iter = this.numRates * this.numSample;
        for (int iter_index = 0; iter_index < this.iter; iter_index++) {
            allLog.info(String.format("%-15s\t", "Iter:" + iter_index));
            allLog.info(new Date());
            userTagWeightNum = 0;
            userTagWeightIterNum = 0;
            this.trainStartTime = System.currentTimeMillis();
            for (int sample = 0; sample < num_draws_per_iter; sample++) {
                int post_index = Randoms.uniform(this.numRates);
                Post tempPost = this.trainPosts.get(post_index);
                int user = tempPost.getUser();
                int item = tempPost.getItem();
                int tag = tempPost.getTag();
                long time = tempPost.getTime();

                double userTagWeight = this.userTagTrainWeightList.get(post_index);
                double itemTagWeight = this.calItemTagWeight(item, tag);

                double x_uit = this.predictTrain(user, item, tag, userTagWeight, itemTagWeight);

                int neg_tag = this.drawSample(user, item);
                double userNegTagWeight = this.calUserTagWeight(user, neg_tag, time);
                double itemNegTagWeight = this.calItemTagWeight(item, neg_tag);

                double x_uint = this.predictTrain(user, item, neg_tag, userNegTagWeight, itemNegTagWeight);
                double normalizer = loss(x_uit - x_uint);

                for (int k = 0; k < this.dim; k++) {
                    double u_k = this.userVectors.get(user, k);
                    double i_k = this.itemVectors.get(item, k);
                    double t_p_u_f = this.tagUserVectors.get(tag, k);
                    double t_p_i_f = this.tagItemVectors.get(tag, k);
                    double t_n_u_f = this.tagUserVectors.get(neg_tag, k);
                    double t_n_i_f = this.tagItemVectors.get(neg_tag, k);

                    this.userVectors.set(user, k, u_k + this.learnRate * (normalizer * (t_p_u_f * userTagWeight - t_n_u_f * userNegTagWeight) - this.regU * u_k));
                    this.itemVectors.set(item, k, i_k + this.learnRate * (normalizer * (t_p_i_f * itemTagWeight - t_n_i_f * itemNegTagWeight) - this.regI * i_k));
                    this.tagUserVectors.set(tag, k, t_p_u_f + this.learnRate * (normalizer * u_k * userTagWeight - this.regT * t_p_u_f));
                    this.tagUserVectors.set(neg_tag, k, t_n_u_f + this.learnRate * (normalizer * (-u_k) * userNegTagWeight - this.regT * t_n_u_f));
                    this.tagItemVectors.set(tag, k, t_p_i_f + this.learnRate * (normalizer * i_k * itemTagWeight - this.regT * t_p_i_f));
                    this.tagItemVectors.set(neg_tag, k, t_n_i_f + this.learnRate * (normalizer * (-i_k) * itemNegTagWeight - this.regT * t_n_i_f));
                }
            }
            this.trainEndTime = System.currentTimeMillis();
            averageTrainTime += (this.trainEndTime - this.trainStartTime) * 1.0 / 1000;
            measureList = evaluate();
        }
        averageTrainTime /= this.iter;
    }

    public double getAverageTrainTime (){
        return averageTrainTime;
    }

    protected int drawSample(int user, int item) {
        int tag;
        Set<Integer> tagSet = this.trainTagSet.get(user, item);
        do {
            tag = Randoms.uniform(this.numTag);
        } while (tagSet.contains(tag));
        return tag;
    }

    protected static double loss(double x) {
        double expX = Math.exp(-x);
        return expX / (1 + expX);
    }

    @Override
    protected double predict(int user, int item, int tag) {
        double userTagWeight = 1.0;
        double itemTagWeight = 1.0;
        if (this.userTagPrefList.get(user).containsKey(tag)) {
            userTagWeight = this.userTagPrefList.get(user).get(tag);
        }
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            itemTagWeight = this.itemTagPrefList.get(item).get(tag);
        }
        double value = userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
        return value;
    }

    protected double predictTrain(int user, int item, int tag, double userTagWeight, double itemTagWeight) {
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
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

    /**
     * PITFBLLC中时间权重的计算不是递归的，所以计算train中的user-tag weight和
     * 计算user-negative weight的方法是一样的，训练时的negative-tag-weight不能利用cache提前计算
     * train中time 权重计算只取给定时间戳之前的，度量给定时间戳之前发生的事件对当前时间的影响
     * @param user
     * @param tag
     * @param time
     * @return
     */
    private double calUserTagWeight(int user, int tag, long time) {
        double weight = 0.0d;
        double nor = 0.0;
        if (this.userTagSet.get(user).contains(tag)) {
            userTagWeightIterNum++;
            for (KeyValue<Integer, Long> tagsTime : this.userTagRankTimeList.get(user)) {
                double tagTime = tagsTime.getValue();
                if (tagTime < time) {
                    userTagWeightNum++;
                    double value = Math.pow(1.0 * (time - tagTime) + 1000.0, -this.d);
                    nor += value;
                    if (tag == tagsTime.getKey()) {
                        weight += value;
                    }
                } else {
                    break;
                }
            }
            /**
             * 分母是有可能为0的，当时间戳<=用户最早使用这个tag的时间戳
             */
            if (nor != 0.0) {
                weight /= nor;
                weight = 1.0 + Math.log10(1 + Math.pow(10, this.alphaUser) * weight);
            } else {
                weight = 1.0;
            }
        } else {
            //用户根本就没有使用过这个tag
            weight = 1.0;
        }
        return weight;
    }

    private double calItemTagWeight(int item, int tag) {
        double weight = 1.0;
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            weight = this.itemTagPrefList.get(item).get(tag);
        }
        return weight;
    }
}
