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
import java.util.*;

/**
 * Created by wangkeqiang on 2016/5/14.
 * Pairwise Interaction Tensor Factorization for Personalized Tag Recommendation
 */
public class PITF extends TagMFRecommender {
    protected int numSample;
    protected Table<Integer, Integer, Set<Integer>> trainTagSet;
    protected Map<Integer, Set<Integer>> userTagSet, itemTagSet;

    private static org.apache.commons.logging.Log allLog = LogFactory.getLog("alllog");


    public PITF(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                double learnRateParam, double regUParam, double regIParam, double regTParam,
                int randomSeedParam, int numSampleParam) throws IOException {
        super(trainPostsParam, testPostsParam, dimParam, initStdevParam, iterParam, learnRateParam, regUParam, regIParam,
                regTParam, randomSeedParam);
        this.numSample = numSampleParam;
        this.trainTagSet = HashBasedTable.create();

        this.userTagSet = new HashMap<>();
        this.itemTagSet = new HashMap<>();

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

    @Override
    public void buildModel() throws Exception {
        long num_draws_per_iter = this.numRates * this.numSample;
        double averageTraintime = 0.0;
        double loss = 0.0d;
        double last_loss = 0.0d;
        for (int iter_index = 0; iter_index < this.iter; iter_index++) {
            allLog.info("iter " + iter_index +  " " + new Date());
            System.out.printf("%-15s\t", "Iter:" + iter_index);
            System.out.println(new Date());
            loss = 0.0d;
            this.trainStartTime = System.currentTimeMillis();
            for (int sample = 0; sample < num_draws_per_iter; sample++) {
                int post_index = Randoms.uniform(this.numRates);
                Post tempPost = this.trainPosts.get(post_index);
                int user = tempPost.getUser();
                int item = tempPost.getItem();
                int tag = tempPost.getTag();
                double x_uit = this.predict(user, item, tag);
                int neg_tag = this.drawSample(user, item);

                double x_uint = this.predict(user, item, neg_tag);
                double normalizer = loss(x_uit - x_uint);
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

            this.trainEndTime = System.currentTimeMillis();
            allLog.info("loss " + loss + " delta_loss " + (loss - last_loss));
            last_loss = loss;
            /**
             * 隔几轮输出一次
             */
            if(iter_index % 5== 0||iter_index==(this.iter-1)) {
                evaluate();
            }

            averageTraintime += (trainEndTime - trainStartTime) * 1.0 / 1000;
        }
        System.out.println("averageTime " + averageTraintime/this.iter);

    }

    protected int drawSample(int user, int item) {
        int tag;
        Set<Integer> tagSet = this.trainTagSet.get(user, item);
        //当从所有的tag集合中找出一个不属于(user,item)对的就停止
        do {
            tag = Randoms.uniform(this.numTag);
        } while (tagSet.contains(tag));

        return tag;
    }
    protected static double loss(double x) {
        double expX = Math.exp(-x);
        return expX / (1 + expX);
    }
    protected static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    @Override
    protected double predict(int user, int item, int tag) {
        return DenseMatrix.rowMult(this.userVectors, user, this.tagUserVectors, tag) +
                DenseMatrix.rowMult(this.itemVectors, item, this.tagItemVectors, tag);
    }

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
