package core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.ContextPost;
import data.DenseMatrix;
import data.Post;
import intf.TagMFRecommender;
import org.apache.commons.logging.LogFactory;
import util.Lists;
import util.Randoms;

import java.io.IOException;
import java.util.*;
import util.KernelSmoothing;

/**
 * Created by motu on 2018/7/15.
 * 加入归一化操作！
 * 加入context信息
 * 自己的模型
 */
public class CAPITF extends TagMFRecommender{
    protected int numSample;
    protected double alphaItem;
    protected double alphaUser;
    private float b ;           //核函数的宽度参数
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
     * poi地理位置列表、类别列表 context 信息
     */
    protected Map<Integer,Integer> TagCateSet;
    protected Map<Integer,String> TagLocationSet;
//    List<Map<Integer, List<Long>>> userTagTimeList;？？？



    /**
     *各种参数定义在这里
     * 上下文信息post集合
     */
    protected List<ContextPost> trainContextPosts;
    protected List<ContextPost> testContextPosts;
    /**
     * user, poi, preference score user-tag权重值
     *  user-poi的权重值(user-poi-地理位置)
     */
    List<Map<Integer, Double>> UserTagPrefList;
    /**
     * time, poi, preference score
     * 训练集中time-poi权重值（TODO time-poi-category）
     */
    List<Map<Integer, Double>> itemTagPrefList;

    double averageTrainTime = 0.0;
    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    public CAPITF(List<Post> trainPostsParam, List<Post> testPostsParam, List<ContextPost> trainContextPosts,List<ContextPost> testContextPosts, int dimParam, double initStdevParam, int iterParam,
                  double learnRateParam, double regUParam, double regIParam, double regTParam,
                  int randomSeedParam, int numSampleParam, double alphaItem,double alphaUser, float bParam ) throws IOException {//,double alphaUser
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);

