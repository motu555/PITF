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
 * ## modified by wkq on 2017/9/10
 * ## debug by jyy on 2018/4/3
 * <p>
 * 对应论文中的模型 TAPITF
 * 关于计算w(u,ta,sa)的注意点：
 * 1、计算预测时的w(u,ta,sa)权重时，当predictTime=LastTime时，tao这个值取的是1 + tao(lastTime)，
 * 与PITFBLLC类似，多取了一个常数1。认为训练集中同时发生的事件对预测也有影响
 * //TODO 可以做实验比较加不加1是否有影响
 * 2。计算训练时的w(u,ta,sa)权重时，当sa在用户使用tag的timelist中时，直接从相应数据结构中取taoStar(sa)+initialTao即可
 * 3.用户使用tag的时间序列的第一个值，对应的taoStar为0，因为前面没有其他行为
 * 4.用户在train中的最小时间对应的taosum为0，因为之前没有其他行为
 * 5.注意计算tao的时候需要taoStar + initialTao
 */
public class TAPITF extends TagMFRecommender {
    protected int numSample;
    /**
     * <useid, itemid, tagset>
     */
    protected Table<Integer, Integer, Set<Integer>> trainTagSet;
    /**
     * <userid, tagset>, <itemid, tagset>
     */
    protected List<Set<Integer>> userTagSet, itemTagSet;


    double d = 0.1;
    double base = 0.0;
    int userTagWeightNum = 0;
    int userTagWeightIterNum = 0;
    double initialTao = 1.0;
    double averageTrainTime = 0.0;
    double timeUnit = 3600000.0 * 24;

    /**
     * user, tag, preference score user-tag权重值
     * 预测时 user-tag的权重值
     */
    List<Map<Integer, Double>> predictUserTagPrefList;
    /**
     * item, tag, preference score  item-tag权重值
     */
    List<Map<Integer, Double>> itemTagPrefList;
    /**
     * user,tag, 用户使用tag的时间戳列表
     */
    List<Map<Integer, List<Long>>> userTagTimeList;
    List<Set<Integer>> userItemSet;

    double alphaUser = 3.0;
    double alphaItem = 3.0;

