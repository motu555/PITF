package core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.DenseMatrix;
import data.Post;
import intf.TagMFRecommender;
import org.apache.commons.logging.LogFactory;
import util.Lists;
import util.Randoms;

import java.awt.geom.Arc2D;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by motu on 2018/7/15.
 * 加入归一化操作！
 * 加入context信息
 * 自己的模型
 */
public class CAPITF extends TagMFRecommender{
    protected int numSample;
//    protected int noiseRates;
    /**
     * <useid, itemid, tagset>
     * Table代表一个特殊的映射，其中两个键可以在组合的方式被指定为单个值。
     */
    protected Table<Integer, Integer, Set<Integer>> trainTagSet;
//    protected Table<Integer, Integer, Set<Integer>> noiseTagSet;
    /**
     * <userid, tagset>, <itemid, tagset>
     * 暂时没有用到
     */
//    protected Map<Integer, Set<Integer>> userTagSet, itemTagSet;
     protected List<Set<Integer>> userTagSet, itemTagSet;
    /**
     * poi地理位置列表、类别列表
     */
//    List<Map<Integer, List<Long>>> userTagTimeList;？？？



    /**
     *各种参数定义在这里
     */
//    protected List<Post> noisePost;
    /**
     * user, poi, preference score user-tag权重值
     *  user-poi的权重值(user-poi-地理位置)
     */
    List<Map<Integer, Double>> predictUserTagPrefList;
    /**
     * time, poi, preference score  time-poi权重值
     */
    List<Map<Integer, Double>> itemTagPrefList;
    double alphaUser = 3.0;
    double alphaItem = 3.0;

    double averageTrainTime = 0.0;
    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    public CAPITF(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                  double learnRateParam, double regUParam, double regIParam, double regTParam,
                  int randomSeedParam,int numSampleParam) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);

        this.numSample = numSampleParam;
        //自定义