        this.numSample = numSampleParam;
        this.alphaItem =alphaItem;
        this.alphaUser =alphaUser;
        this.trainContextPosts=trainContextPosts;
        this.testContextPosts=testContextPosts;
        this.b = bParam;

    }

    /**
     * 初始化模型，加载相应的内容
     */
    public void initModel() throws Exception{

        this.trainTagSet = HashBasedTable.create();
        //计算权重所用变量,初始化
        this.userTagSet = new ArrayList<>();
        this.itemTagSet = new ArrayList<>();
        //
        this.TagCateSet=new HashMap<>();
        this.TagLocationSet=new HashMap<>();
        this.UserTagPrefList = new ArrayList<>();
        this.itemTagPrefList =new ArrayList<>();
        List<Map<Integer, Integer>> itemTagCountList = new ArrayList<>();
        List<Map<Integer, Integer>> userTagCountList = new ArrayList<>();
//        List<Long> userTimeList = new ArrayList<>();//用来记录最晚时间作为预测时间

        //初始化user权重参数列表
        for (int userIndex = 0; userIndex < this.numUser; userIndex++) {
            this.userTagSet.add(new HashSet<>());
            userTagCountList.add(new HashMap<>());
//            userItemSet.add(new HashSet<>());
            this.UserTagPrefList.add(new HashMap<>());
        }
        //初始化item权重参数列表
        for (int itemIndex = 0; itemIndex < this.numItem; itemIndex++) {
            this.itemTagSet.add(new HashSet<>());
            itemTagCountList.add(new HashMap<>());
            this.itemTagPrefList.add(new HashMap<>());
        }

        //  初始化tag (shop)---context存储的set
        /*for (int tagIndex = 0; tagIndex < this.numTag; tagIndex++) {
            this.TagCateSet=new HashMap<>();
            itemTagCountList.add(new HashMap<>());
            this.itemTagPrefList.add(new HashMap<>());
        }*/
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
            this.itemTagSet.get(item).add(tag);//set已去重
            /*if (!this.userTagSet.get(user).contains(tag)) {
                this.userTagSet.get(user).add(tag);
            }
            if (!this.itemTagSet.get(user).contains(tag)) {
                this.itemTagSet.get(item).add(tag);
            }*/

            //时间-poi对数量
            if (!itemTagCountList.get(item).containsKey(tag)) {
                itemTagCountList.get(item).put(tag, 1);
            } else {
                itemTagCountList.get(item).put(tag, itemTagCountList.get(item).get(tag) + 1);
            }
            //user-poi对数量
            if (!userTagCountList.get(user).containsKey(tag)) {
                userTagCountList.get(user).put(tag, 1);//时间-poi次数对
            } else {
                userTagCountList.get(user).put(tag, userTagCountList.get(user).get(tag) + 1);
            }
        }

        /**
         * 读取train数据集中context信息
         * category和lat lng的信息 shopCatSet、shopLocationSet
         */
        for (ContextPost trainContextPost : this.trainContextPosts) {
            int tag = trainContextPost.gettag();
            int cate = trainContextPost.getcate();
            String lng = trainContextPost.getlng();
            String lat = trainContextPost.getlat();
            if (!this.TagCateSet.containsKey(tag)) {
                this.TagCateSet.put(tag,cate);
            }
            if (!this.TagLocationSet.containsKey(tag)) {
                this.TagLocationSet.put(tag,lng+"\t"+lat);
            }
        }

        /**
         * 读取test数据集中context信息
         * category和lat lng的信息 shopCatSet、shopLocationSet
         */
        for (ContextPost testContextPost : this.testContextPosts) {
            int tag = testContextPost.gettag();
            int cate = testContextPost.getcate();
            String lng = testContextPost.getlng();
            String lat = testContextPost.getlat();
            if (!this.TagCateSet.containsKey(tag)) {
                this.TagCateSet.put(tag,cate);//直接在set中添加？
            }
            if (!this.TagLocationSet.containsKey(tag)) {
                this.TagLocationSet.put(tag,lng+"\t"+lat);
            }
        }
        /**
         * 计算item-tag之间的权重（time-poi），写入itemTagPrefList
         * TODO 加入category
         */
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
         * 计算user-tag之间的权重（user-poi ===loction），写入 UserTagPrefList
         */
        DenseMatrix TagKernel=getAllTagKernel();//所有tag(shop)之间的location核函数
        for (int user = 0; user < this.numUser; user++) {
            Map<Integer, Integer> userTagsCountMap = userTagCountList.get(user);//每个user的 所有poi访问次数
            for (int tempTagId: userTagSet.get(user)){//此user下所有的tag,都计算出权重
//            for (int tempTagId = 0; tempTagId < numTag; tempTagId++) {//此user下所有的tag,包括未访问的，都计算权重
                double normalizeSum = 0.0;//该用户-tag所有访问次数
                double tempKernelSum = 0.0;
                for(Map.Entry<Integer,Integer>tempTagsCount : userTagsCountMap.entrySet()){//该user下每个（poi）tag的个数
                    int tag = tempTagsCount.getKey();
                    int count = tempTagsCount.getValue();
                    tempKernelSum += TagKernel.get(tag,tempTagId)* count;
                    normalizeSum += count;
                }
//                this.UserTagPrefList.get(user).put(tempTagId,tempKernelSum / normalizeSum);
                this.UserTagPrefList.get(user).put(tempTagId, 1.0 + Math.log10(1.0 + Math.pow(10, alphaItem)
                        *tempKernelSum / normalizeSum));
//                        * this.UserTagPrefList.get(user).get(tempTagId) / normalizeSum));
            }

        }


        //初始化隐变量
        this.userVectors = new DenseMatrix(this.numUser, this.dim);
        this.itemVectors = new DenseMatrix(this.numItem, this.dim);
        this.tagUserVectors = new DenseMatrix(this.numTag, this.dim);
        this.tagItemVectors = new DenseMatrix(this.numTag, this.dim);

        this.userVectors.init(0.0, this.initStdev);
        this.itemVectors.init(0.0, this.initStdev);
        this.tagUserVectors.init(0.0, this.initStdev);
        this.tagItemVectors.init(0.0, this.initStdev);

        this.measureList = new ArrayList<>();
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
                double userTagWeight=this.calUserTagWeight(user,tag);
                double itemTagWeight = this.calItemTagWeight(item, tag);
                double x_uit = this.predictTrain(user, item, tag, userTagWeight, itemTagWeight);
                //再sample一个负例
                int neg_tag = this.drawSample(user, item);