    List<List<KeyValue<Integer, Long>>> userTagRankTimeList, itemTagRankTimeList;
    /**
     * tao(u,t,s)= initialTao[初始强度值] + 在该时间点之前的行为对当前的影响tao_star
     * 这里计算的是能递归计算的第二部分 tao_star
     */
    List<Map<Integer, Map<Long, Double>>> trainUserTagTimeTaoStarList;
    /**
     * sum_tao(u,t,s)  归一化tao(u,t,s)的分母
     */
    List<Map<Long, Double>> tempUserTimeSum;
    /**
     * 训练集中w_u,ta,s的权重
     */
    List<Map<Integer, Map<Long, Double>>> userTagTrainWeightList;

    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    public TAPITF(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                  double learnRateParam, double regUParam, double regIParam, double regTParam,
                  int randomSeedParam, int numSampleParam, double dParam, double baseParam,
                  double initialTaoParam, double alphaUserParam, double alphaItemParam) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);
        this.numSample = numSampleParam;
        this.d = dParam;
        this.initialTao = initialTaoParam;
        this.alphaUser = alphaUserParam;
        this.alphaItem = alphaItemParam;
        this.base = baseParam;
    }

    public TAPITF(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                  double learnRateParam, double regUParam, double regIParam, double regTParam,
                  int randomSeedParam, int numSampleParam, double dParam, double alphaUserParam,
                  double alphaItemParam, String params, String[] tempList) throws IOException {
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

        userTagTimeList = new ArrayList<>();
        userItemSet = new ArrayList<>();
        List<Map<Integer, Integer>> itemTagCountList = new ArrayList<>();
        List<Long> userTimeList = new ArrayList<>();

        this.predictUserTagPrefList = new ArrayList<>();
        this.itemTagPrefList = new ArrayList<>();

        this.userTagRankTimeList = new ArrayList<>();
        this.userTagTrainWeightList = new ArrayList<>();

        this.trainUserTagTimeTaoStarList = new ArrayList<>();
        this.tempUserTimeSum = new ArrayList<>();

        for (int userIndex = 0; userIndex < this.numUser; userIndex++) {
            this.userTagSet.add(new HashSet<>());
            userTagTimeList.add(new HashMap<>());
            userItemSet.add(new HashSet<>());
            userTimeList.add(0L);
            this.predictUserTagPrefList.add(new HashMap<>());
            this.userTagRankTimeList.add(new ArrayList<>());
            this.trainUserTagTimeTaoStarList.add(new HashMap<>());
            this.tempUserTimeSum.add(new HashMap<>());
            this.userTagTrainWeightList.add(new HashMap<>());
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

            userItemSet.get(user).add(item);

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
                userTagTimeList.get(user).put(tag, new ArrayList<>());
            }
            userTagTimeList.get(user).get(tag).add(time);

            if (!itemTagCountList.get(item).containsKey(tag)) {
                itemTagCountList.get(item).put(tag, 1);
            } else {
                itemTagCountList.get(item).put(tag, itemTagCountList.get(item).get(tag) + 1);
            }

            this.userTagRankTimeList.get(user).add(new KeyValue<>(tag, time));
        }

        for (Post post : this.testPosts) {
            int user = post.getUser();
            long time = post.getTime();

            if (time > userTimeList.get(user)) {
                //找到测试集中，用户打标签行为的最晚时间作为预测时的时间
                userTimeList.set(user, time);
            }
        }

        for (int user = 0; user < this.numUser; user++) {
            Map<Integer, List<Long>> tempTagsTimeMap = userTagTimeList.get(user);
            for (Map.Entry<Integer, List<Long>> tempTagsTime : tempTagsTimeMap.entrySet()) {
                List<Long> tempTimeList = tempTagsTime.getValue();
                Collections.sort(tempTimeList);
            }
        }

        /**
         *  userTagPrefList 预测时w_[u,ta,sref]的权重
         *  在计算这部分权重时，会顺带把每个用户在各个时间戳的taoStar值存储下来：
         *  trainUserTagTimeTaoStarList
         */
        for (int user = 0; user < this.numUser; user++) {
            //对user在测试时的时间点使用不同tag的时间权重进行归一化
            double normalizeSum = 0.0d;
            //这里的时间测试集中用户打标签行为的最晚时间统一作为预测时的时间predictTime
            long predictTime = userTimeList.get(user);
            Map<Integer, List<Long>> tagsTimeMap = userTagTimeList.get(user);

            for (Map.Entry<Integer, List<Long>> tempTagsTime : tagsTimeMap.entrySet()) {
                int tag = tempTagsTime.getKey();
                List<Long> tempTimeList = tempTagsTime.getValue();
                long firstTime = tempTimeList.get(0);
                if (!this.trainUserTagTimeTaoStarList.get(user).containsKey(tag)) {
                    this.trainUserTagTimeTaoStarList.get(user).put(tag, new HashMap<>());
                }
                this.trainUserTagTimeTaoStarList.get(user).get(tag).put(firstTime, 0.0d);
                /**
                 *  对应tao(u,t,s) = initialTao  + tao_star(u,t,s)中的第二项
                 *  对第一个时间点，由于之前没有其他事件，所以tao_star=0
                 */
                double taoStar = 0.0;
                //从列表的第二项开始计算到最后一项
                for (int index = 1, length = tempTimeList.size(); index < length; index++) {
                    long curTime = tempTimeList.get(index);
                    long previousTime = tempTimeList.get(index - 1);
                    //时间间隔以天为单位
                    taoStar = (1.0 + taoStar) * Math.pow(base, (curTime - previousTime) / timeUnit * -this.d);
                    this.trainUserTagTimeTaoStarList.get(user).get(tag).put(curTime, taoStar);
                }
                /**
                 * 计算完递归的taoStar后，根据taoStar(lastTime)来计算tao(user,tag,predictTime)
                 * 当测试集中的predictTime和训练集中的lastTime相等时，我们仍然计算
                 * 结果为1+taoStar(lastTime)【加上了1这个常数值】,与PITFBLLC中的思路保持一致，
                 * PITFBLLC中当两者相等时，仍然加上了 1000^-d这个常数值
                 * tao(user,tag,predictTime) = taoStar(lastTime) + initialTao
                 */
                long lastTime = tempTimeList.get(tempTimeList.size() - 1);
                double predictTao = (1 + this.trainUserTagTimeTaoStarList.get(user).get(tag).get(lastTime)) * Math.pow(base, (predictTime - lastTime) / timeUnit * -this.d) + initialTao;
                normalizeSum += predictTao;
                this.predictUserTagPrefList.get(user).put(tag, predictTao);
            }

            for (int tag : tagsTimeMap.keySet()) {
                double value = 1.0 + Math.log10(1.0 + Math.pow(10, alphaUser)
                        * this.predictUserTagPrefList.get(user).get(tag) / normalizeSum);
                this.predictUserTagPrefList.get(user).put(tag, value);
            }
        }

        /**
         * userTagTrainWeightList; //训练集中w_u,ta,s的权重
         * 会利用前面计算好的trainUserTagTimeTaoStarList，需要计算的是tempUserTimeSum:用户在每个时间点的用于归一化的tao的和
         * 对于用户行为中已经包含的时间点直接取之前计算好的taoStar
         */
        for (int user = 0; user < this.numUser; user++) {
            Map<Integer, List<Long>> tagsTimeMap = userTagTimeList.get(user);
            for (Map.Entry<Integer, List<Long>> tempTagsTime : tagsTimeMap.entrySet()) {
                int tag = tempTagsTime.getKey();
                List<Long> tempTimeList = tempTagsTime.getValue();
                for (int index = 0, length = tempTimeList.size(); index < length; index++) {
                    long tempTime = tempTimeList.get(index);//得到了user和tempTime
                    double normalizeSum = 0.0d;
                    normalizeSum += trainUserTagTimeTaoStarList.get(user).get(tag).get(tempTime) + initialTao;

                    //再次遍历,计算 tao(u, temTime)的和
                    for (Map.Entry<Integer, List<Long>> tagsTime : tagsTimeMap.entrySet()) {
                        int tagKey = tagsTime.getKey();
                        //除去分子中的tag的其他tag对应的时间列表
                        if (tagKey != tag) {
                            List<Long> eachTimeList = tagsTime.getValue();
                            int binary_index = binary_search(eachTimeList, tempTime);
                            /**
                             * 为计算tao(u, tagKey, tempTime),先找出tempTime前离得最近的时间点的索引binary_index
                             * 有三种情况：
                             *1. 当tempTime存在于eachTimeList中时，直接取trainUserTagTimeTaoStarList.get(user).get(tagKey).get(tempTime)+initialTao即可
                             *2. 找到了tempTime之前的索引，按公式计算
                             *3. 找不到tempTime之前的索引，则tao值为0，无需累加normalizeSum
                             * tao(u, tagKey, tempTime)=initialTao + taoStar
                             */
                            if (binary_index != -1) {
                                long lastTime = eachTimeList.get(binary_index);
                                if (lastTime == tempTime) {
                                    normalizeSum += initialTao + trainUserTagTimeTaoStarList.get(user).get(tagKey).get(tempTime);
                                } else {
                                    normalizeSum += initialTao + Math.pow(base, (tempTime - lastTime) / timeUnit * -this.d)
                                            * (1 + trainUserTagTimeTaoStarList.get(user).get(tagKey).get(lastTime));
                                }
                            }
                        }
                    }
                    tempUserTimeSum.get(user).put(tempTime, normalizeSum); //以user和tempTime为key, 记录tao(user,time)的和
                    if (!userTagTrainWeightList.get(user).containsKey(tag)) {
                        userTagTrainWeightList.get(user).put(tag, new HashMap<>());
                    }
                    double value = 0.0;
                    //当用户在时间tempTime之前根本没有其他行为时[tempTime是用户在训练集中的最小时间]，会出现这种情况，这时分母为0，不能做除法
                    if (normalizeSum == 0.0) {
                        value = 1.0;
                    } else {
                        value = 1.0 + Math.log10(1.0 + Math.pow(10, alphaUser) *
                                (this.trainUserTagTimeTaoStarList.get(user).get(tag).get(tempTime) + initialTao) / normalizeSum);
                    }
                    userTagTrainWeightList.get(user).get(tag).put(tempTime, value);
                }
            }
        }

        for (int item = 0; item < this.numItem; item++) {
            double normalizeSum = 0.0;
            Map<Integer, Integer> tempTagsCountMap = itemTagCountList.get(item);
            for (Map.Entry<Integer, Integer> tempTagsCount : tempTagsCountMap.entrySet()) {
                int tag = tempTagsCount.getKey();
                int count = tempTagsCount.getValue();
                double tempValue = count;
                normalizeSum += tempValue;
                this.itemTagPrefList.get(item).put(tag, tempValue);
            }

            for (int tag : tempTagsCountMap.keySet()) {
                this.itemTagPrefList.get(item).put(tag, 1.0 + Math.log10(1.0 + Math.pow(10, alphaItem)
                        * this.itemTagPrefList.get(item).get(tag) / normalizeSum));
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
    }

    @Override
    public void buildModel() throws Exception {
        //numSample 这里设为100，控制了迭代样本量，实际上是控制负例的个数
        long num_draws_per_iter = this.numRates * this.numSample;

        for (int iter_index = 0; iter_index < this.iter; iter_index++) {
            userTagWeightNum = 0;
            userTagWeightIterNum = 0;
            this.trainStartTime = System.currentTimeMillis();
            /**
             * 这里对正负例对<user,item,[positiveTag, negativeTag],timestamp>的采样是随机的，
             * 可能会重复
             */
            for (int sample = 0; sample < num_draws_per_iter; sample++) {
                //先随机等概率找出一个正例的索引
                int post_index = Randoms.uniform(this.numRates);
                Post tempPost = this.trainPosts.get(post_index);
                int user = tempPost.getUser();
                int item = tempPost.getItem();
                int tag = tempPost.getTag();
                long time = tempPost.getTime();

                double userTagWeight = this.userTagTrainWeightList.get(user).get(tag).get(time);
                double itemTagWeight = this.calItemTagWeight(item, tag);

                double x_uit = this.predictTrain(user, item, tag, userTagWeight, itemTagWeight);

                int neg_tag = this.drawSample(user, item);
                //这里的时间戳是正例的事件戳
                double userNegTagWeight = this.calUserNegativeTagWeight(user, neg_tag, time);
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

                    //与PITFBLLC中的更新公式是相同的
                    this.userVectors.set(user, k, u_k + this.learnRate *
                            (normalizer * (t_p_u_f * userTagWeight - t_n_u_f * userNegTagWeight) - this.regU * u_k));
                    this.itemVectors.set(item, k, i_k + this.learnRate *
                            (normalizer * (t_p_i_f * itemTagWeight - t_n_i_f * itemNegTagWeight) - this.regI * i_k));
                    this.tagUserVectors.set(tag, k, t_p_u_f + this.learnRate *
                            (normalizer * u_k * userTagWeight - this.regT * t_p_u_f));
                    this.tagUserVectors.set(neg_tag, k, t_n_u_f + this.learnRate *
                            (normalizer * (-u_k) * userNegTagWeight - this.regT * t_n_u_f));
                    this.tagItemVectors.set(tag, k, t_p_i_f + this.learnRate *
                            (normalizer * i_k * itemTagWeight - this.regT * t_p_i_f));
                    this.tagItemVectors.set(neg_tag, k, t_n_i_f + this.learnRate *
                            (normalizer * (-i_k) * itemNegTagWeight - this.regT * t_n_i_f));
                }
            }

            this.trainEndTime = System.currentTimeMillis();
            allLog.info("iter " + iter_index + " train time " + (trainEndTime - trainStartTime) * 1.0 / 1000 + "s");
            averageTrainTime += (trainEndTime - trainStartTime) * 1.0 / 1000;
            measureList = evaluate();
        }
        averageTrainTime /= this.iter;
    }

    public double getAverageTrainTime() {
        return averageTrainTime;
    }

    /**
     * 负例的选取规则与PITF相同，不考虑时间戳，
     * 只要在训练集中，用户在这个item上没有打的标签都可以作为负例
     * @param user
     * @param item
     * @return
     */
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

    /**
     * 用于测试集中的推荐分数计算
     * @param user
     * @param item
     * @param tag
     * @return
     */
    @Override
    protected double predict(int user, int item, int tag) {
        /**
         * 当用户在训练集中没有打过tag这个标签，或者item在训练集中没有被打过tag这个标签，权重最低为1
         */
        double userTagWeight = 1.0;
        double itemTagWeight = 1.0;

        if (this.predictUserTagPrefList.get(user).containsKey(tag)) {
            userTagWeight = this.predictUserTagPrefList.get(user).get(tag);
        }
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            itemTagWeight = this.itemTagPrefList.get(item).get(tag);
        }
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }

    /**
     * 用于训练集中的推荐分数计算
     * @param user
     * @param item
     * @param tag
     * @param userTagWeight
     * @param itemTagWeight
     * @return
     */
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
     * 该函数负责计算负例的时间相关的权重w(user,tb,sa),tb为负例
     * 负例的时间戳与相应的正例是相同的
     *
     * @param user
     * @param tag
     * @param time
     * @return
     */
    private double calUserNegativeTagWeight(int user, int tag, long time) {
        double weight = 0.0d;
        double normalizeSum = 0.0;
        if (this.userTagSet.get(user).contains(tag)) {
            List<Long> tempTagsTimeList = this.userTagTimeList.get(user).get(tag);
            int binary_index = binary_search(tempTagsTimeList, time);
            if (binary_index != -1) {
                //当time是user在训练集中的最小时间时，tao会都为0
                if (tempUserTimeSum.get(user).get(time) == 0.0) {
                    weight = 1.0;
                } else {
                    long lastTime = tempTagsTimeList.get(binary_index);
                    if (lastTime == time) {
                        //在用户使用负例tag的时间序列中包含time，则可以直接取之前算好的weight
                        weight = userTagTrainWeightList.get(user).get(tag).get(time);
                    } else {
                        //在用户使用负例tag的时间序列中不包含time，time前最近的时间戳是lastTime
                        weight = (Math.pow(base, (time - lastTime) / timeUnit * -this.d) * (1 +
                                trainUserTagTimeTaoStarList.get(user).get(tag).get(lastTime)) + initialTao)
                                / tempUserTimeSum.get(user).get(time);
                        weight = 1.0 + Math.log10(1 + Math.pow(10, this.alphaUser) * weight);
                    }
                }
            } else {
                //用户在time之前没有用过tag,weight也为1
                weight = 1.0;
            }
        }
        //用户在训练集中没有用过tag，则权重为1.0
        else {
            weight = 1.0;
        }
        return weight;
    }

    private double calItemTagWeight(int item, int tag) {
        //tag没有被用户用来标记过item,则权重为1.0
        double weight = 1.0;
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            weight = this.itemTagPrefList.get(item).get(tag);
        }
        return weight;
    }

    /**
     * 二分查找找到key之前的最大的时间戳
     * 在按时间排好序的用户的train时间序列上
     * 1.若key不包含在list中，返回key之前最大的时间戳，若找不到则返回-1
     * 2.key本身就包含在list中，直接返回key在list中的索引
     */
    private int binary_search(List<Long> a, long key) {
        int low = 0;
        int high = a.size() - 1;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (a.get(mid) > key) {
                high = mid - 1;
            } else {
                low = mid;
            }
        }

        if (a.get(low) <= key) {
            return low;
        } else {
            return -1;
        }
    }
}
