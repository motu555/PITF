package core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.DenseMatrix;
import data.Post;
import intf.TagMFRecommender;
import org.apache.commons.logging.LogFactory;
import util.Lists;
import util.Randoms;

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
    /**
     * <useid, itemid, tagset>
     * Table代表一个特殊的映射，其中两个键可以在组合的方式被指定为单个值。
     */
    protected Table<Integer, Integer, Set<Integer>> trainTagSet;
    /**
     * <userid, tagset>, <itemid, tagset>
     * 需要改动否？
     */
    protected Map<Integer, Set<Integer>> userTagSet, itemTagSet;

    /**
     *各种参数定义在这里
     */

    double averageTrainTime = 0.0;
    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");

    public CAPITF(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                  double learnRateParam, double regUParam, double regIParam, double regTParam,
                  int randomSeedParam,int numSampleParam) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);

        this.numSample = numSampleParam;
        this.trainTagSet = HashBasedTable.create();

        this.userTagSet = new HashMap<>();
        this.itemTagSet = new HashMap<>();

        //解析训练集
        for (Post trainPost : this.trainPosts) {
            int user = trainPost.getUser();
            int item = trainPost.getItem();
            int tag = trainPost.getTag();

            if (!this.trainTagSet.contains(user, item)) {
                this.trainTagSet.put(user, item, new HashSet<>());
            }
            this.trainTagSet.get(user, item).add(tag);
            if (!this.userTagSet.containsKey(user)) {
                this.userTagSet.put(user, new HashSet<>());
            }
            if (!this.itemTagSet.containsKey(item)) {
                this.itemTagSet.put(item, new HashSet<>());
            }

            this.userTagSet.get(user).add(tag);
            this.itemTagSet.get(item).add(tag);
        }

    }

    /**
     * 初始化模型，加载相应的内容
     */
    public void initModel() throws Exception{
/**
 * TODO
 */
    }
    @Override
    public void buildModel() throws Exception {
        //numSample 这里设为100，控制了迭代样本量，实际上是控制负例的个数
        long num_draws_per_iter = this.numRates * this.numSample;
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
                double x_uit = this.predict(user, item, tag);
                int neg_tag = this.drawSample(user, item);

                double x_uint = this.predict(user, item, neg_tag);
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

                    this.userVectors.set(user, k, u_k + this.learnRate * (normalizer * (t_p_u_f - t_n_u_f) - this.regU * u_k));
                    this.itemVectors.set(item, k, i_k + this.learnRate * (normalizer * (t_p_i_f - t_n_i_f) - this.regI * i_k));
                    this.tagUserVectors.set(tag, k, t_p_u_f + this.learnRate * (normalizer * u_k - this.regT * t_p_u_f));
                    this.tagUserVectors.set(neg_tag, k, t_n_u_f + this.learnRate * (normalizer * (-u_k) - this.regT * t_n_u_f));
                    this.tagItemVectors.set(tag, k, t_p_i_f + this.learnRate * (normalizer * i_k - this.regT * t_p_i_f));
                    this.tagItemVectors.set(neg_tag, k, t_n_i_f + this.learnRate * (normalizer * (-i_k) - this.regT * t_n_i_f));
                }
            }
            /**
             * 归一化设置！！
             * TODO
             */
            double[] x=new double[10];
            this.projsmplx(x);

            this.trainEndTime = System.currentTimeMillis();
            allLog.info("loss " + loss + " delta_loss " + (loss - last_loss));
            last_loss = loss;
            /**
             * 隔几轮输出一次
             */
            if(iter_index % 5== 0||iter_index==(this.iter-1)) {
                measureList = evaluate();
            }

            averageTrainTime += (trainEndTime - trainStartTime) * 1.0 / 1000;
        }
        System.out.println("averageTime " + averageTrainTime/this.iter);

    }
    public void nuConstraint() throws Exception {
        for (Table.Cell<Integer, Integer, Set<Integer>> tempCell : this.testTagSet.cellSet()) {
            int user = tempCell.getRowKey();
            int item = tempCell.getColumnKey();
            Set<Integer> tags = tempCell.getValue();
            //只针对特定的一对user、item，有n个预测值；tag-score；已经经过排序
            List<Map.Entry<Integer, Double>> itemScores = this.predictTopN(user, item);
            /**
             * 候选集数据结构???
             */
            List<Map<Integer,Set<Integer>>> candidateTag;
            candidateTag = new ArrayList<>();
            candidateTag.add(new HashMap<>());
            candidateTag.get(1).put(1,new HashSet<>());//??
            candidateTag.get(1).get(1).add(1);

            for (int index = 0; index < this.numTag; index++) {
                if (index < itemScores.size()) {

                }
            }
        }
    }

    //归一化
    public void projsmplx(double[] x){
        /*b=sort(y);%从小到大排列
                n=length(y);
        t=zeros(n,1);
        tmp=b(n);
        for i=n-1:-1:0%倒数计数
        if i==0
        that=(sum(b)-1)/n;%每个that不一样
        break;
        end
        t(i)=(tmp-1)/(n-i);
        tmp=tmp+b(i);
        if (t(i)>=b(i))
            that=t(i);
        break;
        end
                end
        y=max(y-that,0);*/
//        Collections.sort(x);
        Arrays.sort(x);
        int n=x.length;
        double [] t=new double[n];
        double tmp = x[x.length];
        double that=0.0;
        for(int i=x.length;i>0;i--){
            if(i==1) that = sum(x,n)-1;
            else {
                t[i]=(tmp-1)/(n-i);
                tmp=tmp+x[i];
                if (t[i]>=x[i])
                    that=t[i];
            }

        }


    }
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
     *对指定的user、item对进行所有tag的预测值计算，并排序
     */
    protected List<Map.Entry<Integer, Double>> predictTopN(int user, int item) throws Exception {
        List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>();
        Set<Integer> utSet = this.userTagSet.get(user);
        Set<Integer> itSet = this.itemTagSet.get(item);
        for (int tag = 0; tag < this.numTag; tag++) {
            double rank = 0.0;
            rank += DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag);
            rank += DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
            itemScores.add(new AbstractMap.SimpleImmutableEntry<>(tag, rank));
        }
        Lists.sortList(itemScores, true);
        return itemScores;
    }


}