//        this.noiseTagSet= HashBasedTable.create();
//        this.noiseRates = noiseRatesParam;
    }

    /**
     * 初始化模型，加载相应的内容
     */
    public void initModel() throws Exception{
        this.trainTagSet = HashBasedTable.create();
        //计算权重所用变量
        this.userTagSet = new ArrayList<>();
        this.itemTagSet = new ArrayList<>();

        this.predictUserTagPrefList = new ArrayList<>();
        this.itemTagPrefList =new ArrayList<>();
        List<Map<Integer, Integer>> itemTagCountList = new ArrayList<>();
//        List<Long> userTimeList = new ArrayList<>();//用来记录最晚时间作为预测时间

        for (int userIndex = 0; userIndex < this.numUser; userIndex++) {
            this.userTagSet.add(new HashSet<>());
//            userTagTimeList.add(new HashMap<>());
//            userItemSet.add(new HashSet<>());
            this.predictUserTagPrefList.add(new HashMap<>());
        }

        for (int itemIndex = 0; itemIndex < this.numItem; itemIndex++) {
            this.itemTagSet.add(new HashSet<>());
            itemTagCountList.add(new HashMap<>());
            this.itemTagPrefList.add(new HashMap<>());
        }
        //解析训练集
        for (Post trainPost : this.trainPosts) {
            int user = trainPost.getUser();
            int item = trainPost.getItem();
            int tag = trainPost.getTag();

            if (!this.trainTagSet.contains(user, item)) {
                this.trainTagSet.put(user, item, new HashSet<>());
            }
            this.trainTagSet.get(user, item).add(tag);

            this.userTagSet.get(user).add(tag);
            this.itemTagSet.get(item).add(tag);

            if (!itemTagCountList.get(item).containsKey(tag)) {
                itemTagCountList.get(item).put(tag, 1);//时间-poi次数对
            } else {
                itemTagCountList.get(item).put(tag, itemTagCountList.get(item).get(tag) + 1);
            }
        }

            for (int item = 0; item < this.numItem; item++) {
                double normalizeSum = 0.0;
                Map<Integer, Integer> tempTagsCountMap = itemTagCountList.get(item);//time-poi pair
                for(Map.Entry<Integer,Integer>tempTagsCount : tempTagsCountMap.entrySet()){
                    int tag = tempTagsCount.getKey();
                    int count = tempTagsCount.getValue();
                    double tempValue = count;
                    normalizeSum += tempValue;
                    this.itemTagPrefList.get(item).put(tag, tempValue);//将原本的次数从int转化为double，便于计算
                }
                for (int tag : tempTagsCountMap.keySet()) {
                    this.itemTagPrefList.get(item).put(tag, 1.0 + Math.log10(1.0 + Math.pow(10, alphaItem)
                            * this.itemTagPrefList.get(item).get(tag) / normalizeSum));
                }

            }
        /**
         * TODO 另外文件中读取context信息
         * category和lat lng的信息 shopCatSet、shopLocationSet
         */


        //初始化隐变量
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
        //numRates为训练集样本总数量
        //noiseRates是指针对一对user item，noise增加的数量
        long num_draws_per_iter = this.numRates * this.numSample;
//        long num_draws_per_iter = this.numRates * this.noiseRates;
        double loss = 0.0d;
        double last_loss = 0.0d;

        for (int iter_index = 0; iter_index < this.iter; iter_index++) {
            allLog.info("iter " + iter_index +  " " + new Date());
            System.out.printf("%-15s\t", "Iter:" + iter_index);
            System.out.println(new Date());
            loss = 0.0d;
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

//                double userTagWeight = this.userTagTrainWeightList.get(user).get(tag).get(time);
                double userTagWeight=1.0;
                double itemTagWeight = this.calItemTagWeight(item, tag);
                double x_uit = this.predictTrain(user, item, tag, userTagWeight, itemTagWeight);
                //再sample一个负例
                int neg_tag = this.drawSample(user, item);
//                double userNegTagWeight = this.calUserNegativeTagWeight(user, neg_tag, time);
                double userNegTagWeight=1.0;
                double itemNegTagWeight = this.calItemTagWeight(item, neg_tag);
                double x_uint = this.predictTrain(user, item, neg_tag, userNegTagWeight, itemNegTagWeight);

                double normalizer = loss(x_uit - x_uint);
                //目标函数的loss值
                loss += Math.log(sigmoid(x_uit - x_uint));

                for (int k = 0; k < this.dim; k++) {
                    double u_k = this.userVectors.get(user, k);
                    double i_k = this.itemVectors.get(item, k);
                    double t_p_u_f = this.tagUserVectors.get(tag, k);
                    double t_p_i_f = this.tagItemVectors.get(tag, k);
                    double t_n_u_f = this.tagUserVectors.get(neg_tag, k);
                    double t_n_i_f = this.tagItemVectors.get(neg_tag, k);

                    loss -= regU * Math.pow(u_k, 2);
                    loss -= regI * Math.pow(i_k, 2);
                    loss -= regT * Math.pow(t_p_u_f, 2);
                    loss -= regT * Math.pow(t_p_i_f, 2);
                    loss -= regT * Math.pow(t_n_u_f, 2);
                    loss -= regT * Math.pow(t_n_i_f, 2);

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
            /**
             * 归一化设置！！
             * TODO
             */
//            double[] x=new double[10];
//            this.projsmplx(x);

            this.trainEndTime = System.currentTimeMillis();
            allLog.info("loss " + loss + " delta_loss " + (loss - last_loss));
//            System.out.println("loss " + loss + " delta_loss " + (loss - last_loss));
            last_loss = loss;
            /**
             * 隔几轮输出一次
             */
            if(iter_index % 10== 0||iter_index==(this.iter-1)) {
                measureList = evaluate();
            }

            averageTrainTime += (trainEndTime - trainStartTime) * 1.0 / 1000;
        }
        System.out.println("averageTime " + averageTrainTime/this.iter);

    }

    //归一化
    public void softmax(List<Map.Entry<Integer, Double>> itemScores){
        //候选集数据结构


    }
    /**
     * 计算此list的和，递归，传数组长度
     */
    private static double sum(double arr[], int n) {
        if(n == 1) {
            return arr[0];
        }else {
            return arr[n-1] + sum(arr, --n);
        }
    }

    protected static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
    public double getAverageTrainTime() {
        return averageTrainTime;
    }
    @Override
    protected double predict(int user, int item, int tag) {
        //目标预测结果，矩阵行对应相乘；如果没有被计算过权重，最低为1
        double userTagWeight = 1.0;
        double itemTagWeight = 1.0;

        /*if (this.predictUserTagPrefList.get(user).containsKey(tag)) {
            userTagWeight = this.predictUserTagPrefList.get(user).get(tag);
        }*/
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            itemTagWeight = this.itemTagPrefList.get(item).get(tag);
        }
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }

    protected double predictTrain(int user, int item, int tag, double userTagWeight, double itemTagWeight) {
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }

    /**
     * 负例的选取规则与PITF相同，不考虑时间戳，
     * 只要在训练集中，用户在这个item上没有打的标签都可以作为负例
     * 训练集+noise中都没有的，作为负例
     */
    protected int drawSample(int user, int item) {
        int tag;
        Set<Integer> tagSet = this.trainTagSet.get(user, item);
//        Set<Integer> noisetagSet = this.noiseTagSet.get(user, item);
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
     *对指定的user、item对进行所有tag的预测值计算，并排序
     */
    protected List<Map.Entry<Integer, Double>> predictTopN(int user, int item) throws Exception {
        List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>();
        for (int tag = 0; tag < this.numTag; tag++) {
            double rank = this.predict(user, item, tag);//预测方法中加入了权重
            itemScores.add(new AbstractMap.SimpleImmutableEntry<>(tag, rank));
        }
        Lists.sortList(itemScores, true);
        return itemScores;
    }

    private double calItemTagWeight(int item, int tag) {
        //tag没有被用户用来标记过item,则权重为1.0
        double weight = 1.0;
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            weight = this.itemTagPrefList.get(item).get(tag);
        }
        return weight;
    }

}