//                double userNegTagWeight = this.calUserNegativeTagWeight(user, neg_tag, time);
                double userNegTagWeight=this.calUserTagWeight(user,neg_tag);
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
                measureList = evaluate();//父类方法中写明如何从testPost中取数据进行预测
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
        if (this.UserTagPrefList.get(item).containsKey(tag)) {
            userTagWeight = this.UserTagPrefList.get(item).get(tag);
        }
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }

    protected double predictTrain(int user, int item, int tag, double userTagWeight, double itemTagWeight) {
        return userTagWeight * DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                itemTagWeight * DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }
    protected List<Map.Entry<Integer, Double>> predictTopN(int user, int item) throws Exception {
        List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>();
        for (int tag = 0; tag < this.numTag; tag++) {
            double rank = this.predict(user, item, tag);//预测方法中加入了权重
            itemScores.add(new AbstractMap.SimpleImmutableEntry<>(tag, rank));
        }
        Lists.sortList(itemScores, true);
        return itemScores;
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


    private double calItemTagWeight(int item, int tag) {
        //tag没有被用户用来标记过item,则权重为1.0
        double weight = 1.0;
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            weight = this.itemTagPrefList.get(item).get(tag);
        }
        return weight;
    }
    private double calUserTagWeight(int user, int tag) {
        //tag没有被用户用来标记过item,则权重为1.0
        double weight = 1.0;
        if (this.UserTagPrefList.get(user).containsKey(tag)) {
            weight = this.UserTagPrefList.get(user).get(tag);
        }
        return weight;
    }

    public static double getDistance(String id1,String id2) {
//        System.out.println(id1 + id2);
        if(id1=="null"||id1=="null"){
            System.out.println("get location error!!!=================");
        }
        String[] location1=id1.split("[ \t,]+");
        String[] location2=id2.split("[ \t,]+");
        double lng1 = Double.parseDouble(location1[0]);
        double lat1 = Double.parseDouble(location1[1]);
        double lng2 = Double.parseDouble(location2[0]);
        double lat2 = Double.parseDouble(location2[1]);
        double a, b, R;
        R = 6378137; // 地球半径
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (lng1 - lng2) * Math.PI / 180.0;
        double d;
        double sa2, sb2;
        sa2 = Math.sin(a / 2.0);
        sb2 = Math.sin(b / 2.0);
        d = 2* R
                * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1)
                * Math.cos(lat2) * sb2 * sb2));
        return d/1000;
    }
    private DenseMatrix getAllTagKernel() {
        DenseMatrix TagKernel = new DenseMatrix(numTag,numTag);
//        List<int,Map<int,double>> TagKernel =
        double  kernel ;
        double distance;
        for (int j = 0; j < numTag; j++) {
            for (int i = 0; i <= j; i++) {
                 String id1=TagLocationSet.get(j);
                 String id2=TagLocationSet.get(i);
                distance = getDistance(id1, id2);
                kernel = KernelSmoothing.disKernelize(distance, b, KernelSmoothing.TRIANGULAR_KERNEL);
                TagKernel.set(j, i, kernel);
                TagKernel.set(i, j, kernel);
            }
        }
        // System.out.println("maxDis:"+maxDis);
        return TagKernel;
    }

}
